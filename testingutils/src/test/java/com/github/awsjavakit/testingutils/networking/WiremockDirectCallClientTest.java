package com.github.awsjavakit.testingutils.networking;

import static com.github.awsjavakit.apigateway.HttpMethod.GET;
import static com.github.awsjavakit.apigateway.HttpMethod.POST;
import static com.github.awsjavakit.apigateway.HttpMethod.PUT;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomElement;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomInteger;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.like;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static java.util.Objects.nonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.awsjavakit.apigateway.HttpMethod;
import com.github.awsjavakit.misc.StringUtils;
import com.github.awsjavakit.misc.paths.UriWrapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.direct.DirectCallHttpServerFactory;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class WiremockDirectCallClientTest {

  public static final String LOCALHOST = "https://localhost";
  public static final BiPredicate<String, String> ALLOW_ALL_HEADERS = (header, value) -> true;
  public static final String HEADER_DELIMITER = ",";
  public static final String EMPTY = null;
  private HttpClient directCallClient;
  private WireMockServer directCallServer;

  public static URI uriWithPath(String hostUri) {
    return UriWrapper.fromUri(hostUri).addChild("some/path").getUri();
  }

  @BeforeEach
  public void init() {
    var factory = new DirectCallHttpServerFactory();
    directCallServer = new WireMockServer(options().httpServerFactory(factory));
    directCallServer.start();
    var directCallHttpServer = factory.getHttpServer();
    directCallClient = new WiremockDirectCallClient(directCallHttpServer);
  }

  @AfterEach
  public void stop() {
    directCallServer.stop();
  }

  @ParameterizedTest
  @MethodSource("requestProvider")
  void shouldTransformWiremockResponseForRequests(TestSetup testSetup)
    throws IOException, InterruptedException {
    var expectedResponseBody = testSetup.expectedResponse().getBody();
    var expectedResponseCode = testSetup.expectedResponse().getStatus();
    var expectedHeaders = new ConcurrentHashMap<>(
      HeadersUtils.wiremockHeadersToJavaHeaders(testSetup.expectedResponse().getHeaders()).map());
    var mapping = testSetup.mapping();

    directCallServer.stubFor(mapping);

    var request = testSetup.request();
    var response = directCallClient.send(request, BodyHandlers.ofString());

    assertThat(response.body(), response.statusCode(), is(equalTo(expectedResponseCode)));
    assertThat(response.body(), is(equalTo(expectedResponseBody)));
    var responseHeaders = new ConcurrentHashMap<>(response.headers().map());
    responseHeaders.remove("Matched-Stub-Id");
    assertThat(responseHeaders, is(equalTo(expectedHeaders)));
  }

  @Test
  void shouldThrowExceptionWhenCallingUnsupportedMethod() {
    assertThrows(UnsupportedOperationException.class, directCallClient::cookieHandler);
    assertThrows(UnsupportedOperationException.class, directCallClient::connectTimeout);
    assertThrows(UnsupportedOperationException.class, directCallClient::followRedirects);
    assertThrows(UnsupportedOperationException.class, directCallClient::proxy);
    assertThrows(UnsupportedOperationException.class, directCallClient::sslContext);
    assertThrows(UnsupportedOperationException.class, directCallClient::sslParameters);
    assertThrows(UnsupportedOperationException.class, directCallClient::authenticator);
    assertThrows(UnsupportedOperationException.class, directCallClient::version);
    assertThrows(UnsupportedOperationException.class, directCallClient::executor);
    assertThrows(UnsupportedOperationException.class,
      () -> directCallClient.sendAsync(null, null, null));

  }

  private static Stream<TestSetup> getRequestsProvider() {
    return Stream.of(createSetupWithSimpleGetRequest(),
      createSetupWithRequestWithQueryParams(GET),
      createSetupWithRequestWithHeaders(GET),
      createSetupWhereResponseHasHeaders(GET));
  }

  private static Stream<TestSetup> postRequestsProvider() {
    return Stream.of(createSetupWithRequestWithBody(POST),
      createSetupWithRequestWithEmptyBody(POST),
      createSetupWithRequestWithQueryParams(POST),
      createSetupWithRequestWithHeaders(POST),
      createSetupWhereResponseHasHeaders(POST)
      );
  }

  private static Stream<TestSetup> putRequestsProvider() {
    return Stream.of(createSetupWithRequestWithBody(PUT),
      createSetupWithRequestWithEmptyBody(PUT),
      createSetupWithRequestWithQueryParams(PUT),
      createSetupWithRequestWithHeaders(PUT),
      createSetupWhereResponseHasHeaders(PUT));
  }

  private static Stream<TestSetup> requestProvider() {
    return Stream.of(getRequestsProvider(), postRequestsProvider(), putRequestsProvider())
      .reduce(Stream::concat).get();
  }

  private static TestSetup createSetupWithSimpleGetRequest() {
    var uri = uriWithPath(LOCALHOST);
    var response = aResponse().withBody(randomString()).withStatus(randomResponseCode());
    var mapping = createBasicStubRequestMapping(uri, EMPTY, GET).willReturn(response);

    var request = createBasicHttpRequest(uri, EMPTY, GET);
    return new TestSetup(response.build(), mapping, request);
  }

  private static TestSetup createSetupWithRequestWithBody(HttpMethod method) {
    var uri = uriWithPath(LOCALHOST);
    var requestBody = randomString();
    var responseBody = randomString();
    var responseCode = randomResponseCode();

    var response = aResponse().withBody(responseBody).withStatus(responseCode).build();

    var mapping = createBasicStubRequestMapping(uri, requestBody, method).willReturn(
      like(response));
    var request = createBasicHttpRequest(uri, requestBody, method);
    return new TestSetup(response, mapping, request);
  }

  private static TestSetup createSetupWithRequestWithEmptyBody(HttpMethod method) {
    var uri = uriWithPath(LOCALHOST);
    var response = aResponse().withBody(randomString()).withStatus(randomResponseCode()).build();
    // TODO Empty body should be non null
    var mapping = createBasicStubRequestMapping(uri, EMPTY, method).willReturn(like(response));

    var request = createBasicHttpRequest(uri, EMPTY, method);

    return new TestSetup(response, mapping, request);
  }

  private static TestSetup createSetupWithRequestWithQueryParams(HttpMethod method) {
    var uri = uriWithQueryParameters();
    var requestBody = randomString();
    var response = aResponse().withBody(randomString()).withStatus(randomInteger()).build();

    var mapping = createBasicStubRequestMapping(uri, requestBody, method);
    addQueryParametersToStubMapping(mapping, uri);
    mapping.willReturn(like(response));

    var request = createBasicHttpRequest(uri, requestBody, method);
    return new TestSetup(response, mapping, request);
  }

  private static TestSetup createSetupWithRequestWithHeaders(HttpMethod method) {
    var uri = uriWithPath(LOCALHOST);
    var requestBody = randomString();
    var requestHeader = HttpHeader.httpHeader(randomString(), randomString());
    var response = aResponse().withBody(randomString()).withStatus(randomResponseCode()).build();

    var mapping = createBasicStubRequestMapping(uri, requestBody, method);
    addHeadersToStubMapping(mapping, requestHeader);

    mapping.willReturn(like(response));

    var request = createHttpRequestWithHeaders(uri, requestBody, requestHeader, method);
    return new TestSetup(response, mapping, request);
  }

  private static TestSetup createSetupWhereResponseHasHeaders(HttpMethod method) {
    var uri = uriWithPath(LOCALHOST);
    var requestBody = randomString();

    var response = aResponse().withBody(randomString()).withStatus(randomResponseCode())
      .withHeader(randomString(), randomString());

    var mapping = createBasicStubRequestMapping(uri, requestBody, method).willReturn(response);

    var request = createBasicHttpRequest(uri, requestBody, method);
    return new TestSetup(response.build(), mapping, request);
  }

  private static Integer randomResponseCode() {
    return randomElement(randomInteger(1000));
  }

  private static MappingBuilder createBasicStubRequestMapping(URI uri, String requestBody, HttpMethod method) {
    var mapping = WireMock.request(method.toString(), urlPathEqualTo(uri.getPath()));
    if (StringUtils.isNotBlank(requestBody)) {
      mapping = mapping.withRequestBody(WireMock.equalTo(requestBody));
    }
    return mapping;

  }

  private static void addQueryParametersToStubMapping(MappingBuilder mapping, URI uri) {
    if (StringUtils.isNotBlank(uri.getRawQuery())) {
      UriWrapper.fromUri(uri).getQueryParameters()
        .forEach((key, value) -> mapping.withQueryParam(key, WireMock.equalTo(value)));
    }
  }

  private static void addHeadersToStubMapping(MappingBuilder mapping, HttpHeader httpHeader) {
    if (StringUtils.isNotBlank(httpHeader.getKey()) && StringUtils.isNotBlank(
      joinHeaderValues(httpHeader))) {
      mapping.withHeader(httpHeader.key(), WireMock.equalTo(joinHeaderValues(httpHeader)));
    }
  }

  private static HttpRequest createBasicHttpRequest(URI uri, String requestBody, HttpMethod method) {
    var bodyPublisher =
      nonNull(requestBody) ? BodyPublishers.ofString(requestBody) : BodyPublishers.noBody();
    return HttpRequest.newBuilder(uri).method(method.toString(), bodyPublisher).build();
  }

  private static HttpRequest createHttpRequestWithHeaders(URI uri, String requestBody,
                                                          HttpHeader httpHeader, HttpMethod method) {
    var headerValues = joinHeaderValues(httpHeader);

    return HttpRequest.newBuilder(createBasicHttpRequest(uri, requestBody, method),
      ALLOW_ALL_HEADERS).header(httpHeader.getKey(), headerValues).build();

  }

  private static String joinHeaderValues(HttpHeader httpHeader) {
    return String.join(HEADER_DELIMITER, httpHeader.values()).trim();
  }

  private static URI uriWithQueryParameters() {
    return UriWrapper.fromUri(LOCALHOST).addChild("some/path")
      .addQueryParameter(randomString(), randomString()).getUri();
  }

  private record TestSetup(ResponseDefinition expectedResponse, MappingBuilder mapping,
                           HttpRequest request) {

  }
}