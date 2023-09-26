package com.github.awsjavakit.jsonconfig;

import static com.gtihub.awsjavakit.attempt.Try.attempt;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class JsonConfig {

  public static final ObjectMapper JSON =
    JsonMapper.builder()
      .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
      .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
      .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
      .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
      .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
      .configure(Feature.IGNORE_UNDEFINED, true)
      .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
      .serializationInclusion(Include.NON_ABSENT)
      .addModule(new JavaTimeModule())
      .addModule(new Jdk8Module())
      .build();
  private static final ObjectMapper SINGLE_LINE_JSON =
    JSON.configure(SerializationFeature.INDENT_OUTPUT, false);

  private JsonConfig() {

  }

  public static String writeValueAsOneLine(Object object) {
    return attempt(() -> SINGLE_LINE_JSON.writeValueAsString(object)).orElseThrow();
  }

  public static <T> T readValue(String json, Class<T> tClass) {
    return attempt(() -> JSON.readValue(json, tClass)).orElseThrow();
  }

  public static String writeValueAsString(Object object) {
    return attempt(() -> JSON.writeValueAsString(object)).orElseThrow();
  }
}
