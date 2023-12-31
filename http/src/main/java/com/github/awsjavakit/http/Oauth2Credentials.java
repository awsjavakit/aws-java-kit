package com.github.awsjavakit.http;

import static com.gtihub.awsjavakit.attempt.Try.attempt;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;

/**
 * Structure containing all the necessary information for an OAuth2 authentication handshake with
 * for grant_type "client_credentials".
 *
 * @param authEndpointUri    the URI of the authentication endpoint (e.g. "https://auth.example.com/oauth2/token")
 * @param clientId     the client id (username)
 * @param clientSecret the client secret (password)
 */
@JsonTypeInfo(use = Id.NAME, property = "type")
public record Oauth2Credentials(
  @JsonAlias("serverUri") @JsonProperty("authEndpointUri")  URI authEndpointUri,
  @JsonProperty("clientId") String clientId,
  @JsonProperty("clientSecret") String clientSecret) {

  public static Oauth2Credentials fromJson(String secretName, ObjectMapper json) {
    return attempt(() -> json.readValue(secretName, Oauth2Credentials.class)).orElseThrow();
  }

  public String toJsonString(ObjectMapper objectMapper) {
    return attempt(() -> objectMapper.writeValueAsString(this)).orElseThrow();
  }

}
