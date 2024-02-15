package com.github.awsjavakit.http.updatestrategies;

import static java.util.Objects.isNull;

import com.github.awsjavakit.http.token.OAuthTokenEntry;
import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;

public class DefaultTokenCacheUpdateStrategy implements TokenCacheUpdateStrategy {

  public static final int MINIMUM_SLEEP_AMOUNT = 100;
  public static final int MAX_SLEEP_AMOUNT = 400;
  public static final Random RANDOM = new Random();
  private final int minSleepPeriod;
  private final int maxSleepPeriod;

  public DefaultTokenCacheUpdateStrategy(int minSleepPeriod, int maxSleepPeriod) {
    this.minSleepPeriod = minSleepPeriod;
    this.maxSleepPeriod = maxSleepPeriod;
  }

  @Override
  public OAuthTokenEntry fetchAndUpdate(
    Supplier<OAuthTokenEntry> fetchCachedEntry,
    Supplier<OAuthTokenEntry> updateCache) {
    return fetchToken(fetchCachedEntry)
      .or(() -> someOtherProcessMightBeGeneratingAToken(fetchCachedEntry))
      .orElseGet(updateCache);

  }

  private Optional<OAuthTokenEntry> fetchToken(Supplier<OAuthTokenEntry> tokenSupplier) {
    var token = tokenSupplier.get();
    return isNotValid(token) ? Optional.empty() : Optional.of(token);
  }

  private boolean isNotValid(OAuthTokenEntry token) {
    return isNull(token) || token.hasExpired();
  }

  private Optional<OAuthTokenEntry> someOtherProcessMightBeGeneratingAToken(
    Supplier<OAuthTokenEntry> tokenSupplier) {
    sleep();
    return fetchToken(tokenSupplier);
  }

  private void sleep() {
    try {
      Thread.sleep(minSleepPeriod + RANDOM.nextLong(maxSleepPeriod - minSleepPeriod));
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

}
