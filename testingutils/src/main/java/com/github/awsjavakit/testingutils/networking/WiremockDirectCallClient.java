package com.github.awsjavakit.testingutils.networking;

import static com.gtihub.awsjavakit.attempt.Try.attempt;

import com.github.tomakehurst.wiremock.direct.DirectCallHttpServer;
import com.github.tomakehurst.wiremock.http.ImmutableRequest;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

public class WiremockDirectCallClient extends HttpClient {

  private final DirectCallHttpServer server;

  public WiremockDirectCallClient(DirectCallHttpServer server) {
    this.server = server;
  }

  @Override
  public Optional<CookieHandler> cookieHandler() {
    return Optional.empty();
  }

  @Override
  public Optional<Duration> connectTimeout() {
    return Optional.empty();
  }

  @Override
  public Redirect followRedirects() {
    return null;
  }

  @Override
  public Optional<ProxySelector> proxy() {
    return Optional.empty();
  }

  @Override
  public SSLContext sslContext() {
    return null;
  }

  @Override
  public SSLParameters sslParameters() {
    return null;
  }

  @Override
  public Optional<Authenticator> authenticator() {
    return Optional.empty();
  }

  @Override
  public Version version() {
    return Version.HTTP_2;
  }

  @Override
  public Optional<Executor> executor() {
    return Optional.empty();
  }

  @Override
  public <T> HttpResponse<T> send(HttpRequest request,
    HttpResponse.BodyHandler<T> responseBodyHandler) {
    return attempt(() -> sendAsync(request, responseBodyHandler).get()).orElseThrow();
  }

  @Override
  public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
    HttpResponse.BodyHandler<T> responseBodyHandler) {
    var wireMockRequest = ImmutableRequest.create()
      .withAbsoluteUrl(request.uri().toString())
      .withMethod(RequestMethod.fromString(request.method()))
      .build();
    var wireMockResponse = server.stubRequest(wireMockRequest);
    return CompletableFuture.completedFuture(new MockHttpResponse<>(wireMockResponse,
      request,
      responseBodyHandler));

  }

  @Override
  public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
    HttpResponse.BodyHandler<T> responseBodyHandler,
    HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

}
