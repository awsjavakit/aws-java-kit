package com.github.awsjavakit.apigateway.bodyparsing;

import com.fasterxml.jackson.databind.ObjectMapper;

public interface BodyParser<I> {

  static <I> BodyParser<I> jsonParser(Class<I> iClass, ObjectMapper objectMapper) {
    return new DefaultJsonParser<>(iClass, objectMapper);
  }

  I parseBody(String serializedBody);

}
