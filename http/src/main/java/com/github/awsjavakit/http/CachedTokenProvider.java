package com.github.awsjavakit.http;

import com.github.awsjavakit.http.token.OAuthTokenEntry;
import com.github.awsjavakit.http.updatestrategies.TokenCacheUpdateStrategy;

public class CachedTokenProvider implements TokenProvider {

  private final TokenProvider tokenProvider;
  private final TokenCacheUpdateStrategy updateStrategy;
  private OAuthTokenEntry token;

  public CachedTokenProvider(TokenProvider newTokenProvider,
    TokenCacheUpdateStrategy updateStrategy) {
    this.tokenProvider = newTokenProvider;
    this.updateStrategy = updateStrategy;

  }

  @Override
  public OAuthTokenEntry fetchToken() {
    return updateStrategy.fetchAndUpdate(() -> token, this::updateCache);
  }

  @Override
  public String getTag() {
    return tokenProvider.getTag();
  }

  private OAuthTokenEntry updateCache() {
    token = tokenProvider.fetchToken();
    return token;
  }
}
