package com.github.awsjavakit.http.token;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)

public class OAuthTokenResponse {

  @JsonProperty("access_token")
  private String accessToken;
  @JsonProperty("expires_in")
  private Long validityPeriodInSeconds;
  private Instant timestamp;

  public OAuthTokenResponse() {
    timestamp = Instant.now();
  }

  public OAuthTokenResponse(String accessToken, long durationInSeconds) {
    this();
    this.accessToken = accessToken;
    this.validityPeriodInSeconds = durationInSeconds;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public Long getValidityPeriodInSeconds() {
    return validityPeriodInSeconds;
  }

  public void setValidityPeriodInSeconds(Long validityPeriodInSeconds) {
    this.validityPeriodInSeconds = validityPeriodInSeconds;
  }

  @JsonIgnore
  public Instant getExpirationTimestamp() {
    return getTimestamp().plusSeconds(validityPeriodInSeconds);
  }
}
