package com.github.awsjavakit.testingutils.aws;

import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;

import static com.gtihub.awsjavakit.attempt.Try.attempt;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;

class FakeLambdaClientTest {

  public static final ObjectMapper JSON = new ObjectMapper();

  @Test
  void shouldInvokeSubmittedFunction() {
    var handler = new FakeLambdaClient<>(FakeLambdaClientTest::someFunction, SampleInput.class,
      JSON);
    var input = new SampleInput(randomString());
    var expectedOutput = someFunction(input);
    var invokeRequest = InvokeRequest.builder()
      .functionName(randomString())
      .payload(SdkBytes.fromUtf8String(toJson(input)))
      .build();

    var response = handler.invoke(invokeRequest).payload().asUtf8String();
    var parsed = fromJson(response, SampleOutput.class);
    assertThat(parsed).isEqualTo(expectedOutput);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"", " "})
  void shouldThrowExceptionWhenLambdaNameIsNotSpecified(String functionName) {
    var handler = new FakeLambdaClient<>(FakeLambdaClientTest::someFunction, SampleInput.class,
      JSON);

    var invokeRequest = InvokeRequest.builder()
      .payload(SdkBytes.fromUtf8String(toJson(new SampleInput(randomString()))))
      .functionName(functionName)
      .build();
    var exception = assertThrows(IllegalArgumentException.class,
      () -> handler.invoke(invokeRequest));
    assertThat(exception.getMessage()).contains("Function name cannot be blank");
  }

  private static SampleOutput someFunction(SampleInput input) {
    return new SampleOutput(input.value().toUpperCase(Locale.ROOT));
  }

  private <I> I fromJson(String json, Class<I> type) {
    return attempt(() -> JSON.readValue(json, type)).orElseThrow();
  }

  private String toJson(SampleInput input) {
    return attempt(() -> JSON.writeValueAsString(input)).orElseThrow();
  }

  private record SampleInput(String value) {

  }

  private record SampleOutput(String value) {

  }
}