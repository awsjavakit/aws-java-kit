package com.github.awsjavakit.testingutils.networking;

import static com.gtihub.awsjavakit.attempt.Try.attempt;

import com.github.tomakehurst.wiremock.http.MultiValue;
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
import java.util.stream.Collectors;
import javax.net.ssl.SSLSession;

public class MockHttpResponse<T> implements HttpResponse<T> {

  public static final java.util.function.BiPredicate<String, String> ACCEPT_ALL_HEADERS = (headerName, headerValue) -> true;
  private final Response wiremockResponse;
  private final HttpRequest request;
  private final BodyHandler<T> reponseBodyHandler;

  public MockHttpResponse(Response wiremockResponse, HttpRequest request,
    BodyHandler<T> reponseBodyHandler) {

    this.wiremockResponse = wiremockResponse;
    this.request = request;
    this.reponseBodyHandler = reponseBodyHandler;
  }

  public static HttpHeaders parseHeaders(Response response) {
    var headers = response.getHeaders().all()
      .stream()
      .collect(Collectors.toMap(MultiValue::getKey, MultiValue::getValues));
    return HttpHeaders.of(headers, ACCEPT_ALL_HEADERS);
  }

  @Override
  public int statusCode() {
    return wiremockResponse.getStatus();
  }

  @Override
  public HttpRequest request() {
    return request;
  }

  @Override
  public Optional<HttpResponse<T>> previousResponse() {
    return Optional.empty();
  }

  @Override
  public HttpHeaders headers() {
    return parseHeaders(wiremockResponse);
  }

  @Override
  public T body() {
    var publisher = BodyPublishers.ofByteArray(wiremockResponse.getBody());
    var subscriber =
      this.reponseBodyHandler.apply(new CustomResponseInfo(wiremockResponse));
    var wrapper = new CustomerSubscriber(subscriber);
    publisher.subscribe(wrapper);
    return attempt(() -> subscriber.getBody().toCompletableFuture().get()).orElseThrow();
  }

  @Override
  public Optional<SSLSession> sslSession() {
    return Optional.empty();
  }

  @Override
  public URI uri() {
    return request.uri();
  }

  @Override
  public Version version() {
    return Version.HTTP_2;
  }

  private record CustomResponseInfo(Response wiremockResponse) implements ResponseInfo {

    @Override
    public int statusCode() {
      return wiremockResponse.getStatus();
    }

    @Override
    public HttpHeaders headers() {
      return parseHeaders(wiremockResponse);
    }

    @Override
    public Version version() {
      return Version.HTTP_2;
    }
  }

  private class CustomerSubscriber implements Subscriber<ByteBuffer> {

    private final Subscriber<List<ByteBuffer>> subscriber;

    public CustomerSubscriber(BodySubscriber<T> subscriber) {
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
    public void onError(Throwable throwable) {
      subscriber.onError(throwable);
    }

    @Override
    public void onComplete() {
      subscriber.onComplete();
    }
  }
}
