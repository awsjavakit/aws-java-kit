package com.github.awsjavakit.testingutils.networking;

import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomElement;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomInteger;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;

import com.github.awsjavakit.misc.StringUtils;
import com.github.awsjavakit.misc.paths.UriWrapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.direct.DirectCallHttpServerFactory;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class WiremockDirectCallClientTest {

  public static final String LOCALHOST = "https://localhost";
  public static final String GET = "GET";
  public static final String POST = "POST";
  public static final String PUT = "PUT";
  public static final String METHOD_ERROR = "Unrecognized value of variable method";
  public static final String NOT_USED = null;
  private WireMockServer directCallServer;
  private HttpClient directCallClient;

  public static URI uriWithPath(String hostUri) {
    return UriWrapper.fromUri(hostUri).addChild("some/path").getUri();
  }

  @BeforeEach
  public void init() {
    var factory = new DirectCallHttpServerFactory();

    this.directCallServer = new WireMockServer(options().httpServerFactory(factory));
    directCallServer.start(); // no-op, not required
    var directCallHttpServer = factory.getHttpServer();
    this.directCallClient = new WiremockDirectCallClient(directCallHttpServer);
  }

  @AfterEach
  public void stop() {
    this.directCallServer.stop();
  }

  @ParameterizedTest
  @MethodSource("requestProvider")
  void shouldTransformWiremockResponseForRequests(TestSetup testSetup)
    throws IOException, InterruptedException {
    var expectedResponseBody = testSetup.responseBody();
    var expectedResponseCode = testSetup.responseCode();

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
    var expectedResponseBody = testSetup.responseBody();
    var expectedResponseCode = testSetup.responseCode();
    var expectedResponseHeaderKey = testSetup.responseHeader();
    var expectedResponseHeaderValue = testSetup.responseHeaderValue();

    var mapping = testSetup.mapping();
    directCallServer.stubFor(mapping);

    var request = testSetup.request();
    var response = directCallClient.send(request, BodyHandlers.ofString());

    assertThat(response.body(), response.statusCode(), is(equalTo(expectedResponseCode)));
    assertThat(response.body(), is(equalTo(expectedResponseBody)));
    var responseHeaders = response.headers().map();
    assertThat(responseHeaders.keySet(), hasItem(expectedResponseHeaderKey));
    assertThat(responseHeaders.get(expectedResponseHeaderKey),
      is(equalTo(List.of(expectedResponseHeaderValue))));
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
      createSetupWithRequestExpectingResponseHeaders(PUT)
    );
  }

  private static TestSetup createSetupWithSimpleGetRequest() {
    var uri = uriWithPath(LOCALHOST);
    var responseBody = randomString();
    var responseCode = randomResponseCode();

    var mapping = createBasicStubRequestMapping(uri, NOT_USED, GET);
    addBasicResponseToStubMapping(mapping, responseBody, responseCode);
    var request = createBasicHttpRequest(uri, NOT_USED, GET);
    return new TestSetup(responseBody, responseCode, NOT_USED, NOT_USED, mapping, request);
  }

  private static TestSetup createSetupWithRequestWithBody(String method) {
    var uri = uriWithPath(LOCALHOST);
    var requestBody = randomString();
    var responseBody = randomString();
    var responseCode = randomResponseCode();

    MappingBuilder mapping = createBasicStubRequestMapping(uri, requestBody, method);
    addBasicResponseToStubMapping(mapping, responseBody, responseCode);
    HttpRequest request = createBasicHttpRequest(uri, requestBody, method);
    return new TestSetup(responseBody, responseCode, NOT_USED, NOT_USED, mapping, request);
  }

  private static TestSetup createSetupWithRequestWithEmptyBody(String method) {
    var uri = uriWithPath(LOCALHOST);
    var responseBody = randomString();
    var responseCode = randomResponseCode();

    MappingBuilder mapping = createStubRequestMappingWithEmptyBody(uri, method);
    addBasicResponseToStubMapping(mapping, responseBody, responseCode);
    HttpRequest request = createHttpRequestWithEmptyBody(uri, method);
    return new TestSetup(responseBody, responseCode, NOT_USED, NOT_USED, mapping, request);
  }

  private static TestSetup createSetupWithRequestWithQueryParams(String method) {
    var uri = uriWithParameters();
    var requestBody = randomString();
    var responseBody = randomString();
    var responseCode = randomResponseCode();

    MappingBuilder mapping = createBasicStubRequestMapping(uri, requestBody, method);
    addQueryParametersToStubMapping(mapping, uri);
    addBasicResponseToStubMapping(mapping, responseBody, responseCode);
    HttpRequest request = createBasicHttpRequest(uri, requestBody, method);
    return new TestSetup(responseBody, responseCode, NOT_USED, NOT_USED, mapping, request);
  }

  private static TestSetup createSetupWithRequestWithHeaders(String method) {
    var uri = uriWithPath(LOCALHOST);
    var requestBody = randomString();
    var requestHeader = randomString();
    var requestHeaderValue = randomString();
    var responseBody = randomString();
    var responseCode = randomResponseCode();

    MappingBuilder mapping = createBasicStubRequestMapping(uri, requestBody, method);
    addHeadersToStubMapping(mapping, requestHeader, requestHeaderValue);
    addBasicResponseToStubMapping(mapping, responseBody, responseCode);
    HttpRequest request = createHttpRequestWithHeaders(uri, requestBody,
      requestHeader, requestHeaderValue, method);
    return new TestSetup(responseBody, responseCode, NOT_USED, NOT_USED, mapping, request);
  }

  private static TestSetup createSetupWithRequestExpectingResponseHeaders(String method) {
    var uri = uriWithPath(LOCALHOST);
    var requestBody = randomString();
    var responseHeaderKey = randomString();
    var responseHeaderValue = randomString();
    var responseBody = randomString();
    int responseCode = randomResponseCode();

    MappingBuilder mapping = createBasicStubRequestMapping(uri, requestBody, method);
    addResponseWithHeadersToStubMapping(mapping, responseBody, responseCode, responseHeaderKey, responseHeaderValue);
    HttpRequest request = createBasicHttpRequest(uri, requestBody, method);
    return new TestSetup(responseBody, responseCode, responseHeaderKey, responseHeaderValue, mapping, request);
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

  private static void addBasicResponseToStubMapping(MappingBuilder mapping, String responseBody, Integer responseCode){
    mapping.willReturn(aResponse().withBody(responseBody).withStatus(responseCode));
  }

  private static void addResponseWithHeadersToStubMapping(MappingBuilder mapping, String responseBody,
                                                          Integer responseCode, String header, String headerValue){
    mapping.willReturn(aResponse().withBody(responseBody).withStatus(responseCode)
      .withHeader(header, headerValue));
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
                                                          String requestHeader, String requestHeaderValue,
                                                          String method) {
    return switch (method) {
      case GET -> HttpRequest.newBuilder(uri).GET().header(requestHeader, requestHeaderValue).build();
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

  private record TestSetup(String responseBody, Integer responseCode,
                           String responseHeader, String responseHeaderValue,
                           MappingBuilder mapping, HttpRequest request) {
  }
}