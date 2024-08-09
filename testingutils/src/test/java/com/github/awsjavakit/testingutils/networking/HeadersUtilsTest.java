package com.github.awsjavakit.testingutils.networking;

import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static com.github.awsjavakit.testingutils.networking.HeadersUtils.ACCEPT_ALL_HEADERS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;

import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HeadersUtilsTest {

  @Test
  void shouldConvertJavaHeadersToWireMockHeaders() {
    var javaHeadersMap = Map.of(randomString(), List.of(randomString()));
    var javaHeaders = java.net.http.HttpHeaders.of(javaHeadersMap, ACCEPT_ALL_HEADERS);
    var wiremockHeaders = HeadersUtils.javaHeadersToWiremockHeaders(javaHeaders);

    assertThat(wiremockHeaders, is(instanceOf(HttpHeaders.class)));
    assertThat(wiremockHeaders.size(), is(equalTo(javaHeadersMap.size())));
    wiremockHeaders.all().forEach(wiremockHeader -> assertThat(wiremockHeader.values(),
      containsInAnyOrder(javaHeadersMap.get(wiremockHeader.key()).toArray())));
  }

  @Test
  void shouldConvertWiremockHeadersToJavaHeaders() {
    var wiremockHeaders = new HttpHeaders(new HttpHeader(randomString(), randomString()));
    var javaHeaders = HeadersUtils.wiremockHeadersToJavaHeaders(wiremockHeaders);

    assertThat(javaHeaders, is(instanceOf(java.net.http.HttpHeaders.class)));
    assertThat(javaHeaders.map().size(), is(equalTo(wiremockHeaders.size())));
    javaHeaders.map().forEach((key, value) -> assertThat(value,
      containsInAnyOrder(wiremockHeaders.getHeader(key).getValues().toArray())));
  }

}