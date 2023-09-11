package com.github.awsjavakit.apigateway;

import static java.util.Calendar.YEAR;
import static org.assertj.core.api.Assertions.assertThat;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.awsjavakit.testingutils.ApiGatewayRequestBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Map;
import java.util.Random;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ApiGatewayHandlerTest {

  public static final Context EMPTY_CONTEXT = null;
  public static final int ARBITRARY_CEILING = 120;
  private static final int MIN_RANDOM_STRING_LENGTH = 10;
  private static final int MAX_RANDOM_STRING_LENGTH = 20;
  private static final Map<String, String> CONTENT_TYPE_APPLICATION_JSON = Map.of("Content-Type",
    "application/json");
  private ObjectMapper objectMapper;
  private ByteArrayOutputStream outputStream;
  private ApiGatewayHandler<?, ?> handler;

  public static String randomString() {
    return RandomStringUtils.randomAlphanumeric(MIN_RANDOM_STRING_LENGTH, MAX_RANDOM_STRING_LENGTH);
  }

  @BeforeEach
  public void init() {
    this.outputStream = new ByteArrayOutputStream();
    this.objectMapper = new ObjectMapper();
    this.handler = new DemoHandler(objectMapper);
  }

  @Test
  void shouldParseRequestBody() throws IOException {
    var sampleInput = createSampleInput();
    handler.handleRequest(createRequest(sampleInput), outputStream, EMPTY_CONTEXT);
    var actualResponseBody = parseResponse();
    var expectedResponseBody = new SampleOutput(sampleInput.name(),
      calculateAge(sampleInput.birthYear()));
    assertThat(actualResponseBody).isEqualTo(expectedResponseBody);
  }

  @Test
  void shouldReturnSpecifiedHeaderWhenSuccessful() throws IOException {
    var sampleInput = createSampleInput();
    handler.handleRequest(createRequest(sampleInput), outputStream, EMPTY_CONTEXT);
    var actualResponse =
      GatewayResponse.fromOutputStream(outputStream, objectMapper);
    assertThat(actualResponse.getHeaders()).isEqualTo(CONTENT_TYPE_APPLICATION_JSON);
  }

  @Test
  void shouldReturnBodyAsIsWhenInputTypeIsString() throws IOException {
    var sampleInput = randomString();
    this.handler = new EchoHandler(objectMapper, InputObserver.noOp());
    handler.handleRequest(createRequest(sampleInput), outputStream, EMPTY_CONTEXT);
    var response = GatewayResponse.fromOutputStream(outputStream, objectMapper);
    var responseBody = response.getBody(objectMapper, String.class);
    assertThat(responseBody).isEqualTo(sampleInput);
  }

  @Test
  void shouldReturnStatusCodeSpecifiedByApiGatewayException() throws IOException {
    var sampleInput = randomString();
    var expectedException = new NotFoundException();
    this.handler =
      new EchoHandler(objectMapper, InputObserver.throwException(expectedException));
    handler.handleRequest(createRequest(sampleInput), outputStream, EMPTY_CONTEXT);
    var response = GatewayResponse.fromOutputStream(outputStream, objectMapper);
    assertThat(response.getStatusCode()).isEqualTo(expectedException.statusCode());
  }

  private SampleInput createSampleInput() {
    return new SampleInput(randomString(), 1900 + randomInteger());
  }

  private <I> InputStream createRequest(I sampleInput) {
    return ApiGatewayRequestBuilder
      .create(objectMapper)
      .withBody(sampleInput)
      .build();
  }

  private SampleOutput parseResponse() {
    var response = GatewayResponse.fromOutputStream(outputStream, objectMapper);
    return response.getBody(objectMapper, SampleOutput.class);
  }

  private int calculateAge(int birthYear) {
    return Calendar.getInstance().get(YEAR) - birthYear;
  }

  private int randomInteger() {
    return new Random().nextInt(ARBITRARY_CEILING);
  }

  private static class DemoHandler extends ApiGatewayHandler<SampleInput, SampleOutput> {

    public DemoHandler(ObjectMapper objectMapper) {
      super(SampleInput.class, objectMapper);
    }

    @Override
    public SampleOutput processInput(SampleInput input, ApiGatewayEvent apiGatewayEvent,
      Context context) {
      var currentYear = Calendar.getInstance().get(YEAR);
      return new SampleOutput(input.name(), currentYear - input.birthYear());
    }
  }
}
