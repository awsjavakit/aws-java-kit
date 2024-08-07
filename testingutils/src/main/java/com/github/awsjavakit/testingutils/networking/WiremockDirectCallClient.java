package com.github.awsjavakit.testingutils.networking;

import static com.github.awsjavakit.testingutils.networking.HeadersUtils.javaHeadersToWiremockHeaders;
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
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

public class WiremockDirectCallClient extends HttpClient {

  public static final byte[] NON_NULL_EMPTY_BODY = {};
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
    var body = fetchBodyFromRequest(request);
    var wireMockRequest = ImmutableRequest.create()
      .withAbsoluteUrl(request.uri().toString())
      .withMethod(RequestMethod.fromString(request.method()))
      .withBody(body)
      .withHeaders(javaHeadersToWiremockHeaders(request.headers()))
      .build();
    var wireMockResponse = server.stubRequest(wireMockRequest);
    return CompletableFuture.completedFuture(new MockHttpResponse<>(wireMockResponse,
      request,
      responseBodyHandler));

  }

  private static byte[] fetchBodyFromRequest(HttpRequest request) {
    var bodyPublisher = request.bodyPublisher();
    if(bodyPublisher.isPresent()){
      var subscriber = BodySubscribers.ofByteArray();
      bodyPublisher.orElseThrow().subscribe(new CustomSubscriber(subscriber));
      return attempt(()->subscriber.getBody().toCompletableFuture().get()).orElseThrow();
    }
    return NON_NULL_EMPTY_BODY;

  }

  @Override
  public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
    HttpResponse.BodyHandler<T> responseBodyHandler,
    HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  private record CustomSubscriber(BodySubscriber<byte[]> subscriber) implements
    Flow.Subscriber<ByteBuffer> {

    @Override
      public void onSubscribe(Flow.Subscription subscription) {
        subscriber.onSubscribe(subscription);
      }

      @Override
      public void onError(Throwable throwable) {
        subscriber.onError(throwable);
      }

      @Override
      public void onComplete() {
        subscriber.onComplete();
      }

      @Override
      public void onNext(ByteBuffer item) {
        subscriber.onNext(List.of(item));
      }
    }
}
