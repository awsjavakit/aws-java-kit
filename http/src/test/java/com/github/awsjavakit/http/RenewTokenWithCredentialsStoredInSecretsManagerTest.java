package com.github.awsjavakit.http;

import static com.github.awsjavakit.http.JsonConfig.toJson;
import static com.github.awsjavakit.http.OAuth2HttpClient.AUTHORIZATION_HEADER;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomInteger;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.awsjavakit.http.token.OAuthTokenResponse;
import com.github.awsjavakit.misc.paths.UriWrapper;
import com.github.awsjavakit.testingutils.aws.FakeSecretsManagerClient;
import com.github.awsjavakit.testingutils.networking.WiremockHttpClient;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RenewTokenWithCredentialsStoredInSecretsManagerTest {

  public static final ObjectMapper JSON = new ObjectMapper();
  public static final String SECURED_ENDPOINT_PATH = "/secured/endpoint";
  public static final String SECRET_NAME = "oauthCredentials";
  private static final String OAUTH2_TOKEN_PATH = "/oauth2/token";
  private WireMockServer server;
  private URI serverUri;
  private String accessToken;
  private String clientId;
  private String clientSecret;
  private FakeSecretsManagerClient secretsClient;
  private String expectedResponseBody;
  private OAuth2HttpClient authorizedClient;

  @BeforeEach
  public void init() {
    this.server = new WireMockServer(options().httpDisabled(true).dynamicHttpsPort());
    server.start();
    this.serverUri = URI.create(server.baseUrl());
    this.accessToken = randomString();
    this.clientId = randomString();
    this.clientSecret = randomString();
    this.secretsClient = new FakeSecretsManagerClient(JSON);
    this.expectedResponseBody = randomString();
    persistSecretsInSecretsManager();
    setupAuthAndServiceServer();
    var httpClient = WiremockHttpClient.create().build();
    var tokenProvider = TokenProvider.defaultProvider(httpClient,
      new OAuth2CredentialsFromSecretsManager(secretsClient, SECRET_NAME, JSON));
    this.authorizedClient =
       OAuth2HttpClient.create(WiremockHttpClient.create().build(), tokenProvider);
  }

  @Test
  void shouldSendAuthorizedRequestWhenFetchingOAuthCredentialsFromSecretsManager()
    throws IOException, InterruptedException {
    var requestUri = UriWrapper.fromUri(serverUri).addChild(SECURED_ENDPOINT_PATH).getUri();
    var request = HttpRequest.newBuilder(requestUri).GET().build();
    var response = authorizedClient.send(request, BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(HTTP_OK);
    assertThat(response.body()).isEqualTo(expectedResponseBody);
  }

  private void persistSecretsInSecretsManager() {
    var authEndpoint = UriWrapper.fromUri(serverUri).addChild(OAUTH2_TOKEN_PATH).getUri();
    var oauthCredentials = new Oauth2Credentials(authEndpoint, clientId, clientSecret,
      randomString());
    secretsClient.putPlainTextSecret(SECRET_NAME, oauthCredentials.toJsonString(JSON));
  }

  private void setupAuthAndServiceServer() {
    var response = new OAuthTokenResponse(accessToken, randomInteger());
    server.stubFor(post(urlPathEqualTo(OAUTH2_TOKEN_PATH))
      .withBasicAuth(clientId, clientSecret)
      .withFormParam("grant_type", new EqualToPattern("client_credentials"))
      .willReturn(aResponse().withStatus(HTTP_OK).withBody(toJson(response))));

    server.stubFor(WireMock.get(urlPathEqualTo(SECURED_ENDPOINT_PATH))
      .withHeader(AUTHORIZATION_HEADER, new EqualToPattern("Bearer " + accessToken))
      .willReturn(aResponse().withStatus(HTTP_OK).withBody(expectedResponseBody)));
  }
}