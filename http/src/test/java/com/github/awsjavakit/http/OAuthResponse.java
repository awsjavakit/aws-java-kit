package com.github.awsjavakit.http;

import static com.gtihub.awsjavakit.attempt.Try.attempt;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public record OAuthResponse(
  @JsonProperty("access_token") String accessToken,
  @JsonProperty("expires_in") String expiresIn,
  @JsonProperty("token_type") String tokenType) {

  public String toJsonString(ObjectMapper json) {
    return attempt(()->json.writeValueAsString(this)).orElseThrow();
  }
}
