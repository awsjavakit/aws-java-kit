package com.github.awsjavakit.testingutils.networking;

import static com.github.awsjavakit.testingutils.networking.HeadersUtils.wiremockHeadersToJavaHeaders;
import static com.gtihub.awsjavakit.attempt.Try.attempt;

import com.github.awsjavakit.misc.JacocoGenerated;
import com.github.tomakehurst.wiremock.http.Response;
import java.net.URI;
import java.net.http.HttpClient.Version;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import javax.net.ssl.SSLSession;

public class MockHttpResponse<T> implements HttpResponse<T> {

  private final Response wiremockResponse;
  private final BodyHandler<T> reponseBodyHandler;

  public MockHttpResponse(Response wiremockResponse, BodyHandler<T> reponseBodyHandler) {
    this.wiremockResponse = wiremockResponse;
    this.reponseBodyHandler = reponseBodyHandler;
  }

  @Override
  public int statusCode() {
    return wiremockResponse.getStatus();
  }

  @Override
  public HttpRequest request() {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public Optional<HttpResponse<T>> previousResponse() {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public HttpHeaders headers() {
    return wiremockHeadersToJavaHeaders(wiremockResponse.getHeaders());
  }

  @Override
  public T body() {
    var publisher = BodyPublishers.ofByteArray(wiremockResponse.getBody());
    var subscriber = this.reponseBodyHandler.apply(CustomResponseInfo.create(wiremockResponse));
    var wrapper = new CustomSubscriber(subscriber);
    publisher.subscribe(wrapper);
    return attempt(() -> subscriber.getBody().toCompletableFuture().get()).orElseThrow();
  }

  @Override
  public Optional<SSLSession> sslSession() {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public URI uri() {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public Version version() {
    throw new UnsupportedOperationException("Not implemented yet");
  }


  private class CustomSubscriber implements Subscriber<ByteBuffer> {

    private final Subscriber<List<ByteBuffer>> subscriber;

    public CustomSubscriber(BodySubscriber<T> subscriber) {
      this.subscriber = subscriber;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
      subscriber.onSubscribe(subscription);
    }

    @Override
    public void onNext(ByteBuffer item) {
      subscriber.onNext(List.of(item));
    }

    @Override
    @JacocoGenerated
    public void onError(Throwable throwable) {
      subscriber.onError(throwable);
    }

    @Override
    public void onComplete() {
      subscriber.onComplete();
    }
  }
}
