package com.github.awsjavakit.apigateway.bodyparsing;

import static com.gtihub.awsjavakit.attempt.Try.attempt;
import static java.util.Objects.nonNull;

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
    return nonNull(body) ? parseNonNullBody(body) : null;
  }

  private I parseNonNullBody(String body) {
    if (expectedInputIsString()){
      return iClass.cast(body);
    }
    return parseJsonString(body);

  }

  private boolean expectedInputIsString() {
    return String.class.equals(iClass);
  }

  private I parseJsonString(String body) {
    return attempt(() -> objectMapper.readValue(body, iClass)).orElseThrow();
  }

}