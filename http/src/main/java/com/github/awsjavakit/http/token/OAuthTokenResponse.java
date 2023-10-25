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
  private Long durationInSeconds;
  private Instant timestamp;

  public OAuthTokenResponse() {
    timestamp = Instant.now();
  }

  public OAuthTokenResponse(String accessToken, long durationInSeconds) {
    this();
    this.accessToken = accessToken;
    this.durationInSeconds = durationInSeconds;
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

  public Long getDurationInSeconds() {
    return durationInSeconds;
  }

  public void setDurationInSeconds(Long durationInSeconds) {
    this.durationInSeconds = durationInSeconds;
  }

  @JsonIgnore
  public Instant getExpirationTimestamp() {
    return getTimestamp().plusSeconds(durationInSeconds);
  }
}
