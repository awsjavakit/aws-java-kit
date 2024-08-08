package com.github.awsjavakit.testingutils.networking;

import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomElement;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomInteger;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.like;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.awsjavakit.misc.StringUtils;
import com.github.awsjavakit.misc.paths.UriWrapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.direct.DirectCallHttpServerFactory;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@SuppressWarnings("PMD.GodClass")
class WiremockDirectCallClientTest {

  public static final String LOCALHOST = "https://localhost";
  public static final String GET = "GET";
  public static final String POST = "POST";
  public static final String PUT = "PUT";
  public static final String METHOD_ERROR = "Unrecognized value of variable method";
  public static final String NOT_USED = null;
  private static HttpClient directCallClient;
  private WireMockServer directCallServer;

  public static URI uriWithPath(String hostUri) {
    return UriWrapper.fromUri(hostUri).addChild("some/path").getUri();
  }

  @BeforeEach
  public void init() {
    var factory = new DirectCallHttpServerFactory();
    this.directCallServer = new WireMockServer(options().httpServerFactory(factory));
    directCallServer.start(); // no-op, not required
    var directCallHttpServer = factory.getHttpServer();
    directCallClient = new WiremockDirectCallClient(directCallHttpServer);
  }

  @AfterEach
  public void stop() {
    this.directCallServer.stop();
  }

  @ParameterizedTest
  @MethodSource("requestProvider")
  void shouldTransformWiremockResponseForRequests(TestSetup testSetup)
    throws IOException, InterruptedException {
    var expectedResponseBody = testSetup.expectedResponse().getBody();
    var expectedResponseCode = testSetup.expectedResponse().getStatus();

    var mapping = testSetup.mapping();
    directCallServer.stubFor(mapping);

    var request = testSetup.request();
    var response = directCallClient.send(request, BodyHandlers.ofString());

    assertThat(response.body(), response.statusCode(), is(equalTo(expectedResponseCode)));
    assertThat(response.body(), is(equalTo(expectedResponseBody)));
  }

  @ParameterizedTest
  @MethodSource("responseProvider")
  void shouldForwardResponseHeaders(TestSetup testSetup) throws IOException, InterruptedException {
    var expectedResponseBody = testSetup.expectedResponse().getBody();
    var expectedResponseCode = testSetup.expectedResponse().getStatus();
    var expectedHeaders = new ConcurrentHashMap<>(HeadersUtils.wiremockHeadersToJavaHeaders(testSetup.expectedResponse().getHeaders()).map());



    var mapping = testSetup.mapping();
    directCallServer.stubFor(mapping);

    var request = testSetup.request();
    var response = directCallClient.send(request, BodyHandlers.ofString());

    assertThat(response.body(), response.statusCode(), is(equalTo(expectedResponseCode)));
    assertThat(response.body(), is(equalTo(expectedResponseBody)));
    var responseHeaders = new ConcurrentHashMap<>(response.headers().map());
    responseHeaders.remove("Matched-Stub-Id");
    assertThat(responseHeaders,is(equalTo(expectedHeaders)));

  }

  @ParameterizedTest
  @MethodSource("unsupportedProvider")
  void shouldThrowExceptionWhenCallingUnsupportedMethod(Supplier<Object> supplier) {
    var exception = assertThrows(UnsupportedOperationException.class, supplier::get);
    assertThat(exception, is(instanceOf(UnsupportedOperationException.class)));

    exception = assertThrows(UnsupportedOperationException.class,
      () -> directCallClient.sendAsync(null, null, null));
    assertThat(exception, is(instanceOf(UnsupportedOperationException.class)));
  }

  private static Stream<TestSetup> requestProvider() {
    return Stream.of(createSetupWithSimpleGetRequest(),
      createSetupWithRequestWithQueryParams(GET),
      createSetupWithRequestWithHeaders(GET),
      createSetupWithRequestWithBody(POST),
      createSetupWithRequestWithEmptyBody(POST),
      createSetupWithRequestWithQueryParams(POST),
      createSetupWithRequestWithHeaders(POST),
      createSetupWithRequestWithBody(PUT),
      createSetupWithRequestWithEmptyBody(PUT),
      createSetupWithRequestWithQueryParams(PUT),
      createSetupWithRequestWithHeaders(PUT));
  }

  private static Stream<TestSetup> responseProvider() {
    return Stream.of(createSetupWithRequestExpectingResponseHeaders(GET),
      createSetupWithRequestExpectingResponseHeaders(POST),
      createSetupWithRequestExpectingResponseHeaders(PUT));
  }

  private static Stream<Supplier<Object>> unsupportedProvider() {
    return Stream.of(directCallClient::cookieHandler,
      directCallClient::connectTimeout,
      directCallClient::followRedirects,
      directCallClient::proxy,
      directCallClient::sslContext,
      directCallClient::sslParameters,
      directCallClient::authenticator,
      directCallClient::version,
      directCallClient::executor);
  }

  private static TestSetup createSetupWithSimpleGetRequest() {
    var uri = uriWithPath(LOCALHOST);
    var responseBody = randomString();
    var responseCode = randomResponseCode();
    var response=aResponse().withBody(responseBody).withStatus(responseCode);
    var mapping = createBasicStubRequestMapping(uri, NOT_USED, GET)
      .willReturn(response);

    var request = createBasicHttpRequest(uri, NOT_USED, GET);
    return new TestSetup(response.build(), mapping, request);
  }

  private static TestSetup createSetupWithRequestWithBody(String method) {
    var uri = uriWithPath(LOCALHOST);
    var requestBody = randomString();
    var responseBody = randomString();
    var responseCode = randomResponseCode();

    var response =
      aResponse().withBody(responseBody).withStatus(responseCode).build();

    var mapping = createBasicStubRequestMapping(uri, requestBody, method)
      .willReturn(like(response));
    var request = createBasicHttpRequest(uri, requestBody, method);
    return new TestSetup(response, mapping, request);
  }

  private static TestSetup createSetupWithRequestWithEmptyBody(String method) {
    var uri = uriWithPath(LOCALHOST);
    var responseBody = randomString();
    var responseCode = randomResponseCode();

    var mapping = createStubRequestMappingWithEmptyBody(uri, method);
    var response=addBasicResponseToStubMapping(mapping, responseBody, responseCode);
    var request = createHttpRequestWithEmptyBody(uri, method);
    return new TestSetup(response, mapping, request);
  }

  private static TestSetup createSetupWithRequestWithQueryParams(String method) {
    var uri = uriWithParameters();
    var requestBody = randomString();
    var responseBody = randomString();
    var responseCode = randomResponseCode();

    var mapping = createBasicStubRequestMapping(uri, requestBody, method);
    addQueryParametersToStubMapping(mapping, uri);
    var response= addBasicResponseToStubMapping(mapping, responseBody, responseCode);
    var request = createBasicHttpRequest(uri, requestBody, method);
    return new TestSetup(response, mapping, request);
  }

  private static TestSetup createSetupWithRequestWithHeaders(String method) {
    var uri = uriWithPath(LOCALHOST);
    var requestBody = randomString();
    var requestHeader = randomString();
    var requestHeaderValue = randomString();
    var responseBody = randomString();
    var responseCode = randomResponseCode();

    var mapping = createBasicStubRequestMapping(uri, requestBody, method);
    addHeadersToStubMapping(mapping, requestHeader, requestHeaderValue);
    var response=addBasicResponseToStubMapping(mapping, responseBody, responseCode);
    var request =
      createHttpRequestWithHeaders(uri, requestBody, requestHeader, requestHeaderValue, method);
    return new TestSetup(response, mapping, request);
  }

  private static TestSetup createSetupWithRequestExpectingResponseHeaders(String method) {
    var uri = uriWithPath(LOCALHOST);
    var requestBody = randomString();
    var responseHeaderKey = randomString();
    var responseHeaderValue = randomString();
    var responseBody = randomString();
    int responseCode = randomResponseCode();
    var response = aResponse()
      .withBody(responseBody)
      .withStatus(responseCode)
      .withHeader(responseHeaderKey,responseHeaderValue);

    var mapping = createBasicStubRequestMapping(uri, requestBody, method)
      .willReturn(response);

    var request = createBasicHttpRequest(uri, requestBody, method);
    return new TestSetup(response.build(), mapping, request);
  }

  private static Integer randomResponseCode() {
    return randomElement(randomInteger(1000));
  }

  private static MappingBuilder createBasicStubRequestMapping(URI uri, String requestBody, String method) {
    return switch (method) {
      case GET -> WireMock.get(WireMock.urlPathEqualTo(uri.getPath()));
      case POST -> WireMock.post(WireMock.urlPathEqualTo(uri.getPath()))
        .withRequestBody(WireMock.equalTo(requestBody));
      case PUT -> WireMock.put(WireMock.urlPathEqualTo(uri.getPath()))
        .withRequestBody(WireMock.equalTo(requestBody));
      default -> throw new IllegalArgumentException(METHOD_ERROR);
    };
  }

  private static MappingBuilder createStubRequestMappingWithEmptyBody(URI uri, String method) {
    return switch (method) {
      case GET -> WireMock.get(WireMock.urlPathEqualTo(uri.getPath()));
      case POST -> WireMock.post(WireMock.urlPathEqualTo(uri.getPath()))
        .withRequestBody(WireMock.absent());
      case PUT -> WireMock.put(WireMock.urlPathEqualTo(uri.getPath()))
        .withRequestBody(WireMock.absent());
      default -> throw new IllegalArgumentException(METHOD_ERROR);
    };
  }

  private static void addQueryParametersToStubMapping(MappingBuilder mapping, URI uri) {
    if (StringUtils.isNotBlank(uri.getRawQuery())) {
      UriWrapper.fromUri(uri).getQueryParameters()
        .forEach((key, value) -> mapping.withQueryParam(key, WireMock.equalTo(value)));
    }
  }

  private static void addHeadersToStubMapping(MappingBuilder mapping, String headerKey,
                                              String headerValue) {
    if (StringUtils.isNotBlank(headerKey) && StringUtils.isNotBlank(headerValue)) {
      mapping.withHeader(headerKey, WireMock.equalTo(headerValue));
    }
  }

  private static ResponseDefinition addBasicResponseToStubMapping(MappingBuilder mapping, String responseBody,
                                                    Integer responseCode) {

    var response =
      aResponse().withBody(responseBody).withStatus(responseCode).build();
    mapping.willReturn(like(response));
    return response;
  }



  private static HttpRequest createBasicHttpRequest(URI uri, String requestBody, String method) {
    return switch (method) {
      case GET -> HttpRequest.newBuilder(uri).GET().build();
      case POST -> HttpRequest.newBuilder(uri).POST(BodyPublishers.ofString(requestBody)).build();
      case PUT -> HttpRequest.newBuilder(uri).PUT(BodyPublishers.ofString(requestBody)).build();
      default -> throw new IllegalArgumentException(METHOD_ERROR);
    };
  }

  private static HttpRequest createHttpRequestWithEmptyBody(URI uri, String method) {
    return switch (method) {
      case GET -> HttpRequest.newBuilder(uri).GET().build();
      case POST -> HttpRequest.newBuilder(uri).POST(BodyPublishers.noBody()).build();
      case PUT -> HttpRequest.newBuilder(uri).PUT(BodyPublishers.noBody()).build();
      default -> throw new IllegalArgumentException(METHOD_ERROR);
    };
  }

  private static HttpRequest createHttpRequestWithHeaders(URI uri, String requestBody,
                                                          String requestHeader,
                                                          String requestHeaderValue,
                                                          String method) {
    return switch (method) {
      case GET -> HttpRequest.newBuilder(uri).GET()
        .header(requestHeader, requestHeaderValue).build();
      case POST -> HttpRequest.newBuilder(uri).POST(BodyPublishers.ofString(requestBody))
        .header(requestHeader, requestHeaderValue).build();
      case PUT -> HttpRequest.newBuilder(uri).PUT(BodyPublishers.ofString(requestBody))
        .header(requestHeader, requestHeaderValue).build();
      default -> throw new IllegalArgumentException(METHOD_ERROR);
    };
  }

  private static URI uriWithParameters() {
    return UriWrapper.fromUri(LOCALHOST).addChild("some/path")
      .addQueryParameter(randomString(), randomString()).getUri();
  }

  private record TestSetup(ResponseDefinition expectedResponse,
                           MappingBuilder mapping,
                           HttpRequest request) {
  }
}