package com.github.awsjavakit.http;

import static com.github.awsjavakit.http.OAuth2HttpClient.AUTHORIZATION_HEADER;
import static com.github.awsjavakit.http.OAuthCredentialsProvider.OAUTH2_TOKEN_PATH;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.gtihub.awsjavakit.attempt.Try.attempt;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.awsjavakit.misc.paths.UriWrapper;
import com.github.awsjavakit.testingutils.networking.WiremockHttpClient;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.PushPromiseHandler;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OAuth2HttpClientTest {

  public static final String PROTECTED_ENDPOINT_PATH = "/protected/endpoint";
  private static final ObjectMapper JSON = new ObjectMapper();
  private WireMockServer authServer;
  private URI serverUrl;
  private String clientId;
  private String clientSecret;
  private String authToken;
  private String expectedResponseBody;
  private OAuth2HttpClient client;

  @BeforeEach
  public void init() {
    this.authServer = new WireMockServer(options().httpDisabled(true).dynamicHttpsPort());
    this.authServer.start();
    this.serverUrl = URI.create(authServer.baseUrl());
    this.expectedResponseBody = randomString();
    this.clientId = randomString();
    this.clientSecret = randomString();
    this.authToken = randomString();

    this.client=
      OAuth2HttpClient.create(WiremockHttpClient.create().build(),
        new CredentialsProvider(serverUrl, clientId, clientSecret));
    setupCredentialsResponse();
  }

  public String toJsonString(Object object) {
    return attempt(() -> JSON.writeValueAsString(object)).orElseThrow();
  }

  @Test
  void shouldFetchBearerTokenFromBearerTokenProviderWhenSendingSyncedRequest()
    throws IOException, InterruptedException {
    var request = HttpRequest.newBuilder(protectedEndpoint()).GET().build();
    HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(HTTP_OK);
  }

  @Test
  void shouldRemoveAnyOtherExistingAuthorizationHeader()
    throws IOException, InterruptedException {
    var request = HttpRequest.newBuilder(protectedEndpoint()).GET()
      .setHeader(AUTHORIZATION_HEADER,randomString())
      .build();
    var response = client.send(request, BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(HTTP_OK);
  }

  @Test
  void shouldFetchBearerTokenFromBearerTokenProviderWhenSendingAsyncRequest()
    throws InterruptedException, ExecutionException {
    var request = HttpRequest.newBuilder(protectedEndpoint()).GET().build();
    var response = client.sendAsync(request, BodyHandlers.ofString()).get();
    assertThat(response.statusCode()).isEqualTo(HTTP_OK);
  }

  @Test
  void shouldFetchBearerTokenFromBearerTokenProviderWhenSendingAsyncRequestWithPushPromiseHandler()
    throws InterruptedException, ExecutionException {
    setupCredentialsResponse();
    var request = HttpRequest.newBuilder(protectedEndpoint()).GET().build();
    var response = client.sendAsync(request,
      BodyHandlers.ofString(),
      dummyPushPromiseHandler()).get();

    assertThat(response.statusCode()).isEqualTo(HTTP_OK);
  }

  private static PushPromiseHandler<String> dummyPushPromiseHandler() {
    Function<HttpRequest, BodyHandler<String>> dummyHandler = httpRequest -> BodyHandlers.ofString();
    ConcurrentMap<HttpRequest, CompletableFuture<HttpResponse<String>>> map = new ConcurrentHashMap<>();
    return PushPromiseHandler.of(dummyHandler, map);
  }

  private URI protectedEndpoint() {
    return UriWrapper.fromUri(serverUrl).addChild(PROTECTED_ENDPOINT_PATH).getUri();
  }

  private void setupCredentialsResponse() {
    authServer.stubFor(post(urlPathEqualTo(OAUTH2_TOKEN_PATH))
      .withBasicAuth(clientId, clientSecret)
      .withFormParam("grant_type", new EqualToPattern("client_credentials"))
      .willReturn(aResponse().withStatus(HTTP_OK).withBody(createOAuthResponse())));

    authServer.stubFor(get(urlPathEqualTo(PROTECTED_ENDPOINT_PATH))
      .withHeader(AUTHORIZATION_HEADER, new EqualToPattern("Bearer " + authToken))
      .willReturn(aResponse().withStatus(HTTP_OK).withBody(expectedResponseBody)));

  }

  private String createOAuthResponse() {
    var response = new OAuthResponse(authToken, randomString(), randomString());
    return toJsonString(response);
  }

  private record CredentialsProvider(URI serverUri, String clientId, String clientSecret) implements
    OAuthCredentialsProvider {

    @Override
    public String getClientId() {
      return clientId;
    }

    @Override
    public String getClientSecret() {
      return clientSecret;
    }

    @Override
    public URI getAuthServerUri() {
      return serverUri;
    }

  }
}