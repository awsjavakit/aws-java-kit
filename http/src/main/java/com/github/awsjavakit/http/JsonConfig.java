package com.github.awsjavakit.http;

import static com.github.awsjavakit.attempt.Try.attempt;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

final class JsonConfig {

  static final ObjectMapper JSON = JsonMapper.builder()
    .build();

  private JsonConfig() {

  }

  static String toJson(Object object) {
    return attempt(() -> JSON.writeValueAsString(object)).orElseThrow();
  }

  static <T> T fromJson(String json, Class<T> objectType) {
    return attempt(() -> JSON.readValue(json, objectType)).orElseThrow();
  }

}
