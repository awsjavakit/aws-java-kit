package com.github.awsjavakit.http;

import static com.gtihub.awsjavakit.attempt.Try.attempt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.awsjavakit.misc.JacocoGenerated;
import com.github.awsjavakit.misc.paths.UriWrapper;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.PushPromiseHandler;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiPredicate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

/**
 * A wrapper of HttpClient that performs an OAuth2 authentication of "grant_type"
 * "client_credentials" before each query.
 */
public class OAuth2HttpClient extends HttpClient {

  public static final String JWT_TOKEN_FIELD = "access_token";
  public static final String AUTHORIZATION_HEADER = "Authorization";
  public static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";

  private static final ObjectMapper JSON = new ObjectMapper();
  private static final Map<String, String> GRANT_TYPE_CLIENT_CREDENTIALS = Map.of("grant_type",
    "client_credentials");
  private static final String CONTENT_TYPE_HEADER = "Content-Type";

  private final HttpClient httpClient;
  private final OAuthCredentialsProvider credentialsProvider;

  protected OAuth2HttpClient(HttpClient httpClient,
    OAuthCredentialsProvider credentialsProvider) {
    super();
    this.httpClient = httpClient;
    this.credentialsProvider = credentialsProvider;
  }

  public static OAuth2HttpClient create(HttpClient httpClient,
    OAuthCredentialsProvider credentialsProvider) {
    return new OAuth2HttpClient(httpClient, credentialsProvider);

  }

  @Override
  public <T> HttpResponse<T> send(HttpRequest request, BodyHandler<T> responseBodyHandler)
    throws IOException, InterruptedException {
    var authorizedRequest = authorizeRequest(request);
    return httpClient.send(authorizedRequest, responseBodyHandler);
  }

  @Override
  public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
    BodyHandler<T> responseBodyHandler) {
    var authorizedRequest = authorizeRequest(request);
    return httpClient.sendAsync(authorizedRequest, responseBodyHandler);
  }

  @Override
  public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
    BodyHandler<T> responseBodyHandler, PushPromiseHandler<T> pushPromiseHandler) {
    var authorizedRequest = authorizeRequest(request);
    return httpClient.sendAsync(authorizedRequest, responseBodyHandler, pushPromiseHandler);
  }

  @JacocoGenerated
  @Override
  public Optional<CookieHandler> cookieHandler() {
    return httpClient.cookieHandler();
  }

  @JacocoGenerated
  @Override
  public Optional<Duration> connectTimeout() {
    return httpClient.connectTimeout();
  }

  @JacocoGenerated
  @Override
  public Redirect followRedirects() {
    return httpClient.followRedirects();
  }

  @JacocoGenerated
  @Override
  public Optional<ProxySelector> proxy() {
    return httpClient.proxy();
  }

  @JacocoGenerated
  @Override
  public SSLContext sslContext() {
    return httpClient.sslContext();
  }

  @JacocoGenerated
  @Override
  public SSLParameters sslParameters() {
    return httpClient.sslParameters();
  }

  @JacocoGenerated
  @Override
  public Optional<Authenticator> authenticator() {
    return httpClient.authenticator();
  }

  @JacocoGenerated
  @Override
  public Version version() {
    return httpClient.version();
  }

  @JacocoGenerated
  @Override
  public Optional<Executor> executor() {
    return httpClient.executor();
  }

  private HttpRequest authorizeRequest(HttpRequest request) {
    var bearerToken = authenticate();
    return addAuthorizationHeader(request, bearerToken);
  }

  private HttpRequest addAuthorizationHeader(HttpRequest request, String accessToken) {
    return HttpRequest.newBuilder(request, filterOutAuthHeader())
      .setHeader(AUTHORIZATION_HEADER, accessToken)
      .build();
  }

  private BiPredicate<String, String> filterOutAuthHeader() {
    return (headerName, headerValue) -> !AUTHORIZATION_HEADER.equals(headerName);
  }

  private String authenticate() {
    var request = formatRequestForOauth2Token();
    return sendRequestAndExtractToken(request);
  }

  private HttpRequest formatRequestForOauth2Token() {
    return HttpRequest.newBuilder(credentialsProvider.getAuthorizationEndpoint())
      .setHeader(AUTHORIZATION_HEADER, credentialsProvider.getAuthorizationHeader())
      .setHeader(CONTENT_TYPE_HEADER, APPLICATION_X_WWW_FORM_URLENCODED)
      .POST(clientCredentialsAuthType())
      .build();
  }

  private HttpRequest.BodyPublisher clientCredentialsAuthType() {
    var queryParameters = UriWrapper.fromHost("notimportant")
      .addQueryParameters(GRANT_TYPE_CLIENT_CREDENTIALS).getUri().getRawQuery();
    return HttpRequest.BodyPublishers.ofString(queryParameters);
  }

  private String createBearerToken(String accessToken) {
    return "Bearer " + accessToken;
  }

  private String sendRequestAndExtractToken(HttpRequest request) {
    return attempt(
      () -> this.httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8)))
      .map(HttpResponse::body)
      .map(JSON::readTree)
      .map(json -> json.get(JWT_TOKEN_FIELD))
      .map(JsonNode::textValue)
      .map(Objects::toString)
      .map(this::createBearerToken)
      .orElseThrow();
  }

}
