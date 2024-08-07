package com.github.awsjavakit.testingutils.networking;

import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomElement;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomInteger;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
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
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class WiremockDirectCallClientTest {

  public static final String LOCALHOST = "https://localhost";
  private WireMockServer directCallServer;
  private HttpClient directCallClient;

  public static Stream<URI> uriProvider() {
    return Stream.of(uriWithPath(LOCALHOST),
      uriWithParameters(),
      uriWithMultipleChildren());
  }

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
  @MethodSource("uriProvider")
  void shouldTransformWiremockResponsesForGetRequests(URI uri)
    throws IOException, InterruptedException {
    var expectedResponseBody = randomString();
    var expectedResponseCode = randomResponseCode();

    var mapping = createBasicStubForGetRequest(uri, expectedResponseBody, expectedResponseCode);
    addQueryParametersToStubMapping(uri, mapping);
    directCallServer.stubFor(mapping);

    var request = HttpRequest.newBuilder(uri).GET().build();
    var response = directCallClient.send(request, BodyHandlers.ofString());

    assertThat(response.body(), response.statusCode(), is(equalTo(expectedResponseCode)));
    assertThat(response.body(), is(equalTo(expectedResponseBody)));
  }

  @ParameterizedTest
  @MethodSource("uriProvider")
  void shouldIncludeHeadersForGetRequests(URI uri) throws IOException, InterruptedException {
    var requestHeaderKey = randomString();
    var requestHeaderValue = randomString();
    int expectedResponseCode = randomResponseCode();
    var expectedResponseBody = randomString();

    var mapping = createBasicStubForGetRequest(uri, expectedResponseBody, expectedResponseCode);
    addQueryParametersToStubMapping(uri, mapping);
    addHeadersToStubMapping(mapping, requestHeaderKey, requestHeaderValue);
    directCallServer.stubFor(mapping);

    var request =
      HttpRequest.newBuilder(uri).GET().header(requestHeaderKey, requestHeaderValue).build();
    var response = directCallClient.send(request, BodyHandlers.ofString());

    assertThat(response.body(), response.statusCode(), is(equalTo(expectedResponseCode)));
    assertThat(response.body(), is(equalTo(expectedResponseBody)));
  }

  @Test
  void shouldTransformPostRequestWithBody() throws IOException, InterruptedException {
    var uri = uriWithPath(LOCALHOST);
    var requestBody = randomString();
    var expectedResponseBody = randomString();
    var expectedResponseCode = randomResponseCode();

    var mapping =
      createBasicStubForPostRequest(uri, requestBody, expectedResponseBody, expectedResponseCode);
    directCallServer.stubFor(mapping);

    var request = HttpRequest.newBuilder(uri).POST(BodyPublishers.ofString(requestBody)).build();
    var response = directCallClient.send(request, BodyHandlers.ofString());

    assertThat(response.body(), response.statusCode(), is(equalTo(expectedResponseCode)));
    assertThat(response.body(), is(equalTo(expectedResponseBody)));
  }

  @Test
  void shouldTransformPostRequestWithoutBody() throws IOException, InterruptedException {
    var uri = uriWithPath(LOCALHOST);
    var expectedResponseBody = randomString();
    var expectedResponseCode = randomResponseCode();

    var mapping = WireMock.post(WireMock.urlPathEqualTo(uri.getPath()))
      .withRequestBody(WireMock.absent())
      .willReturn(aResponse().withBody(expectedResponseBody).withStatus(expectedResponseCode));
    directCallServer.stubFor(mapping);

    var request = HttpRequest.newBuilder(uri).POST(BodyPublishers.noBody()).build();
    var response = directCallClient.send(request, BodyHandlers.ofString());

    assertThat(response.body(), response.statusCode(), is(equalTo(expectedResponseCode)));
    assertThat(response.body(), is(equalTo(expectedResponseBody)));
  }

  @Test
  void shouldIncludeQueryParametersForPostRequests() throws IOException, InterruptedException {
    var uri = uriWithParameters();
    var requestBody = randomString();
    var expectedResponseBody = randomString();
    int expectedResponseCode = randomResponseCode();

    var mapping =
      createBasicStubForPostRequest(uri, requestBody, expectedResponseBody, expectedResponseCode);
    addQueryParametersToStubMapping(uri, mapping);
    directCallServer.stubFor(mapping);

    var request = HttpRequest.newBuilder(uri).POST(BodyPublishers.ofString(requestBody)).build();
    var response = directCallClient.send(request, BodyHandlers.ofString());

    assertThat(response.body(), response.statusCode(), is(equalTo(expectedResponseCode)));
    assertThat(response.body(), is(equalTo(expectedResponseBody)));
  }

  @Test
  void shouldIncludeHeadersForPostRequests() throws IOException, InterruptedException {
    var uri = uriWithPath(LOCALHOST);
    var requestBody = randomString();
    var requestHeader = randomString();
    var requestHeaderValue = randomString();
    int expectedResponseCode = randomResponseCode();
    var expectedResponseBody = randomString();

    var mapping =
      createBasicStubForPostRequest(uri, requestBody, expectedResponseBody, expectedResponseCode);
    addHeadersToStubMapping(mapping, requestHeader, requestHeaderValue);
    directCallServer.stubFor(mapping);

    var request = HttpRequest.newBuilder(uri).POST(BodyPublishers.ofString(requestBody))
      .header(requestHeader, requestHeaderValue).build();
    var response = directCallClient.send(request, BodyHandlers.ofString());

    assertThat(response.body(), response.statusCode(), is(equalTo(expectedResponseCode)));
    assertThat(response.body(), is(equalTo(expectedResponseBody)));
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

  private static URI uriWithMultipleChildren() {
    return UriWrapper.fromUri(LOCALHOST).addChild("some/path")
      .addChild("second/path")
      .getUri();
  }

}