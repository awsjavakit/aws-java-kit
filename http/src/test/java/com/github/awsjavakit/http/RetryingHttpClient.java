package com.github.awsjavakit.http;

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
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

public class RetryingHttpClient extends HttpClient {

  private final HttpClient httpClient;
  private final RetryStrategy retryStrategy;

  private RetryingHttpClient(HttpClient httpClient, RetryStrategy retryStrategy) {

    this.httpClient = httpClient;
    this.retryStrategy = retryStrategy;
  }

  public static RetryingHttpClient create(HttpClient httpClient, RetryStrategy retryStrategy) {
    return new RetryingHttpClient(httpClient, retryStrategy);
  }

  @Override
  public Optional<CookieHandler> cookieHandler() {
    return httpClient.cookieHandler();
  }

  @Override
  public Optional<Duration> connectTimeout() {
    return httpClient.connectTimeout();
  }

  @Override
  public Redirect followRedirects() {
    return httpClient.followRedirects();
  }

  @Override
  public Optional<ProxySelector> proxy() {
    return httpClient.proxy();
  }

  @Override
  public SSLContext sslContext() {
    return httpClient.sslContext();
  }

  @Override
  public SSLParameters sslParameters() {
    return httpClient.sslParameters();
  }

  @Override
  public Optional<Authenticator> authenticator() {
    return httpClient.authenticator();
  }

  @Override
  public Version version() {
    return httpClient.version();
  }

  @Override
  public Optional<Executor> executor() {
    return httpClient.executor();
  }

  @Override
  public <T> HttpResponse<T> send(HttpRequest request, BodyHandler<T> responseBodyHandler) {
    return retryStrategy.apply(() -> httpClient.send(request, responseBodyHandler));
  }

  @Override
  public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
    BodyHandler<T> responseBodyHandler) {
    return httpClient.sendAsync(request, responseBodyHandler);
  }

  @Override
  public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
    BodyHandler<T> responseBodyHandler, PushPromiseHandler<T> pushPromiseHandler) {
    return httpClient.sendAsync(request, responseBodyHandler, pushPromiseHandler);
  }
}
