package com.github.awsjavakit.http;

import static com.gtihub.awsjavakit.attempt.Try.attempt;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;

/**
 * Structure containing all the necessary information for an OAuth2 authentication handshake with
 * for grant_type "client_credentials".
 *
 * @param authEndpointUri the URI of the authentication endpoint (e.g.
 *                        "https://auth.example.com/oauth2/token")
 * @param clientId        the client id (username)
 * @param clientSecret    the client secret (password)
 */
@JsonTypeInfo(use = Id.NAME, include = As.EXISTING_PROPERTY, property = "type")
@JsonTypeName(Oauth2Credentials.TYPE)
public record Oauth2Credentials(
  @JsonAlias("serverUri") @JsonProperty("authEndpointUri") URI authEndpointUri,
  @JsonProperty("clientId") String clientId,
  @JsonProperty("clientSecret") String clientSecret,
  @JsonProperty("tag") String tag)
  implements OAuthCredentialsProvider {

  public static final String TYPE = "OAuth2Credentials";

  public static Oauth2Credentials fromJson(String secretName, ObjectMapper json) {
    return attempt(() -> json.readValue(secretName, Oauth2Credentials.class)).orElseThrow();
  }

  public String toJsonString(ObjectMapper objectMapper) {
    return attempt(() -> objectMapper.writeValueAsString(this)).orElseThrow();
  }

  @Override
  @JsonIgnore
  public String getClientId() {
    return clientId();
  }

  @Override
  @JsonIgnore
  public String getClientSecret() {
    return clientSecret();
  }

  @Override
  @JsonIgnore
  public URI getAuthorizationEndpoint() {
    return authEndpointUri();
  }

  @Override
  public String getTag() {
    return tag();
  }

  @JsonProperty(value = "type", access = Access.READ_ONLY)
  public String getType() {
    return TYPE;
  }
}
