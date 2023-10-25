package com.github.awsjavakit.http.updatestrategies;

import java.util.function.Supplier;

public interface TokenCacheUpdateStrategy<T> {

  T fetchAndUpdate(Supplier<T> updateEntry, Supplier<T> fetchEntry);
}
