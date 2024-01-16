package com.github.awsjavakit.http;

import static java.util.Objects.isNull;

import com.github.awsjavakit.http.token.OAuthTokenEntry;

public class CachedTokenProvider implements TokenProvider {

  private final TokenProvider tokenProvider;
  private OAuthTokenEntry token;

  public CachedTokenProvider(TokenProvider newTokenProvider) {
    this.tokenProvider = newTokenProvider;

  }

  @Override
  public OAuthTokenEntry fetchToken() {
    if (shouldRenewToken()) {
      token = renewToken();
    }
    return token;
  }

  @Override
  public String tag() {
    return tokenProvider.tag();
  }

  private OAuthTokenEntry renewToken() {
    return tokenProvider.fetchToken();
  }

  private boolean shouldRenewToken() {
    return isNull(token) || token.hasExpired();
  }
}
