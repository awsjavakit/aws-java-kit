package com.github.awsjavakit.http.token;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OAuthTokenResponse {

  @JsonProperty("access_token")
  private final String accessToken;
  @JsonProperty("expires_in")
  private final Long validityPeriodInSeconds;
  private final Instant timestamp;

  @JsonCreator
  public OAuthTokenResponse(
    @JsonProperty(value = "access_token", required = true) String accessToken,
    @JsonProperty(value = "expires_in", required = true) long durationInSeconds) {
    timestamp = Instant.now();
    this.accessToken = accessToken;
    this.validityPeriodInSeconds = durationInSeconds;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public Long getValidityPeriodInSeconds() {
    return validityPeriodInSeconds;
  }

  @JsonIgnore
  public Instant getExpirationTimestamp() {
    return getTimestamp().plusSeconds(validityPeriodInSeconds);
  }
}
