package com.github.awsjavakit.testingutils.networking;

import static com.github.awsjavakit.testingutils.networking.HeadersUtils.wiremockHeadersToJavaHeaders;

import com.github.awsjavakit.misc.JacocoGenerated;
import com.github.tomakehurst.wiremock.http.Response;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;

@JacocoGenerated
public record CustomResponseInfo(int status, HttpHeaders httpHeaders) implements HttpResponse.ResponseInfo {

  public static CustomResponseInfo create(Response wiremockResponse) {
    return new CustomResponseInfo(wiremockResponse.getStatus(),
      wiremockHeadersToJavaHeaders(wiremockResponse.getHeaders()));
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
