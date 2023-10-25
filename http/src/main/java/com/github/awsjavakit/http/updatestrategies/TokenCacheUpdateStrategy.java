package com.github.awsjavakit.http.updatestrategies;

import com.github.awsjavakit.http.token.OAuthTokenEntry;
import java.util.function.Supplier;

public interface TokenCacheUpdateStrategy<T> {

  T fetchAndUpdate(Supplier<OAuthTokenEntry> fetchCachedEntry, Supplier<T> updateCache);
}
