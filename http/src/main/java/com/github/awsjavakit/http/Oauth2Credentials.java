package com.github.awsjavakit.http;

import static com.gtihub.awsjavakit.attempt.Try.attempt;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;

@JsonTypeInfo(use = Id.NAME, property = "type")
public record Oauth2Credentials(
  @JsonProperty("serverUri") URI serverUri,
  @JsonProperty("clientId") String clientId,
  @JsonProperty("clientSecret") String clientSecret) {

  public static Oauth2Credentials fromJson(String secretName, ObjectMapper json) {
    return attempt(() -> json.readValue(secretName, Oauth2Credentials.class)).orElseThrow();
  }

  public String toJsonString(ObjectMapper objectMapper) {
    return attempt(() -> objectMapper.writeValueAsString(this)).orElseThrow();
  }

}
