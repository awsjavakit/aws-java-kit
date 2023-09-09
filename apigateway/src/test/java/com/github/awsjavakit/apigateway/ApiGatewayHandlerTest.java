package com.github.awsjavakit.apigateway;

import static java.util.Calendar.YEAR;
import static org.assertj.core.api.Assertions.assertThat;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.awsjavakit.testingutils.ApiGatewayRequestBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
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
  private static final Map<String, String> CONTENT_TYPE_APPLICATION_JSON = Map.of("Content-Type", "application/json");
  private ObjectMapper objectMapper;
  private ByteArrayOutputStream outputStream;

  public static String randomString() {
    return RandomStringUtils.randomAlphanumeric(MIN_RANDOM_STRING_LENGTH, MAX_RANDOM_STRING_LENGTH);
  }

  @BeforeEach
  public void init() {
    this.outputStream = new ByteArrayOutputStream();
    this.objectMapper = new ObjectMapper();
  }

  @Test
  void shouldParseRequestBody() throws IOException {
    var handler = new DemoHandler(objectMapper);
    var sampleInput = new SampleInput(randomString(), 1900 + randomInteger());
    handler.handleRequest(createRequest(sampleInput), outputStream, EMPTY_CONTEXT);
    var actualResponseBody = parseResponse();
    var expectedResponseBody = new SampleOutput(sampleInput.name(),
      calculateAge(sampleInput.birthYear()));
    assertThat(actualResponseBody).isEqualTo(expectedResponseBody);
  }

  private InputStream createRequest(SampleInput sampleInput) {
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
    public SampleOutput processInput(SampleInput input, ApiGatewayEvent apiGatewayEvent, Context context) {
      var currentYear = Calendar.getInstance().get(YEAR);
      return new SampleOutput(input.name(), currentYear - input.birthYear());
    }

    @Override
    protected Map<String, String> getSuccessHeaders() {
      return CONTENT_TYPE_APPLICATION_JSON;
    }

    @Override
    protected int getSuccessStatusCode() {
      return HttpURLConnection.HTTP_OK;
    }
  }
}
