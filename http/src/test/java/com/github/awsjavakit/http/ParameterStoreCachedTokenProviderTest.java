package com.github.awsjavakit.http;

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
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.awsjavakit.misc.paths.UnixPath;
import com.github.awsjavakit.misc.paths.UriWrapper;
import com.github.awsjavakit.testingutils.aws.FakeSsmClient;
import com.github.awsjavakit.testingutils.networking.WiremockHttpClient;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.PutParameterRequest;
import software.amazon.awssdk.services.ssm.model.PutParameterResponse;

class ParameterStoreCachedTokenProviderTest {

  public static final int FIRST_TIME_FAILURE_SECOND_TIME_SUCCESS = 2;
  public static final int WRITE_ON_FAILURE = 1;
  private static final UnixPath AUTH_PATH = UnixPath.of("/oauth2/token");
  private static final ObjectMapper JSON = JsonMapper.builder()
    .addModule(new JavaTimeModule())
    .addModule(new Jdk8Module())
    .build();
  private static final Duration SOME_LARGE_DURATION = Duration.ofDays(1);
  private WireMockServer server;
  private String clientId;
  private String clientSecret;
  private String accessToken;
  private HttpClient httpClient;
  private SimpleCredentialsProvider authCredentialsProvider;
  private FakeSsmClientWithCounters ssmClient;

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
    this.ssmClient = new FakeSsmClientWithCounters();

    this.httpClient = WiremockHttpClient.create().build();
    this.authCredentialsProvider =
      new SimpleCredentialsProvider(clientId, clientSecret, authEndpoint);
    setupAuthResponse();
  }

  @Test
  void shouldFetchNewTokenWhenCalledForTheFirstTime() {
    var tokenRefresher = new NewTokenProvider(httpClient, authCredentialsProvider);
    var tokenProvider = new ParameterStoreCachedTokenProvider(
      tokenRefresher,
      ssmClient,
      SOME_LARGE_DURATION,
      JSON);

    var actualToken = tokenProvider.fetchToken();
    assertThat(actualToken).isEqualTo(this.accessToken);
  }

  @Test
  void shouldReuseTokenStoredInSsmWhenTokenExistsInSsmAndIsValid() {
    var tokenRefresher = new NewTokenProvider(httpClient, authCredentialsProvider);
    var tokenProvider = new ParameterStoreCachedTokenProvider(
      tokenRefresher,
      ssmClient,
      SOME_LARGE_DURATION,
      JSON);
    tokenProvider.fetchToken();
    var actualToken = tokenProvider.fetchToken();

    server.verify(exactly(1), postRequestedFor(urlPathEqualTo(AUTH_PATH.toString())));
    assertThat(ssmClient.getReadCounter().get()).isEqualTo(FIRST_TIME_FAILURE_SECOND_TIME_SUCCESS);
    assertThat(ssmClient.getWriteCounter().get()).isEqualTo(WRITE_ON_FAILURE);
    assertThat(actualToken).isEqualTo(this.accessToken);
  }

  @Test
  void shouldFetchTokenFromOAuthWhenTokenExistsButIsInvalid() {
    var tokenRefresher = new NewTokenProvider(httpClient, authCredentialsProvider);
    var tokenProvider = new ParameterStoreCachedTokenProvider(
      tokenRefresher,
      ssmClient,
      SOME_LARGE_DURATION,
      JSON);
    tokenProvider.fetchToken();
    var actualToken = tokenProvider.fetchToken();

    server.verify(exactly(1), postRequestedFor(urlPathEqualTo(AUTH_PATH.toString())));
    assertThat(ssmClient.getReadCounter().get()).isEqualTo(FIRST_TIME_FAILURE_SECOND_TIME_SUCCESS);
    assertThat(ssmClient.getWriteCounter().get()).isEqualTo(WRITE_ON_FAILURE);
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

  private static class FakeSsmClientWithCounters extends FakeSsmClient {

    private final AtomicInteger readCounter = new AtomicInteger();
    private final AtomicInteger writeCounter = new AtomicInteger();

    public FakeSsmClientWithCounters() {
      super();
    }

    public AtomicInteger getReadCounter() {
      return readCounter;
    }

    public AtomicInteger getWriteCounter() {
      return writeCounter;
    }

    @Override
    public GetParameterResponse getParameter(GetParameterRequest getParameterRequest) {
      readCounter.incrementAndGet();
      return super.getParameter(getParameterRequest);
    }

    @Override
    public PutParameterResponse putParameter(PutParameterRequest putParameterRequest) {
      writeCounter.incrementAndGet();
      return super.putParameter(putParameterRequest);
    }
  }

}