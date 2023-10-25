package com.github.awsjavakit.http;

import static com.github.awsjavakit.http.OAuth2HttpClient.AUTHORIZATION_HEADER;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.awsjavakit.http.token.OAuthTokenEntry;
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
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OAuth2HttpClientTest {

  public static final String PROTECTED_ENDPOINT_PATH = "/protected/endpoint";
  public static final Duration SOME_LONG_DURATION = Duration.ofSeconds(600);
  private WireMockServer authServer;
  private URI serverUrl;
  private String authToken;
  private String expectedResponseBody;
  private OAuth2HttpClient client;

  @BeforeEach
  public void init() {
    this.authServer = new WireMockServer(options().httpDisabled(true).dynamicHttpsPort());
    this.authServer.start();
    this.serverUrl = URI.create(authServer.baseUrl());
    this.expectedResponseBody = randomString();
    this.authToken = randomString();
    var tokenProvider = new SimpleTokenProvider(authToken);
    var httpClient = WiremockHttpClient.create().build();
    this.client =
      OAuth2HttpClient.create(httpClient, tokenProvider);
    setupCredentialsResponse();
  }

  @Test
  void shouldFetchBearerTokenFromTokenProviderWhenSendingSyncedRequest()
    throws IOException, InterruptedException {
    var request = HttpRequest.newBuilder(protectedEndpoint()).GET().build();
    HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(HTTP_OK);
  }

  @Test
  void shouldRemoveAnyOtherExistingAuthorizationHeader()
    throws IOException, InterruptedException {
    var request = HttpRequest.newBuilder(protectedEndpoint()).GET()
      .setHeader(AUTHORIZATION_HEADER, randomString())
      .build();
    var response = client.send(request, BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(HTTP_OK);
  }

  @Test
  void shouldFetchBearerTokenFromTokenProviderWhenSendingAsyncRequest()
    throws InterruptedException, ExecutionException {
    var request = HttpRequest.newBuilder(protectedEndpoint()).GET().build();
    var response = client.sendAsync(request, BodyHandlers.ofString()).get();
    assertThat(response.statusCode()).isEqualTo(HTTP_OK);
  }

  @Test
  void shouldFetchBearerTokenFromTokenProviderWhenSendingAsyncRequestWithPushPromiseHandler()
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
    authServer.stubFor(get(urlPathEqualTo(PROTECTED_ENDPOINT_PATH))
      .withHeader(AUTHORIZATION_HEADER, new EqualToPattern("Bearer " + authToken))
      .willReturn(aResponse().withStatus(HTTP_OK).withBody(expectedResponseBody)));

  }

  private record SimpleTokenProvider(String token) implements TokenProvider {

    @Override
    public OAuthTokenEntry fetchToken() {
      var now = Instant.now();
      return new OAuthTokenEntry(token, now,now.plus(SOME_LONG_DURATION));
    }
  }
}