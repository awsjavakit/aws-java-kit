package com.github.awsjavakit.testingutils.networking;

import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;

import com.github.awsjavakit.misc.paths.UriWrapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.direct.DirectCallHttpServerFactory;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WiremockDirectCallClientTest {

  private WireMockServer server;
  private HttpClient client;

  @BeforeEach
  public void init() {
    var factory = new DirectCallHttpServerFactory();

    this.server = new WireMockServer(options()
      .notifier(new ConsoleNotifier(true))
      .httpServerFactory(factory)
      .dynamicHttpsPort().httpDisabled(true));
    server.start(); // no-op, not required
    this.client = new WiremockDirectCallClient(factory.getHttpServer());
  }

  @AfterEach
  public void stop() {
    this.server.stop();
  }

  @Test
  void shouldTransformWiremockResponsesForGetRequests() throws IOException, InterruptedException {
    var expectedBody = randomString();
    var uri = UriWrapper.fromUri("https://localhost").addChild("some/path").getUri();
    server.stubFor(WireMock.get(WireMock.urlPathEqualTo(uri.getPath()))
      .willReturn(aResponse()
        .withBody(expectedBody)
        .withStatus(HttpURLConnection.HTTP_OK)));

    var request = HttpRequest.newBuilder()
      .GET()
      .uri(UriWrapper.fromUri(uri).getUri())
      .build();

    var response = client.send(request, HttpResponse.BodyHandlers.ofString());
    assertThat(response.body(), response.statusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
    assertThat(response.body(), is(equalTo(expectedBody)));

  }

}