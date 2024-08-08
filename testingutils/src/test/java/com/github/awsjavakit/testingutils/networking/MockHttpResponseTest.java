package com.github.awsjavakit.testingutils.networking;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class MockHttpResponseTest {

  @ParameterizedTest
  @MethodSource("unsupportedProvider")
  void shouldThrowExceptionWhenCallingUnsupportedMethod(Supplier<Object> supplier) {
    var exception = assertThrows(UnsupportedOperationException.class, supplier::get);
    assertThat(exception, is(instanceOf(UnsupportedOperationException.class)));
  }

  private static Stream<Supplier<Object>> unsupportedProvider() {
    var mockResponse = new MockHttpResponse<>(null, null);
    return Stream.of(mockResponse::request,
      mockResponse::previousResponse,
      mockResponse::sslSession,
      mockResponse::uri,
      mockResponse::version);
  }

}