package com.github.awsjavakit.testingutils.networking;

import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.MultiValue;
import java.util.ArrayList;
import java.util.stream.Collectors;

public final class HeadersUtils {

  public static final java.util.function.BiPredicate<String, String> ACCEPT_ALL_HEADERS =
    (headerName, headerValue) -> true;

  private HeadersUtils() {
    // No-op; won't be called
  }

  public static java.net.http.HttpHeaders wiremockHeadersToJavaHeaders(
    HttpHeaders wiremockHeaders) {
    var headers = wiremockHeaders.all().stream()
      .collect(Collectors.toMap(MultiValue::getKey, MultiValue::getValues));
    return java.net.http.HttpHeaders.of(headers, ACCEPT_ALL_HEADERS);
  }

  public static HttpHeaders javaHeadersToWiremockHeaders(java.net.http.HttpHeaders javaHeaders) {
    var wiremockHeadersList = new ArrayList<HttpHeader>();
    javaHeaders.map().forEach((key, value) -> wiremockHeadersList.add(new HttpHeader(key, value)));
    return new HttpHeaders(wiremockHeadersList);
  }
}
