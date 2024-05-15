package com.github.awsjavakit.http;

import com.github.awsjavakit.misc.JacocoGenerated;
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

public final class RetryingHttpClient extends HttpClient {

  private final HttpClient httpClient;
  private final RetryStrategy retryStrategy;

  private RetryingHttpClient(HttpClient httpClient, RetryStrategy retryStrategy) {
    super();
    this.httpClient = httpClient;
    this.retryStrategy = retryStrategy;
  }

  public static RetryingHttpClient create(HttpClient httpClient, RetryStrategy retryStrategy) {
    return new RetryingHttpClient(httpClient, retryStrategy);
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

  @Override
  @JacocoGenerated
  public Version version() {
    return httpClient.version();
  }

  @JacocoGenerated
  @Override
  public Optional<Executor> executor() {
    return httpClient.executor();
  }


  @Override
  public <T> HttpResponse<T> send(HttpRequest request, BodyHandler<T> responseBodyHandler) {
    return retryStrategy.apply(req -> httpClient.send(req, responseBodyHandler),request);
  }

  @Override
  public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
    BodyHandler<T> responseBodyHandler) {
    return httpClient.sendAsync(request, responseBodyHandler);
  }

  @JacocoGenerated
  @Override
  public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
    BodyHandler<T> responseBodyHandler, PushPromiseHandler<T> pushPromiseHandler) {
    return httpClient.sendAsync(request, responseBodyHandler, pushPromiseHandler);
  }
}
