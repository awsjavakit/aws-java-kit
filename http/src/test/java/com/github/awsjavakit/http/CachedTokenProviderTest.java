package com.github.awsjavakit.http;

import static com.github.awsjavakit.http.TokenProvider.locallyCachedTokenProvider;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.awsjavakit.misc.paths.UnixPath;
import com.github.awsjavakit.misc.paths.UriWrapper;
import com.github.awsjavakit.testingutils.networking.WiremockHttpClient;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.net.http.HttpClient;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CachedTokenProviderTest {

  public static final Duration SOME_LARGE_DURATION = Duration.ofDays(1);
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final UnixPath AUTH_PATH = UnixPath.of("/oauth2/token");

  private WireMockServer server;
  private String clientId;
  private String clientSecret;
  private String accessToken;
  private HttpClient httpClient;
  private SimpleCredentialsProvider authCredentialsProvider;

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
      new SimpleCredentialsProvider(clientId, clientSecret, authEndpoint);
    setupAuthResponse();
  }


  @Test
  void shouldFetchNewTokenWhenCalledForTheFirstTime() {
    var tokenProvider = locallyCachedTokenProvider(httpClient,
      authCredentialsProvider,
      SOME_LARGE_DURATION);

    var actualToken = tokenProvider.fetchToken();
    assertThat(actualToken).isEqualTo(this.accessToken);
  }

  @Test
  void shouldNotFetchNewTokenWhenCalledForTheSecondTimeAndThereIsValidCachedToken() {
    var tokenProvider =
      locallyCachedTokenProvider(httpClient,
        authCredentialsProvider,
        SOME_LARGE_DURATION);
    tokenProvider.fetchToken();
    var actualToken = tokenProvider.fetchToken();
    server.verify(exactly(1), postRequestedFor(urlPathEqualTo(AUTH_PATH.toString())));
    assertThat(actualToken).isEqualTo(this.accessToken);
  }

  @Test
  void shouldFetchNewTokenWhenCachedTokenIsEstimatedToBeExpired() {
    var tokenProvider =
      locallyCachedTokenProvider(httpClient,
        authCredentialsProvider,
        Duration.ZERO);
    tokenProvider.fetchToken();
    var actualToken = tokenProvider.fetchToken();
    server.verify(exactly(2), postRequestedFor(urlPathEqualTo(AUTH_PATH.toString())));
    assertThat(actualToken).isEqualTo(this.accessToken);
  }

  private void setupAuthResponse() {
    server.stubFor(post(urlPathEqualTo(AUTH_PATH.toString()))
      .withBasicAuth(clientId, clientSecret)
      .withFormParam("grant_type", equalTo("client_credentials"))
      .willReturn(aResponse().withStatus(HTTP_OK).withBody(createResponse())));

  }

  private String createResponse() {
    return new OAuthResponse(accessToken, randomString(), randomString())
      .toJsonString(JSON);
  }


}