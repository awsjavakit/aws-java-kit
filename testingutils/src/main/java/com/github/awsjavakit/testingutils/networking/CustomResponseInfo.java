package com.github.awsjavakit.testingutils.networking;

import static com.github.awsjavakit.testingutils.networking.MockHttpResponse.parseHeaders;

import com.github.tomakehurst.wiremock.http.Response;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;

public final class CustomResponseInfo implements HttpResponse.ResponseInfo {


  private final int status;
  private final HttpHeaders httpHeaders;

  public CustomResponseInfo(int status, HttpHeaders httpHeaders) {

    this.status = status;
    this.httpHeaders = httpHeaders;
  }

  public static CustomResponseInfo create(Response wiremockResponse) {
    return new CustomResponseInfo(wiremockResponse.getStatus(), parseHeaders(wiremockResponse));
  }

  @Override
  public int statusCode() {
    return status;
  }

  @Override
  public HttpHeaders headers() {
    return httpHeaders;
  }

  @Override
  public HttpClient.Version version() {
    return HttpClient.Version.HTTP_2;
  }

}
