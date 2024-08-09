package com.github.awsjavakit.testingutils.networking;

import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.MultiValue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class HeadersUtils {

  public static final java.util.function.BiPredicate<String, String> ACCEPT_ALL_HEADERS =
    (headerName, headerValue) -> true;

  private HeadersUtils() {
    // NO-OP
  }

  public static java.net.http.HttpHeaders wiremockHeadersToJavaHeaders(
    HttpHeaders wiremockHeaders) {
    var headers = Optional.ofNullable(wiremockHeaders)
      .stream()
      .map(HttpHeaders::all)
      .flatMap(Collection::stream)
      .collect(Collectors.toMap(MultiValue::getKey, MultiValue::getValues));
    return java.net.http.HttpHeaders.of(headers, ACCEPT_ALL_HEADERS);
  }

  public static HttpHeaders javaHeadersToWiremockHeaders(java.net.http.HttpHeaders javaHeaders) {
    var wiremockHeadersList = new ArrayList<HttpHeader>();
    Optional.ofNullable(javaHeaders).stream().map(java.net.http.HttpHeaders::map)
      .map(Map::entrySet)
      .flatMap(Collection::stream)
      .forEach(entry -> wiremockHeadersList.add(new HttpHeader(entry.getKey(), entry.getValue())));
    return new HttpHeaders(wiremockHeadersList);
  }
}
