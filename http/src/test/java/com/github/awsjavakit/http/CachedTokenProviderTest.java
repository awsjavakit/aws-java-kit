package com.github.awsjavakit.http;

import static com.github.awsjavakit.http.JsonConfig.toJson;
import static com.github.awsjavakit.http.TokenProvider.locallyCachedTokenProvider;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.gtihub.awsjavakit.attempt.Try.attempt;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.awsjavakit.http.token.OAuthTokenResponse;
import com.github.awsjavakit.misc.paths.UnixPath;
import com.github.awsjavakit.misc.paths.UriWrapper;
import com.github.awsjavakit.testingutils.networking.WiremockHttpClient;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.net.http.HttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CachedTokenProviderTest {

  private static final UnixPath AUTH_PATH = UnixPath.of("/oauth2/token");

  private WireMockServer server;
  private String clientId;
  private String clientSecret;
  private String accessToken;
  private HttpClient httpClient;
  private Oauth2Credentials authCredentialsProvider;

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
    this.authCredentialsProvider =
      new Oauth2Credentials(authEndpoint, clientId, clientSecret, randomString());

  }

  @Test
  void shouldFetchNewTokenWhenCalledForTheFirstTime() {
    setupAuthResponse(600);
    var tokenProvider = locallyCachedTokenProvider(httpClient,
      authCredentialsProvider);

    var actualToken = tokenProvider.fetchToken();
    assertThat(actualToken.value()).isEqualTo(this.accessToken);
  }

  @Test
  void shouldNotFetchNewTokenWhenCalledForTheSecondTimeAndThereIsValidCachedToken() {
    setupAuthResponse(600);
    var tokenProvider =
      locallyCachedTokenProvider(httpClient,
        authCredentialsProvider);
    tokenProvider.fetchToken();
    var actualToken = tokenProvider.fetchToken();
    server.verify(exactly(1), postRequestedFor(urlPathEqualTo(AUTH_PATH.toString())));
    assertThat(actualToken.value()).isEqualTo(this.accessToken);
  }

  @Test
  void shouldFetchNewTokenWhenCachedTokenIsEstimatedToBeExpired() {
    setupAuthResponse(0);
    var tokenProvider =
      locallyCachedTokenProvider(httpClient, authCredentialsProvider);
    tokenProvider.fetchToken();
    var actualToken = tokenProvider.fetchToken();
    server.verify(exactly(2), postRequestedFor(urlPathEqualTo(AUTH_PATH.toString())));
    assertThat(actualToken.value()).isEqualTo(this.accessToken);
  }

  @Test
  void shouldReturnTheTypeOfTokenItFetches() {
    setupAuthResponse(600);
    var tokenProvider = locallyCachedTokenProvider(httpClient, authCredentialsProvider);
    var token = tokenProvider.fetchToken();
    assertThat(tokenProvider.getTag()).isEqualTo(token.tag());
    assertThat(tokenProvider.getTag()).isEqualTo(authCredentialsProvider.getTag());
  }

  private void setupAuthResponse(int tokenDurationInSeconds) {
    server.stubFor(post(urlPathEqualTo(AUTH_PATH.toString()))
      .withBasicAuth(clientId, clientSecret)
      .withFormParam("grant_type", equalTo("client_credentials"))
      .willReturn(
        aResponse().withStatus(HTTP_OK).withBody(createResponse(tokenDurationInSeconds))));

  }

  private String createResponse(int tokenDurationInSeconds) {
    var response = new OAuthTokenResponse(accessToken, tokenDurationInSeconds);
    return attempt(() -> toJson(response)).orElseThrow();

  }

}