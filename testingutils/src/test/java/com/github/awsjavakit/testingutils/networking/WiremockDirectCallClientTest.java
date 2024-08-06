package com.github.awsjavakit.testingutils.networking;

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
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.direct.DirectCallHttpServerFactory;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class WiremockDirectCallClientTest {

  public static final boolean CASE_INSENSITIVE = true;
  private WireMockServer server;
  private HttpClient client;

  public static Stream<URI> uriProvider() {
    return Stream.of(emptyUri(), uriWithParameters(), uriWithMultipleChildren());
  }

  @BeforeEach
  public void init() {
    var factory = new DirectCallHttpServerFactory();

    this.server = new WireMockServer(
      options().notifier(new ConsoleNotifier(true)).httpServerFactory(factory).dynamicHttpsPort()
        .httpDisabled(true));
    server.start(); // no-op, not required
    this.client = new WiremockDirectCallClient(factory.getHttpServer());
  }

  @AfterEach
  public void stop() {
    this.server.stop();
  }

  @ParameterizedTest
  @MethodSource("uriProvider")
  void shouldTransformWiremockResponsesForGetRequests(URI uri)
    throws IOException, InterruptedException {
    var expectedBody = randomString();
    var mapping = createStubForGetRequest(uri, expectedBody);
    server.stubFor(mapping);

    var request = HttpRequest.newBuilder()
      .GET()
      .uri(UriWrapper.fromUri(uri).getUri())
      .build();

    var response = client.send(request, HttpResponse.BodyHandlers.ofString());
    assertThat(response.body(), response.statusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
    assertThat(response.body(), is(equalTo(expectedBody)));

  }

  private static MappingBuilder createStubForGetRequest(URI uri, String expectedBody) {
    var mapping = WireMock.get(WireMock.urlPathEqualTo(uri.getPath()));
    mapping = addQueryParameters(uri, mapping);
    mapping = mapping.willReturn(aResponse()
      .withBody(expectedBody)
      .withStatus(HttpURLConnection.HTTP_OK));
    return mapping;
  }

  private static MappingBuilder addQueryParameters(URI uri, MappingBuilder mapping) {
    var newMapping = mapping;
    if(StringUtils.isNotBlank(uri.getRawQuery())) {
      var queryParameters = UriWrapper.fromUri(uri).getQueryParameters();
      for (var entry : queryParameters.entrySet()) {
        newMapping = newMapping.withQueryParam(entry.getKey(), WireMock.equalTo(entry.getValue()));
      }
    }
    return newMapping;
  }

  private static URI emptyUri() {
    return UriWrapper.fromUri("https://localhost").addChild("some/path").getUri();
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