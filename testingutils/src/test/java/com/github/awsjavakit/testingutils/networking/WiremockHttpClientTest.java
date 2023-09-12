package com.github.awsjavakit.testingutils.networking;

import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomInteger;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.awsjavakit.misc.paths.UriWrapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WiremockHttpClientTest {

  public static final int LAST_WELL_KNOWN_PORT = 1024;
  public static final String SOME_PATH = "/hello";
  private WireMockServer server;
  private URI serverUri;
  private HttpClient httpClient;

  @BeforeEach
  public void init() {
    this.server = new WireMockServer(options().httpDisabled(true).httpsPort(randomPort()));
    server.start();
    this.serverUri = URI.create(server.baseUrl());
    this.httpClient = WiremockHttpClient.create().build();
  }

  @Test
  void shouldReturnAClientThatIsAbleToWorkWithWiremockOverHttps()
    throws IOException, InterruptedException {
    var expectedBody = randomString();
    server.stubFor(get(urlPathEqualTo(SOME_PATH))
      .willReturn(aResponse()
        .withBody(expectedBody)
        .withStatus(HTTP_OK)));
    var queryUri = UriWrapper.fromUri(serverUri).addChild(SOME_PATH).getUri();

    var request = HttpRequest.newBuilder(queryUri).GET().build();
    var response = httpClient.send(request, BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(HTTP_OK);
    assertThat(response.body()).isEqualTo(expectedBody);

  }

  private Integer randomPort() {
    return LAST_WELL_KNOWN_PORT + randomInteger(1000);
  }

}