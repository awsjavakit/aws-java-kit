package com.github.awsjavakit.http;

import com.github.awsjavakit.misc.paths.UriWrapper;
import java.net.URI;
import java.util.Base64;

/**
 * This interface is used by {@link OAuth2HttpClient}. It provides the basic information for
 * a successful OAuth2 authentication handshake for the "grant_type" "client_credentials".
 */
public interface OAuthCredentialsProvider {
  String OAUTH2_TOKEN_PATH = "/oauth2/token";
  String getClientId();

  String getClientSecret();

  URI getAuthServerUri();

  default URI getAuthorizationEndpoint(){
    return UriWrapper.fromUri(getAuthServerUri()).addChild(OAUTH2_TOKEN_PATH).getUri();
  }

  default String getAuthorizationHeader(){
    return "Basic " + Base64.getEncoder().encodeToString(formatCredentialsForBasicAuth());
  }

  private byte[] formatCredentialsForBasicAuth() {
    return String.format("%s:%s", getClientId(), getClientSecret()).getBytes();
  }
}
