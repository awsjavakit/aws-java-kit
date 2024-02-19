package com.github.awsjavakit.http;

import static java.util.function.Predicate.not;

import com.github.awsjavakit.http.token.OAuthTokenEntry;
import com.github.awsjavakit.http.updatestrategies.TokenCacheUpdateStrategy;
import java.util.Optional;
import java.util.function.Supplier;

public class CachedTokenProvider implements TokenProvider {

  private final TokenProvider tokenProvider;
  private final TokenCacheUpdateStrategy updateStrategy;
  private OAuthTokenEntry token;

  public CachedTokenProvider(TokenProvider newTokenProvider,
    TokenCacheUpdateStrategy updateStrategy) {
    this.tokenProvider = newTokenProvider;
    this.updateStrategy = updateStrategy;

  }

  public static TokenCacheUpdateStrategy defaultStrategy() {
    return new LocalCacheUpdateStrategy();
  }

  @Override
  public OAuthTokenEntry fetchToken() {
    return updateStrategy.fetchAndUpdate(this::fetchCachedEntry, this::updateCache);
  }

  @Override
  public String getTag() {
    return tokenProvider.getTag();
  }

  private OAuthTokenEntry fetchCachedEntry() {
    return token;
  }

  private OAuthTokenEntry updateCache() {
    token = tokenProvider.fetchToken();
    return token;
  }

  public static class LocalCacheUpdateStrategy implements TokenCacheUpdateStrategy {

    @Override
    public OAuthTokenEntry fetchAndUpdate(Supplier<OAuthTokenEntry> fetchCachedEntry,
      Supplier<OAuthTokenEntry> updateCache) {
      return Optional.ofNullable(fetchCachedEntry.get())
        .filter(not(OAuthTokenEntry::hasExpired))
        .orElseGet(updateCache);
    }
  }
}
