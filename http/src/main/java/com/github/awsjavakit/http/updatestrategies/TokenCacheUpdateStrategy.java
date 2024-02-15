package com.github.awsjavakit.http.updatestrategies;

import static com.github.awsjavakit.http.updatestrategies.DefaultTokenCacheUpdateStrategy.MAX_SLEEP_AMOUNT;
import static com.github.awsjavakit.http.updatestrategies.DefaultTokenCacheUpdateStrategy.MINIMUM_SLEEP_AMOUNT;

import com.github.awsjavakit.http.token.OAuthTokenEntry;
import java.util.function.Supplier;

/**
 * Given a function that provides a new Token and updates some cache (updateCache), and a function
 * that provides an already cached entry, then this function decides how these functions will be
 * combined to provide a valid token. Examples:
 * <ul>
 * <li>
 *
 * <pre>{@code
 *  public OAuthTokenEntry fetchAndUpdate(
 *    Supplier<OAuthTokenEntry> fetchCachedEntry,
 *    Supplier<OAuthTokenEntry> updateCache) {
 *    return Optional.ofNullable(() -> fetchCachedEntry.get())
 *       orEleGet(() -> updateCache.get())
 *  }
 * }</pre>
 * </li>
 * <li>{@link DefaultTokenCacheUpdateStrategy} </li>
 * </ul>
 */
public interface TokenCacheUpdateStrategy {

  static DefaultTokenCacheUpdateStrategy defaultStrategy() {
    return new DefaultTokenCacheUpdateStrategy(MINIMUM_SLEEP_AMOUNT, MAX_SLEEP_AMOUNT);
  }

  OAuthTokenEntry fetchAndUpdate(Supplier<OAuthTokenEntry> fetchCachedEntry,
    Supplier<OAuthTokenEntry> updateCache);
}
