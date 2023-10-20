package com.github.awsjavakit.http;

import java.net.URI;

public record SimpleCredentialsProvider(String clientId, String clientSecret, URI authEndpoint)
  implements OAuthCredentialsProvider {

  @Override
  public String getClientId() {
    return clientId;
  }

  @Override
  public String getClientSecret() {
    return clientSecret;
  }

  @Override
  public URI getAuthorizationEndpoint() {
    return authEndpoint;
  }
}
