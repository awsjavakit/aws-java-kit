package com.github.awsjavakit.http;

import static com.gtihub.awsjavakit.attempt.Try.attempt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

final class JsonConfig {

  private static final ObjectMapper JSON = JsonMapper.builder()
    .addModule(new Jdk8Module())
    .addModule(new JavaTimeModule())
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
