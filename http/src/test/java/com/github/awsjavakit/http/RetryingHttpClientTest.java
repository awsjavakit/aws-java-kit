package com.github.awsjavakit.http;

import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.awsjavakit.http.RetryStrategy.DefaultRetryStrategy;
import com.github.awsjavakit.misc.paths.UriWrapper;
import com.github.awsjavakit.testingutils.networking.WiremockHttpClient;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class RetryingHttpClientTest {

  public static final String RANDOMLY_FAILING_ENDPOINT = "/first-fail/then-succeed";

  private WireMockServer server;
  private URI serverUri;
  private String expectedResponseBody;

  @BeforeEach
  public void init() {
    this.server = new WireMockServer(options().httpDisabled(true).dynamicHttpsPort());
    this.server.start();
    this.serverUri = URI.create(server.baseUrl());
    this.expectedResponseBody = randomString();

  }

  @Test
  void shouldRetrySyncRequestsBasedOnTheRetryPolicy() {
    setupServerToFirstFailThenSucceed();

    var client =  RetryingHttpClient.create(WiremockHttpClient.create().build(),
      new DefaultRetryStrategy(1));
    var requestUri = UriWrapper.fromUri(serverUri)
      .addChild(RANDOMLY_FAILING_ENDPOINT).getUri();
    var request = HttpRequest.newBuilder(requestUri).GET().build();
    var response = client.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
    assertThat(response.statusCode()).isEqualTo(HTTP_OK);
    assertThat(response.body()).isEqualTo(expectedResponseBody);

  }

  @Test
  void canNotRetryAsyncRequestsBasedOnTheRetryPolicy() {
    setupServerToFirstFailThenSucceed();
    var client =  RetryingHttpClient.create(
      WiremockHttpClient.create().build(),
      new DefaultRetryStrategy(1));
    var requestUri = UriWrapper.fromUri(serverUri)
      .addChild(RANDOMLY_FAILING_ENDPOINT).getUri();
    var request = HttpRequest.newBuilder(requestUri).GET().build();

    Executable action = () -> client.sendAsync(request,
        BodyHandlers.ofString(StandardCharsets.UTF_8))
      .get();
    assertThrows(ExecutionException.class, action);
  }

  private void setupServerToFirstFailThenSucceed() {
    this.server.stubFor(get(RANDOMLY_FAILING_ENDPOINT)
      .inScenario("FirstFailThenSucceed")
      .whenScenarioStateIs(STARTED)
      .willReturn(aResponse().withFault(Fault.RANDOM_DATA_THEN_CLOSE))
      .willSetStateTo("Failed"));

    this.server.stubFor(get(RANDOMLY_FAILING_ENDPOINT)
      .inScenario("FirstFailThenSucceed")
      .whenScenarioStateIs("Failed")
      .willReturn(aResponse().withBody(expectedResponseBody))
      .willSetStateTo(STARTED));
  }

}