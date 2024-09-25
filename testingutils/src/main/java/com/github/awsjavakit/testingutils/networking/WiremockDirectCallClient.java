package com.github.awsjavakit.testingutils.networking;

import static com.github.awsjavakit.testingutils.networking.HeadersUtils.javaHeadersToWiremockHeaders;
import static com.gtihub.awsjavakit.attempt.Try.attempt;
import com.github.awsjavakit.misc.JacocoGenerated;
import com.github.tomakehurst.wiremock.direct.DirectCallHttpServer;
import com.github.tomakehurst.wiremock.http.FormParameter;
import com.github.tomakehurst.wiremock.http.ImmutableRequest;
import com.github.tomakehurst.wiremock.http.Request;
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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

public class WiremockDirectCallClient extends HttpClient {

  public static final byte[] NON_NULL_EMPTY_BODY = {};
  public static final String CONTENT_TYPE_HEADER = "Content-Type";
  public static final String WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
  public static final String FORM_PARAMETER_DELIMITER = "&";
  public static final String FORM_KEY_VALUE_DELIMITER = "=";
  private final DirectCallHttpServer server;

  protected WiremockDirectCallClient(DirectCallHttpServer server) {
    super();
    this.server = server;
  }

  public static WiremockDirectCallClient create(DirectCallHttpServer server){
    return new WiremockDirectCallClient(server);
  }

  @Override
  public Optional<CookieHandler> cookieHandler() {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public Optional<Duration> connectTimeout() {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public Redirect followRedirects() {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public Optional<ProxySelector> proxy() {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public SSLContext sslContext() {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public SSLParameters sslParameters() {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public Optional<Authenticator> authenticator() {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public Version version() {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public Optional<Executor> executor() {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public <T> HttpResponse<T> send(HttpRequest request,
                                  HttpResponse.BodyHandler<T> responseBodyHandler) {
    return attempt(() -> sendAsync(request, responseBodyHandler).get()).orElseThrow();
  }

  @Override
  public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                                                          HttpResponse.BodyHandler<T> responseBodyHandler) {

    var wireMockRequest = addBody(request);

    var wireMockResponse = server.stubRequest(wireMockRequest);
    return CompletableFuture.completedFuture(
      new MockHttpResponse<>(wireMockResponse, responseBodyHandler));
  }

  @Override
  public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                                                          HttpResponse.BodyHandler<T> responseBodyHandler,
                                                          HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  private static ImmutableRequest.Builder requestWithPendingBody(HttpRequest request) {
    return ImmutableRequest.create()
      .withAbsoluteUrl(request.uri().toString())
      .withMethod(RequestMethod.fromString(request.method()))
      .withHeaders(javaHeadersToWiremockHeaders(request.headers()));
  }

  private static boolean requestContainsForm(HttpRequest request) {
    return request.headers().allValues(CONTENT_TYPE_HEADER).contains(WWW_FORM_URLENCODED);
  }

  private static byte[] extractBodyFromRequest(HttpRequest request) {
    var bodyPublisher = request.bodyPublisher();
    if (bodyPublisher.isPresent()) {
      var subscriber = BodySubscribers.ofByteArray();
      bodyPublisher.orElseThrow().subscribe(new CustomSubscriber(subscriber));
      return attempt(() -> subscriber.getBody().toCompletableFuture().get()).orElseThrow();
    }
    return NON_NULL_EMPTY_BODY;
  }

  private static ImmutableRequest buildRequestWithStringBody(HttpRequest request) {
    var body = extractBodyFromRequest(request);
    return requestWithPendingBody(request).withBody(body).build();
  }

  private Request addBody(HttpRequest request) {
    return requestContainsForm(request)
      ? buildRequestWithForm(request)
      : buildRequestWithStringBody(request);

  }

  private CustomRequest buildRequestWithForm(HttpRequest request) {
    var body = extractBodyFromRequest(request);
    var formParameters = extractFormParameters(new String(body));
    return new CustomRequest(requestWithPendingBody(request).build(), formParameters);
  }

  private Map<String, FormParameter> extractFormParameters(String body) {
    var parameterPairs = body.split(FORM_PARAMETER_DELIMITER);
    var parameters = new ConcurrentHashMap<String, FormParameter>();
    for (var parameterPair : parameterPairs) {
      var split = parameterPair.split(FORM_KEY_VALUE_DELIMITER);
      var key = split[0];
      var value = split[1];
      var parameter = new FormParameter(key, List.of(value));
      parameters.put(key, parameter);
    }
    return parameters;
  }

  @JacocoGenerated
  private record CustomSubscriber(BodySubscriber<byte[]> subscriber) implements
    Flow.Subscriber<ByteBuffer> {

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
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
