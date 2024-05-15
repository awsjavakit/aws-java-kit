package com.github.awsjavakit.http;

import com.github.awsjavakit.misc.JacocoGenerated;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.PushPromiseHandler;
import java.time.Duration;
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
public class OAuth2HttpClient extends HttpClient implements Tagged {

  public static final String AUTHORIZATION_HEADER = "Authorization";
  private final HttpClient httpClient;
  private final TokenProvider tokenProvider;

  protected OAuth2HttpClient(HttpClient httpClient, TokenProvider tokenProvider) {
    super();
    this.httpClient = httpClient;
    this.tokenProvider = tokenProvider;
  }


  public static OAuth2HttpClient create(HttpClient httpClient, TokenProvider tokenProvider) {
    return new OAuth2HttpClient(httpClient, tokenProvider);
  }

  @Override
  public <T> HttpResponse<T> send(HttpRequest request, BodyHandler<T> responseBodyHandler)
    throws IOException, InterruptedException {
    var authorizedRequest = authorizeRequest(request);

    return  httpClient.send(authorizedRequest, responseBodyHandler);

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

  @Override
  public String getTag() {
    return tokenProvider.getTag();
  }

  private HttpRequest authorizeRequest(HttpRequest request) {
    var bearerToken = tokenProvider.fetchToken().value();
    return HttpRequest.newBuilder(request, filterOutAuthHeader())
      .setHeader(AUTHORIZATION_HEADER, "Bearer " + bearerToken)
      .build();
  }

  private BiPredicate<String, String> filterOutAuthHeader() {
    return (headerName, headerValue) -> !AUTHORIZATION_HEADER.equals(headerName);
  }

}
