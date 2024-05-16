package com.github.awsjavakit.http;

import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomUri;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static com.gtihub.awsjavakit.attempt.Try.attempt;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.awsjavakit.http.RetryStrategy.DefaultRetryStrategy;
import com.github.awsjavakit.misc.paths.UriWrapper;
import com.github.awsjavakit.testingutils.networking.WiremockHttpClient;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class RetryingHttpClientTest {

  public static final String RANDOMLY_FAILING_ENDPOINT = "/first-fail/then-succeed";

  private WireMockServer server;
  private URI serverUri;
  private String expectedResponseBody;

  public static Stream<Named<ExceptionTestSetup>> failingHttpClient() {
    return Stream.of(new IOException(randomString()), new InterruptedException(randomString()))
      .map(ExceptionTestSetup::new)
      .map(setup -> Named.of(setup.name(), setup));

  }

  @BeforeEach
  public void init() {
    this.server = new WireMockServer(options().httpDisabled(true).dynamicHttpsPort());
    this.server.start();
    this.serverUri = URI.create(server.baseUrl());
    this.expectedResponseBody = randomString();

  }

  @Test
  void shouldRetrySyncRequestsBasedOnTheRetryPolicy() throws IOException, InterruptedException {
    setupServerToFirstFailThenSucceed();

    var client = RetryingHttpClient.create(WiremockHttpClient.create().build(),
      new DefaultRetryStrategy(Duration.ZERO));
    var requestUri = UriWrapper.fromUri(serverUri)
      .addChild(RANDOMLY_FAILING_ENDPOINT).getUri();
    var request = HttpRequest.newBuilder(requestUri).GET().build();
    var response = client.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
    assertThat(response.statusCode()).isEqualTo(HTTP_OK);
    assertThat(response.body()).isEqualTo(expectedResponseBody);
  }

  @Test
  void shouldNotRepeatRequestIfThereIsNoError() throws IOException, InterruptedException {
    setupServerToAlwaysSucceed();

    var client = RetryingHttpClient.create(WiremockHttpClient.create().build(),
      new DefaultRetryStrategy(Duration.ZERO));
    var requestUri = UriWrapper.fromUri(serverUri)
      .addChild(RANDOMLY_FAILING_ENDPOINT).getUri();
    var request = HttpRequest.newBuilder(requestUri).GET().build();
    var response = client.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
    assertThat(response.statusCode()).isEqualTo(HTTP_OK);
    assertThat(response.body()).isEqualTo(expectedResponseBody);
    server.verify(1, getRequestedFor(anyUrl()));
  }

  @Test
  void shouldFailEventually() {
    setupServerToAlwaysFail();
    var client = RetryingHttpClient.create(WiremockHttpClient.create().build(),
      new DefaultRetryStrategy(Duration.ZERO));
    var requestUri = UriWrapper.fromUri(serverUri)
      .addChild(RANDOMLY_FAILING_ENDPOINT).getUri();
    var request = HttpRequest.newBuilder(requestUri).GET().build();
    assertThrows(IOException.class,
      () -> client.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8)));

  }

  @Test
  void canNotRetryAsyncRequestsBasedOnTheRetryPolicy() {
    setupServerToFirstFailThenSucceed();
    var client = RetryingHttpClient.create(
      WiremockHttpClient.create().build(),
      new DefaultRetryStrategy(Duration.ZERO));
    var requestUri = UriWrapper.fromUri(serverUri)
      .addChild(RANDOMLY_FAILING_ENDPOINT).getUri();
    var request = HttpRequest.newBuilder(requestUri).GET().build();

    Executable action = () -> client.sendAsync(request,
        BodyHandlers.ofString(StandardCharsets.UTF_8))
      .get();
    assertThrows(ExecutionException.class, action);
  }

  @ParameterizedTest
  @MethodSource("failingHttpClient")
  void shouldRethrowTheCheckedExceptionsContainedHttpClientHasThrown(ExceptionTestSetup testSetup) {
    var failingClient = testSetup.failingClient();
    var retryClient =
      RetryingHttpClient.create(failingClient,RetryStrategy.defaultStrategy(Duration.ZERO));
    Executable action = () -> retryClient.send(dummyRequest(), BodyHandlers.ofString());
    var exception = assertThrows(testSetup.exception().getClass(), action);
    assertThat(exception.getMessage()).isEqualTo(testSetup.exception().getMessage());

  }

  @Test
  void shouldIntegrateWithExternalResilienceLibraries() throws IOException, InterruptedException {
    setupServerToFirstFailThenSucceed();

    var retryStrategy = new ResilienceRetryStrategy();
    var client = RetryingHttpClient.create(WiremockHttpClient.create().build(), retryStrategy);
    var requestUri = UriWrapper.fromUri(serverUri)
      .addChild(RANDOMLY_FAILING_ENDPOINT).getUri();
    var request = HttpRequest.newBuilder(requestUri).GET().build();
    var response = client.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
    assertThat(response.statusCode()).isEqualTo(HTTP_OK);
    assertThat(response.body()).isEqualTo(expectedResponseBody);
    server.verify(2, getRequestedFor(anyUrl()));
  }

  private HttpRequest dummyRequest() {
    return HttpRequest.newBuilder(randomUri()).GET().build();
  }

  private void setupServerToAlwaysSucceed() {
    this.server.stubFor(get(RANDOMLY_FAILING_ENDPOINT)
      .willReturn(aResponse().withBody(expectedResponseBody)));
  }

  private void setupServerToFirstFailThenSucceed() {
    this.server.stubFor(get(RANDOMLY_FAILING_ENDPOINT)
      .inScenario("FirstRequest")
      .whenScenarioStateIs(STARTED)
      .willReturn(aResponse().withFault(Fault.RANDOM_DATA_THEN_CLOSE))
      .willSetStateTo("Failed"));

    this.server.stubFor(get(RANDOMLY_FAILING_ENDPOINT)
      .inScenario("FirstRequest")
      .whenScenarioStateIs("Failed")
      .willReturn(aResponse().withBody(expectedResponseBody))
      .willSetStateTo(STARTED));

  }

  private void setupServerToAlwaysFail() {
    this.server.stubFor(get(RANDOMLY_FAILING_ENDPOINT)
      .willReturn(aResponse().withFault(Fault.RANDOM_DATA_THEN_CLOSE)));

  }

  public record ExceptionTestSetup(Exception exception) {

    public String name() {
      return exception.getClass().getSimpleName();
    }

    public HttpClient failingClient() {
      var client = mock(HttpClient.class);
      attempt(() -> when(client.send(any(HttpRequest.class), any(BodyHandler.class)))
        .thenThrow(exception)).orElseThrow();
      return client;

    }
  }
}