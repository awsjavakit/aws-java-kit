package com.github.awsjavakit.apigateway.bodyparsing;

import static com.gtihub.awsjavakit.attempt.Try.attempt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DefaultJsonParser<I> implements BodyParser<I> {

  private final Class<I> iClass;
  private final ObjectMapper objectMapper;

  public DefaultJsonParser(Class<I> iClass, ObjectMapper objectMapper) {
    this.iClass = iClass;
    this.objectMapper = objectMapper;
  }

  @Override
  public I parseBody(String body) {
    var json = attempt(() -> objectMapper.readTree(body)).orElseThrow();
    if (inputIsStringAndExpectedInputIsString(json)) {
      return inputAsString(json);
    }
    return parseJsonString(body);
  }

  private I parseJsonString(String body) {
    return attempt(() -> objectMapper.readValue(body, iClass)).orElseThrow();
  }

  private I inputAsString(JsonNode json) {
    return iClass.cast(json.textValue());
  }

  private boolean inputIsStringAndExpectedInputIsString(JsonNode json) {
    return iClass.equals(String.class) && json.isTextual();
  }
}