package com.github.awsjavakit.http;

import static com.github.awsjavakit.http.JsonConfig.toJson;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.awsjavakit.misc.paths.UnixPath;
import com.github.awsjavakit.misc.paths.UriWrapper;
import com.github.awsjavakit.testingutils.networking.WiremockHttpClient;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.net.http.HttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NewTokenProviderTest {

  public static final UnixPath AUTH_PATH = UnixPath.of("/oauth2/token");
  public static final int SOME_VALIDITY_PERIOD = 600;
  private WireMockServer server;
  private HttpClient httpClient;
  private String clientId;
  private String clientSecret;
  private String accessToken;
  private Oauth2Credentials authCredentials;

  @BeforeEach
  public void init() {
    this.server = new WireMockServer(options().httpDisabled(true).dynamicHttpsPort());
    server.start();
    this.clientId = randomString();
    this.clientSecret = randomString();
    this.accessToken = randomString();
    var authEndpoint = UriWrapper.fromUri(server.baseUrl())
      .addChild(AUTH_PATH)
      .getUri();
    this.httpClient = WiremockHttpClient.create().build();
    this.authCredentials =
      new Oauth2Credentials(authEndpoint, clientId, clientSecret, randomString());

  }

  @Test
  void shouldFetchTokenFromServer() {
    setupAuthHandshake();

    var tokenProvider = TokenProvider.defaultProvider(httpClient, authCredentials);
    var token = tokenProvider.fetchToken();
    assertThat(token.value()).isEqualTo(accessToken);
  }

  @Test
  void shouldTagTokenWithSameTagAsTheClientCredentials() {
    setupAuthHandshake();

    var tokenProvider = TokenProvider.defaultProvider(httpClient, authCredentials);
    var token = tokenProvider.fetchToken();
    assertThat(token.tag()).isEqualTo(authCredentials.getTag());
  }

  @Test
  void shouldReportTagWhenErrorOccurs() {
    setupAuthHandshake();
    var wrongCredentials = new Oauth2Credentials(authCredentials.getAuthEndpointUri(), randomString(),
      randomString(), authCredentials.getTag());
    var tokenProvider = TokenProvider.defaultProvider(httpClient, wrongCredentials);
    var exception = assertThrows(Exception.class, tokenProvider::fetchToken);
    assertThat(exception.getMessage()).contains(wrongCredentials.getTag());
  }

  private void setupAuthHandshake() {
    server.stubFor(post(urlPathEqualTo(AUTH_PATH.toString()))
      .withBasicAuth(clientId, clientSecret)
      .withFormParam("grant_type", WireMock.equalTo("client_credentials"))
      .willReturn(aResponse().withStatus(HTTP_OK).withBody(createAuthResponse()))
    );
  }

  private String createAuthResponse() {
    var response = JsonConfig.JSON.createObjectNode();
    response.put("access_token", accessToken);
    response.put("expires_in", SOME_VALIDITY_PERIOD);
    return toJson(response);
  }

}