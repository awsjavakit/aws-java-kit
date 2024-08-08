package com.github.awsjavakit.testingutils.networking;

import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static com.github.awsjavakit.testingutils.networking.WiremockDirectCallClientTest.uriWithPath;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.direct.DirectCallHttpServer;
import com.github.tomakehurst.wiremock.direct.DirectCallHttpServerFactory;
import com.github.tomakehurst.wiremock.http.ImmutableRequest;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class WiremockBugTest {

  private WireMockServer directCallServer;
  private WireMockServer jettyServer;
  private HttpClient jettyClient;
  private DirectCallHttpServer directCallHttpServer;

  @BeforeEach
  public void init() {
    var factory = new DirectCallHttpServerFactory();

    this.directCallServer = new WireMockServer(options().httpServerFactory(factory));
    directCallServer.start(); // no-op, not required

    this.jettyServer = new WireMockServer(options().dynamicHttpsPort().httpDisabled(true));
    jettyServer.start();

    this.directCallHttpServer = factory.getHttpServer();
    this.jettyClient = WiremockHttpClient.create().build();
  }

  @AfterEach
  public void stop() {
    this.directCallServer.stop();
  }


  @Test
  @Disabled
  void shouldFailWhenSubmittingWrongRequestBodyDirectCallVersion() {
    var uri = uriWithPath("https://localhost");
    var expectedResponseBody = randomString();
    var expectedRequestBody = "ExpectedRequestBody";

    directCallServer.stubFor(WireMock.post(WireMock.urlPathEqualTo(uri.getPath()))
      .withRequestBody(WireMock.equalTo(expectedRequestBody))
      .willReturn(aResponse().withBody(expectedResponseBody).withStatus(HTTP_OK)));

    var wireMockRequest = ImmutableRequest.create()
      .withAbsoluteUrl(uri.toString())
      .withMethod(RequestMethod.POST)
      //.withBody(new byte[]{})
      .build();
    var response = directCallHttpServer.stubRequest(wireMockRequest);

    assertThat(response.getBodyAsString(), response.getStatus(), is(equalTo(HTTP_OK)));
    assertThat(response.getBodyAsString(), is(equalTo(expectedResponseBody)));
  }

  @Test
  @Disabled
  void shouldFailWhenSubmittingWrongRequestBodyJettyVersion()
    throws IOException, InterruptedException {
    var uri = uriWithPath(jettyServer.baseUrl());
    var expectedResponseBody = randomString();
    var expectedRequestBody = "ExpectedRequestBody";

    jettyServer.stubFor(WireMock.post(WireMock.urlPathEqualTo(uri.getPath()))
      .withRequestBody(WireMock.equalTo(expectedRequestBody))
      .willReturn(aResponse().withBody(expectedResponseBody).withStatus(HTTP_OK)));

    var request = HttpRequest.newBuilder(uri).POST(BodyPublishers.noBody()).build();
    var response = jettyClient.send(request, BodyHandlers.ofString());

    assertThat(response.body(), response.statusCode(), is(equalTo(HTTP_OK)));
    assertThat(response.body(), is(equalTo(expectedResponseBody)));
  }

}
