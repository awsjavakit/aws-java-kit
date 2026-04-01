package com.github.awsjavakit.jsonconfig;

import static com.github.awsjavakit.attempt.Try.attempt;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.json.JsonMapper.Builder;

public final class JsonConfig {

  public static final ObjectMapper JSON =
    defaultBuilder().enable(SerializationFeature.INDENT_OUTPUT).build();

  private JsonConfig() {

  }

  public static <T> T readValue(String json, Class<T> tClass) {
    return attempt(() -> JSON.readValue(json, tClass)).orElseThrow();
  }

  public static String writeValueAsString(Object object) {
    return attempt(() -> JSON.writeValueAsString(object)).orElseThrow();
  }

  private static Builder defaultBuilder() {
    return JsonMapper.builder()
      .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
      .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
      .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
      .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
      .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
      .configure(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS, false)
      .changeDefaultPropertyInclusion(
        v -> JsonInclude.Value.construct(Include.NON_ABSENT, Include.ALWAYS));
  }
}
