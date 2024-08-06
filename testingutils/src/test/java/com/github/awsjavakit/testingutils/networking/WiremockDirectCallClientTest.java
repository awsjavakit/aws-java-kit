package com.github.awsjavakit.testingutils.networking;

import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;

import com.github.awsjavakit.misc.StringUtils;
import com.github.awsjavakit.misc.paths.UriWrapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.direct.DirectCallHttpServer;
import com.github.tomakehurst.wiremock.direct.DirectCallHttpServerFactory;
import com.github.tomakehurst.wiremock.http.ImmutableRequest;
import com.github.tomakehurst.wiremock.http.RequestMethod;
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

  private WireMockServer directCallServer;
  private HttpClient directCallClient;
  private WireMockServer jettyServer;
  private HttpClient jettyClient;
  private DirectCallHttpServer directCallHttpServer;

  public static Stream<URI> uriProvider() {
    return Stream.of(uriWithPath("https://localhost"), uriWithParameters(), uriWithMultipleChildren());
  }

  @BeforeEach
  public void init() {
    var factory = new DirectCallHttpServerFactory();

    this.directCallServer = new WireMockServer(
      options()
        .httpServerFactory(factory));
    directCallServer.start(); // no-op, not required

    this.jettyServer = new WireMockServer(
      options().dynamicHttpsPort().httpDisabled(true)
    );
    jettyServer.start();

    this.directCallHttpServer = factory.getHttpServer();
    this.directCallClient = new WiremockDirectCallClient(directCallHttpServer);
    this.jettyClient = WiremockHttpClient.create().build();
  }

  @AfterEach
  public void stop() {
    this.directCallServer.stop();
  }

  @ParameterizedTest
  @MethodSource("uriProvider")
  void shouldTransformWiremockResponsesForGetRequests(URI uri)
    throws IOException, InterruptedException {
    var expectedBody = randomString();
    var mapping = createStubForGetRequest(uri, expectedBody);
    directCallServer.stubFor(mapping);

    var request = HttpRequest.newBuilder().GET().uri(UriWrapper.fromUri(uri).getUri()).build();

    var response = directCallClient.send(request, BodyHandlers.ofString());
    assertThat(response.body(), response.statusCode(), is(equalTo(HTTP_OK)));
    assertThat(response.body(), is(equalTo(expectedBody)));

  }

  @Test
  void shouldPost() throws IOException, InterruptedException {
    var uri = uriWithPath("https://localhost");
    var expectedResponseBody = randomString();
    var expectedRequestBody = "ExpectedRequestBody";

    var mapping = WireMock.post(WireMock.urlPathEqualTo(uri.getPath()))
      .withRequestBody(WireMock.equalTo(expectedRequestBody))
      .willReturn(aResponse().withBody(expectedResponseBody).withStatus(HTTP_OK));
    directCallServer.stubFor(mapping);

    var request = HttpRequest.newBuilder(uri).POST(BodyPublishers.ofString(expectedRequestBody)).build();

    var response = directCallClient.send(request, BodyHandlers.ofString());

    assertThat(response.body(), response.statusCode(), is(equalTo(HTTP_OK)));
    assertThat(response.body(), is(equalTo(expectedResponseBody)));
  }

  @Test
  void shouldFailWhenSubmittingWrongRequestBodyDirectCallVersion() throws IOException, InterruptedException {
    var uri = uriWithPath("https://localhost");
    var expectedResponseBody = randomString();
    var expectedRequestBody = "ExpectedRequestBody";
    var wrongRequestBody = "WrongRequestBody";

    directCallServer.stubFor(WireMock.post(WireMock.urlPathEqualTo(uri.getPath()))
      .withRequestBody(WireMock.equalTo(expectedRequestBody))
      .willReturn(aResponse().withBody(expectedResponseBody).withStatus(HTTP_OK)));

    var wireMockRequest = ImmutableRequest.create()
      .withAbsoluteUrl(uri.toString())
      .withMethod(RequestMethod.POST)
//      .withBody(expectedRequestBody.getBytes(StandardCharsets.UTF_8))
      .build();
    var response = directCallHttpServer.stubRequest(wireMockRequest);

    assertThat(response.getBodyAsString(), response.getStatus(), is(equalTo(HTTP_OK)));
    assertThat(response.getBodyAsString(), is(equalTo(expectedResponseBody)));
  }

  @Test
  void shouldFailWhenSubmittingWrongRequestBodyJettyVersion() throws IOException, InterruptedException {
    var uri = uriWithPath(jettyServer.baseUrl());
    var expectedResponseBody = randomString();
    var expectedRequestBody = "ExpectedRequestBody";
    var wrongRequestBody = "WrongRequestBody";

    jettyServer.stubFor(WireMock.post(WireMock.urlPathEqualTo(uri.getPath()))
      .withRequestBody(WireMock.equalTo(expectedRequestBody))
      .willReturn(aResponse().withBody(expectedResponseBody).withStatus(HTTP_OK)));

    var request = HttpRequest.newBuilder(uri).POST(BodyPublishers.noBody()).build();
    var response = jettyClient.send(request, BodyHandlers.ofString());

    assertThat(response.body(), response.statusCode(), is(equalTo(HTTP_OK)));
    assertThat(response.body(), is(equalTo(expectedResponseBody)));
  }

  private static MappingBuilder createStubForGetRequest(URI uri, String expectedBody) {
    var mapping = WireMock.get(WireMock.urlPathEqualTo(uri.getPath()));
    mapping = addQueryParameters(uri, mapping);
    mapping =
      mapping.willReturn(aResponse().withBody(expectedBody).withStatus(HTTP_OK));
    return mapping;
  }

  private static MappingBuilder addQueryParameters(URI uri, MappingBuilder mapping) {
    var newMapping = mapping;
    if (StringUtils.isNotBlank(uri.getRawQuery())) {
      var queryParameters = UriWrapper.fromUri(uri).getQueryParameters();
      for (var entry : queryParameters.entrySet()) {
        newMapping = newMapping.withQueryParam(entry.getKey(), WireMock.equalTo(entry.getValue()));
      }
    }
    return newMapping;
  }

  private static URI uriWithPath(String hostUri) {
    return UriWrapper.fromUri(hostUri).addChild("some/path").getUri();
  }

  private static URI uriWithParameters() {
    return UriWrapper.fromUri("https://localhost").addChild("some/path")
      .addQueryParameter(randomString(), randomString()).getUri();
  }

  private static URI uriWithMultipleChildren() {
    return UriWrapper.fromUri("https://localhost").addChild("some/path").addChild("second/path")
      .getUri();
  }

}