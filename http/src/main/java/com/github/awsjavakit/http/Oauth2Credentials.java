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
import java.util.Objects;

/**
 * Structure containing all the necessary information for an OAuth2 authentication handshake with
 * for grant_type "client_credentials".
 *
 */

@JsonTypeInfo(use = Id.NAME, include = As.EXISTING_PROPERTY, property = "type")
@JsonTypeName(Oauth2Credentials.TYPE)
public final class Oauth2Credentials
  implements OAuthCredentialsProvider {

  public static final String TYPE = "OAuth2Credentials";
  @JsonProperty("authEndpointUri")
  private final URI authEndpointUri;
  @JsonProperty("clientId")
  private final String clientId;
  @JsonProperty("clientSecret")
  private final String clientSecret;
  @JsonProperty("tag")
  private final String tag;

  /**
   * @param authEndpointUri the URI of the authentication endpoint (e.g.
   *                        "https://auth.example.com/oauth2/token")
   * @param clientId        the client id (username)
   * @param clientSecret    the client secret (password)
   */
  public Oauth2Credentials(
    @JsonAlias("serverUri") @JsonProperty("authEndpointUri") URI authEndpointUri,
    @JsonProperty("clientId") String clientId,
    @JsonProperty("clientSecret") String clientSecret,
    @JsonProperty("tag") String tag) {
    this.authEndpointUri = authEndpointUri;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.tag = tag;
  }

  public static Oauth2Credentials fromJson(String secretName, ObjectMapper json) {
    return attempt(() -> json.readValue(secretName, Oauth2Credentials.class)).orElseThrow();
  }

  public String toJsonString(ObjectMapper objectMapper) {
    return attempt(() -> objectMapper.writeValueAsString(this)).orElseThrow();
  }

  @Override
  public String getClientId() {
    return clientId;
  }

  @Override
  public String getClientSecret() {
    return clientSecret;
  }

  @Override
  @JsonIgnore
  public URI getAuthorizationEndpoint() {
    return getAuthEndpointUri();
  }

  @Override
  public String getTag() {
    return tag;
  }

  @JsonProperty(value = "type", access = Access.READ_ONLY)
  public String getType() {
    return TYPE;
  }


  public URI getAuthEndpointUri() {
    return authEndpointUri;
  }


  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (Oauth2Credentials) obj;
    return Objects.equals(this.authEndpointUri, that.authEndpointUri) &&
      Objects.equals(this.clientId, that.clientId) &&
      Objects.equals(this.clientSecret, that.clientSecret) &&
      Objects.equals(this.tag, that.tag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(authEndpointUri, clientId, clientSecret, tag);
  }

  @Override
  public String toString() {
    return "Oauth2Credentials[" +
      "authEndpointUri=" + authEndpointUri + ", " +
      "clientId=" + clientId + ", " +
      "clientSecret=" + clientSecret + ", " +
      "tag=" + tag + ']';
  }

}
