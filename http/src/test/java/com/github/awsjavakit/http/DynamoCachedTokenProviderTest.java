package com.github.awsjavakit.http;

import static com.github.awsjavakit.http.JsonConfig.toJson;
import static com.github.awsjavakit.http.OAuthTokenEntryConverter.BODY_FIELD;
import static com.github.awsjavakit.http.OAuthTokenEntryConverter.fromItem;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.awsjavakit.http.token.OAuthTokenEntry;
import com.github.awsjavakit.misc.paths.UnixPath;
import com.github.awsjavakit.misc.paths.UriWrapper;
import com.github.awsjavakit.testingutils.networking.WiremockHttpClient;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

class DynamoCachedTokenProviderTest extends DynamoTest {

  public static final UnixPath AUTH_PATH = UnixPath.of("/oauth2/token");
  private WireMockServer server;
  private String clientId;
  private String clientSecret;
  private String accessToken;
  private HttpClient httpClient;
  private URI authUri;
  private String tableName;

  @BeforeEach
  public void init() {

    this.server = new WireMockServer(options().httpDisabled(true).dynamicHttpsPort());
    server.start();
    var serverUri = URI.create(server.baseUrl());
    this.clientId = randomString();
    this.clientSecret = randomString();
    this.accessToken = randomString();
    this.httpClient = WiremockHttpClient.create().build();
    this.authUri = UriWrapper.fromUri(serverUri).addChild(AUTH_PATH).getUri();
    this.tableName = randomString();

    this.initDatabase(tableName);
  }

  @Test
  void shouldFetchTokenWhenTokenDoesNotExist() {
    setupAuthHandshake(600);
    var tokenProvider = new SimpleDynamoCachedTokenProvider(createNewTokenProvider(), dynamoClient,
      tableName);
    var token = tokenProvider.fetchToken();
    assertThat(token.value()).isEqualTo(accessToken);
  }

  @Test
  void shouldStoreAuthTokenToDynamo() {
    setupAuthHandshake(600);
    var tokenProvider = new SimpleDynamoCachedTokenProvider(createNewTokenProvider(), dynamoClient,
      tableName);
    var token = tokenProvider.fetchToken();
    var response = dynamoClient.getItem(createGetRequest(token));
    var tokenEntry = fromItem(response.item());
    assertThat(tokenEntry.value()).isEqualTo(accessToken);

  }

  @Test
  void shouldFetchTokenFromDynamoWhenTokenExistsAndIsValid() {
    setupAuthHandshake(600);
    var tokenProvider = new SimpleDynamoCachedTokenProvider(createNewTokenProvider(), dynamoClient,
      tableName);
    tokenProvider.fetchToken();
    var token = tokenProvider.fetchToken();

    server.verify(exactly(1), postRequestedFor(urlPathEqualTo(AUTH_PATH.toString())));
    assertThat(token.value()).isEqualTo(accessToken);
  }

  @Test
  void shouldTagTokenWithSameTagAsTheClientCredentials() {
    setupAuthHandshake(600);
    var credentials = newCredentials();
    var tokenProvider = new SimpleDynamoCachedTokenProvider(createNewTokenProvider(credentials),
      dynamoClient, tableName);
    tokenProvider.fetchToken();
    tokenProvider.fetchToken();
    var token = tokenProvider.fetchToken();

    server.verify(exactly(1), postRequestedFor(urlPathEqualTo(AUTH_PATH.toString())));
    assertThat(token.tag()).isEqualTo(credentials.getTag());
  }

  @Test
  void shouldRefreshTokenWhenTokenExistsAndIsInvalid() {
    setupAuthHandshake(0);
    var tokenProvider = new SimpleDynamoCachedTokenProvider(createNewTokenProvider(), dynamoClient,
      tableName);
    tokenProvider.fetchToken();
    var token = tokenProvider.fetchToken();

    server.verify(exactly(2), postRequestedFor(urlPathEqualTo(AUTH_PATH.toString())));
    assertThat(token.value()).isEqualTo(accessToken);
  }

  private Oauth2Credentials newCredentials() {
    return new Oauth2Credentials(authUri, clientId, clientSecret, randomString());
  }

  private GetItemRequest createGetRequest(OAuthTokenEntry token) {
    var keyValue = AttributeValue.builder().s(token.type()).build();
    var key = Map.of(PARTITION_KEY, keyValue, SORT_KEY, keyValue);
    return GetItemRequest.builder().key(key).tableName(tableName).build();
  }

  private TokenProvider createNewTokenProvider(OAuthCredentialsProvider credentials) {
    return TokenProvider.defaultProvider(httpClient, credentials);

  }

  private TokenProvider createNewTokenProvider() {
    return createNewTokenProvider(newCredentials());
  }

  private void setupAuthHandshake(int validityPeriod) {
    server.stubFor(post(urlPathEqualTo(AUTH_PATH.toString())).withBasicAuth(clientId, clientSecret)
      .withFormParam("grant_type", WireMock.equalTo("client_credentials"))
      .willReturn(aResponse().withStatus(HTTP_OK).withBody(createAuthResponse(validityPeriod))));
  }

  private String createAuthResponse(int validityPeriod) {
    var response = JsonConfig.JSON.createObjectNode();
    response.put("access_token", accessToken);
    response.put("expires_in", validityPeriod);
    return toJson(response);
  }

  private static class SimpleDynamoCachedTokenProvider extends DynamoCachedTokenProvider {

    public SimpleDynamoCachedTokenProvider(TokenProvider newTokenProvider,
      DynamoDbClient dynamoClient, String tableName) {
      super(newTokenProvider, dynamoClient, tableName);
    }

    @Override
    protected Optional<OAuthTokenEntry> fromGetResponse(GetItemResponse response) {
      return Optional.of(response).map(GetItemResponse::item)
        .filter(map -> map.containsKey(BODY_FIELD)).map(OAuthTokenEntryConverter::fromItem);
    }

    @Override
    protected Map<String, AttributeValue> createSearchKey() {
      var keyValue = AttributeValue.builder().s(OAuthTokenEntry.TYPE).build();
      return Map.of(PARTITION_KEY, keyValue, SORT_KEY, keyValue);
    }

    @Override
    protected Map<String, AttributeValue> toItem(OAuthTokenEntry token) {
      return OAuthTokenEntryConverter.toItem(token);
    }
  }
}