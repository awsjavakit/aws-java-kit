package com.github.awsjavakit.testingutils.networking;

import static com.gtihub.awsjavakit.attempt.Try.attempt;

import com.github.tomakehurst.wiremock.direct.DirectCallHttpServer;
import com.github.tomakehurst.wiremock.http.ImmutableRequest;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiPredicate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import com.github.tomakehurst.wiremock.http.Request;
import javax.net.ssl.SSLSession;

public class WiremockDirectCallClient extends HttpClient {

  private DirectCallHttpServer server;

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
    return null;
  }

  @Override
  public Optional<Executor> executor() {
    return Optional.empty();
  }

  @Override
  public <T> HttpResponse<T> send(HttpRequest request,
                                  HttpResponse.BodyHandler<T> responseBodyHandler)
    throws IOException, InterruptedException {
    var wireMockRequest = ImmutableRequest.create()
      .withAbsoluteUrl(request.uri().toString())
      .withMethod(RequestMethod.fromString(request.method()))
      .build();
    var wireMockResponse = server.stubRequest(wireMockRequest);

    return new HttpResponse<T>() {
      @Override
      public int statusCode() {
        return wireMockResponse.getStatus();
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
        BiPredicate<String, String> filter = (s, s2) -> true;
        return HttpHeaders.of(Map.of("Content-Type", List.of("application/json")), filter);
      }

      @Override
      public T body() {
       var responseBody = responseBodyHandler.apply(new ResponseInfo() {
          @Override
          public int statusCode() {
            return wireMockResponse.getStatus();
          }

          @Override
          public HttpHeaders headers() {
            var filter = new BiPredicate<String, String>() {
              @Override
              public boolean test(String s, String s2) {
                return true;
              }
            };
            return HttpHeaders.of(Map.of("Content-Type", List.of("application/json")), filter);
          }

          @Override
          public Version version() {
            return Version.HTTP_2;
          }
        });
        return attempt(()->responseBody.getBody().toCompletableFuture().get())
          .orElseThrow();
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
    };
  }

  @Override
  public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                                                          HttpResponse.BodyHandler<T> responseBodyHandler) {
    return null;
  }

  @Override
  public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                                                          HttpResponse.BodyHandler<T> responseBodyHandler,
                                                          HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
    return null;
  }
}
