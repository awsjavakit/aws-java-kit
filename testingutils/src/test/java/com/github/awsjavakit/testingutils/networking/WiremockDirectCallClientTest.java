package com.github.awsjavakit.testingutils.networking;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.direct.DirectCallHttpServer;
import com.github.tomakehurst.wiremock.direct.DirectCallHttpServerFactory;
import com.github.tomakehurst.wiremock.http.Request;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

class WiremockDirectCallClientTest {

  @Test
  void shouldReplicateServer() throws IOException, InterruptedException {
    DirectCallHttpServerFactory factory = new DirectCallHttpServerFactory();
    WireMockServer wm = new WireMockServer(wireMockConfig().httpServerFactory(factory));
    wm.start(); // no-op, not required
    DirectCallHttpServer server = factory.getHttpServer();

    wm.stubFor(WireMock.get(WireMock.urlPathEqualTo("/helloWorld"))
      .willReturn(aResponse()
        .withBody("Aaaaaaa")
        .withStatus(HttpURLConnection.HTTP_OK)));

    var client = new WiremockDirectCallClient(server);
    HttpRequest request = HttpRequest.newBuilder()
      .GET()
      .uri(URI.create("https://example.com/helloWorld"))
      .build();

    var response = client.send(request, HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
    assertThat(response.body(), is(equalTo("Aaaaaaa")));

  }

}