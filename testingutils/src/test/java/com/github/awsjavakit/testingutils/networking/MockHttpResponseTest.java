package com.github.awsjavakit.testingutils.networking;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.http.HttpClient.Version;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class MockHttpResponseTest {

  public static final MockHttpResponse<Object> SAMPLE_RESPONSE = new MockHttpResponse<>(null, null);

  @ParameterizedTest
  @MethodSource("unsupportedProvider")
  void shouldThrowExceptionWhenCallingUnsupportedMethod(Supplier<Object> supplier) {
    var exception = assertThrows(UnsupportedOperationException.class, supplier::get);
    assertThat(exception, is(instanceOf(UnsupportedOperationException.class)));
  }

  @Test
  void shouldReturnVersion2() {
    assertThat(SAMPLE_RESPONSE.version(), is(equalTo(Version.HTTP_2)));
  }

  private static Stream<Supplier<Object>> unsupportedProvider() {
    var mockResponse = SAMPLE_RESPONSE;
    return Stream.of(mockResponse::request,
      mockResponse::previousResponse,
      mockResponse::sslSession,
      mockResponse::uri);
  }

}