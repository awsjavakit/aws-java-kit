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
      createSetupWithGetRequestWithQueryParams(),
      createSetupWithGetRequestWithHeaders(),

      createSetupWithPostRequestWithBody(),
      createSetupWithPostRequestWithEmptyBody(),
      createSetupWithPostRequestWithQueryParams(),
      createSetupWithPostRequestWithHeaders(),

      createSetupWithPutRequestWithBody(),
      createSetupWithPutRequestWithEmptyBody(),
      createSetupWithPutRequestWithQueryParams(),
      createSetupWithPutRequestWithHeaders());
  }

  private static Stream<TestSetup> responseProvider() {
    return Stream.of(createSetupWithGetRequestExpectingResponseHeaders(),
      createSetupWithPostRequestExpectingResponseHeaders(),
      createSetupWithPutRequestExpectingResponseHeaders()
    );
  }

  private static TestSetup createSetupWithSimpleGetRequest() {
    var uri = uriWithPath(LOCALHOST);
    var responseBody = randomString();
    var responseCode = randomResponseCode();

    var mapping = createBasicStubForGetRequest(uri, responseBody, responseCode);
    var request = HttpRequest.newBuilder(uri).GET().build();
    return new TestSetup(responseBody, responseCode, null, null, mapping, request);
  }

  private static TestSetup createSetupWithPostRequestWithBody() {
    return createSetupWithRequestWithBody(POST);
  }

  private static TestSetup createSetupWithPutRequestWithBody() {
    return createSetupWithRequestWithBody(PUT);
  }

  private static TestSetup createSetupWithRequestWithBody(String method) {
    var uri = uriWithPath(LOCALHOST);
    var requestBody = randomString();
    var responseBody = randomString();
    var responseCode = randomResponseCode();

    MappingBuilder mapping;
    HttpRequest request;
    switch (method){
      case GET:
        throw new IllegalArgumentException("GET doesn't have a body");
      case POST:
        mapping = createBasicStubForPostRequest(uri, requestBody, responseBody, responseCode);
        request = HttpRequest.newBuilder(uri).POST(BodyPublishers.ofString(requestBody)).build();
        break;
      case PUT:
        mapping = createBasicStubForPutRequest(uri, requestBody, responseBody, responseCode);
        request = HttpRequest.newBuilder(uri).PUT(BodyPublishers.ofString(requestBody)).build();
        break;
      default:
        throw new IllegalArgumentException(METHOD_ERROR);
    }
    return new TestSetup(responseBody, responseCode, null, null, mapping, request);
  }

  private static TestSetup createSetupWithPostRequestWithEmptyBody() {
    return createSetupWithRequestWithEmptyBody(POST);
  }

  private static TestSetup createSetupWithPutRequestWithEmptyBody() {
    return createSetupWithRequestWithEmptyBody(PUT);
  }

  private static TestSetup createSetupWithRequestWithEmptyBody(String method) {
    var uri = uriWithPath(LOCALHOST);
    var responseBody = randomString();
    var responseCode = randomResponseCode();

    MappingBuilder mapping;
    HttpRequest request;
    switch (method) {
      case GET:
        throw new IllegalArgumentException("GET doesn't have a body");
      case POST:
        mapping = WireMock.post(WireMock.urlPathEqualTo(uri.getPath())).withRequestBody(WireMock.absent())
          .willReturn(aResponse().withBody(responseBody).withStatus(responseCode));
        request = HttpRequest.newBuilder(uri).POST(BodyPublishers.noBody()).build();
        break;
      case PUT:
        mapping = WireMock.put(WireMock.urlPathEqualTo(uri.getPath())).withRequestBody(WireMock.absent())
          .willReturn(aResponse().withBody(responseBody).withStatus(responseCode));
        request = HttpRequest.newBuilder(uri).PUT(BodyPublishers.noBody()).build();
        break;
      default:
        throw new IllegalArgumentException(METHOD_ERROR);
    }
    return new TestSetup(responseBody, responseCode, null, null, mapping, request);
  }

  private static TestSetup createSetupWithGetRequestWithQueryParams() {
    return createSetupWithRequestWithQueryParams(GET);
  }

  private static TestSetup createSetupWithPostRequestWithQueryParams() {
    return createSetupWithRequestWithQueryParams(POST);
  }

  private static TestSetup createSetupWithPutRequestWithQueryParams() {
    return createSetupWithRequestWithQueryParams(PUT);
  }

  private static TestSetup createSetupWithRequestWithQueryParams(String method) {
    var uri = uriWithParameters();
    var requestBody = randomString();
    var responseBody = randomString();
    var responseCode = randomResponseCode();

    MappingBuilder mapping;
    HttpRequest request;
    switch (method){
      case GET:
        mapping = createBasicStubForGetRequest(uri, responseBody, responseCode);
        addQueryParametersToStubMapping(uri, mapping);
        request = HttpRequest.newBuilder(uri).GET().build();
        break;
      case POST:
        mapping = createBasicStubForPostRequest(uri, requestBody, responseBody, responseCode);
        addQueryParametersToStubMapping(uri, mapping);
        request = HttpRequest.newBuilder(uri).POST(BodyPublishers.ofString(requestBody)).build();
        break;
      case PUT:
        mapping = createBasicStubForPutRequest(uri, requestBody, responseBody, responseCode);
        addQueryParametersToStubMapping(uri, mapping);
        request = HttpRequest.newBuilder(uri).PUT(BodyPublishers.ofString(requestBody)).build();
        break;
      default:
        throw new IllegalArgumentException(METHOD_ERROR);
    }
    return new TestSetup(responseBody, responseCode, null, null, mapping, request);
  }

  private static TestSetup createSetupWithGetRequestWithHeaders() {
    return createSetupWithRequestWithHeaders(GET);
  }

  private static TestSetup createSetupWithPostRequestWithHeaders() {
    return createSetupWithRequestWithHeaders(POST);
  }

  private static TestSetup createSetupWithPutRequestWithHeaders() {
    return createSetupWithRequestWithHeaders(PUT);
  }

  private static TestSetup createSetupWithRequestWithHeaders(String method) {
    var uri = uriWithPath(LOCALHOST);
    var requestBody = randomString();
    var requestHeader = randomString();
    var requestHeaderValue = randomString();
    var responseBody = randomString();
    var responseCode = randomResponseCode();

    MappingBuilder mapping;
    HttpRequest request;
    switch (method){
      case GET:
        mapping = createBasicStubForGetRequest(uri, responseBody, responseCode);
        addHeadersToStubMapping(mapping, requestHeader, requestHeaderValue);
        request = HttpRequest.newBuilder(uri).GET().header(requestHeader, requestHeaderValue).build();
        break;
      case POST:
        mapping = createBasicStubForPostRequest(uri, requestBody, responseBody, responseCode);
        addHeadersToStubMapping(mapping, requestHeader, requestHeaderValue);
        request = HttpRequest.newBuilder(uri).POST(BodyPublishers.ofString(requestBody))
          .header(requestHeader, requestHeaderValue).build();
        break;
      case PUT:
        mapping = createBasicStubForPutRequest(uri, requestBody, responseBody, responseCode);
        addHeadersToStubMapping(mapping, requestHeader, requestHeaderValue);
        request = HttpRequest.newBuilder(uri).PUT(BodyPublishers.ofString(requestBody))
          .header(requestHeader, requestHeaderValue).build();
        break;
      default:
        throw new IllegalArgumentException(METHOD_ERROR);
    }
    return new TestSetup(responseBody, responseCode, null, null, mapping, request);
  }

  private static TestSetup createSetupWithGetRequestExpectingResponseHeaders() {
    return createSetupWithRequestExpectingResponseHeaders(GET);
  }

  private static TestSetup createSetupWithPostRequestExpectingResponseHeaders() {
    return createSetupWithRequestExpectingResponseHeaders(POST);
  }

  private static TestSetup createSetupWithPutRequestExpectingResponseHeaders() {
    return createSetupWithRequestExpectingResponseHeaders(PUT);
  }

  private static TestSetup createSetupWithRequestExpectingResponseHeaders(String method) {
    var uri = uriWithPath(LOCALHOST);
    var requestBody = randomString();
    var responseHeaderKey = randomString();
    var responseHeaderValue = randomString();
    var responseBody = randomString();
    int responseCode = randomResponseCode();

    MappingBuilder mapping;
    HttpRequest request;
    switch (method){
      case GET:
        mapping = WireMock.get(WireMock.urlPathEqualTo(uri.getPath()))
          .willReturn(aResponse().withBody(responseBody).withStatus(responseCode)
            .withHeader(responseHeaderKey, responseHeaderValue));
        request = HttpRequest.newBuilder(uri).GET().build();
        break;
      case POST:
        mapping = WireMock.post(WireMock.urlPathEqualTo(uri.getPath()))
          .withRequestBody(WireMock.equalTo(requestBody))
          .willReturn(aResponse().withBody(responseBody).withStatus(responseCode)
            .withHeader(responseHeaderKey, responseHeaderValue));
        request = HttpRequest.newBuilder(uri).POST(BodyPublishers.ofString(requestBody)).build();
        break;
      case PUT:
        mapping = WireMock.put(WireMock.urlPathEqualTo(uri.getPath()))
          .withRequestBody(WireMock.equalTo(requestBody))
          .willReturn(aResponse().withBody(responseBody).withStatus(responseCode)
            .withHeader(responseHeaderKey, responseHeaderValue));
        request = HttpRequest.newBuilder(uri).PUT(BodyPublishers.ofString(requestBody)).build();
        break;
      default:
        throw new IllegalArgumentException(METHOD_ERROR);
    }
    return new TestSetup(responseBody, responseCode, responseHeaderKey, responseHeaderValue, mapping, request);
  }

  private static Integer randomResponseCode() {
    return randomElement(randomInteger(1000));
  }

  private static MappingBuilder createBasicStubForGetRequest(URI uri, String responseBody,
                                                             int responseCode) {
    return WireMock.get(WireMock.urlPathEqualTo(uri.getPath()))
      .willReturn(aResponse().withBody(responseBody).withStatus(responseCode));
  }

  private static MappingBuilder createBasicStubForPostRequest(URI uri, String requestBody,
                                                              String responseBody,
                                                              Integer responseCode) {
    return WireMock.post(WireMock.urlPathEqualTo(uri.getPath()))
      .withRequestBody(WireMock.equalTo(requestBody))
      .willReturn(aResponse().withBody(responseBody).withStatus(responseCode));
  }

  private static MappingBuilder createBasicStubForPutRequest(URI uri, String requestBody,
                                                             String responseBody,
                                                             Integer responseCode) {
    return WireMock.put(WireMock.urlPathEqualTo(uri.getPath()))
      .withRequestBody(WireMock.equalTo(requestBody))
      .willReturn(aResponse().withBody(responseBody).withStatus(responseCode));
  }

  private static void addQueryParametersToStubMapping(URI uri, MappingBuilder mapping) {
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

  private static URI uriWithParameters() {
    return UriWrapper.fromUri(LOCALHOST).addChild("some/path")
      .addQueryParameter(randomString(), randomString()).getUri();
  }

  private record TestSetup(String responseBody, Integer responseCode,
                           String responseHeader, String responseHeaderValue,
                           MappingBuilder mapping, HttpRequest request) {
  }
}