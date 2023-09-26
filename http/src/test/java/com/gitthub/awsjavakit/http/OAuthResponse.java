package com.gitthub.awsjavakit.http;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OAuthResponse(
  @JsonProperty("access_token") String accessToken,
  @JsonProperty("expires_in") String expiresIn,
  @JsonProperty("token_type") String tokenType) {

}
