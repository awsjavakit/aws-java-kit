package com.github.awsjavakit.http;

import static java.util.Objects.isNull;

import com.github.awsjavakit.http.token.OAuthTokenEntry;
import java.time.Duration;
import java.time.Instant;

public class CachedTokenProvider implements TokenProvider {

  private final TokenProvider tokenProvider;
  private final Duration maxTokenAge;
  private OAuthTokenEntry token;
  private Instant tokenTimestamp;

  public CachedTokenProvider(TokenProvider newTokenProvider, Duration maxTokenAge) {
    this.tokenProvider = newTokenProvider;
    this.maxTokenAge = maxTokenAge;
  }

  @Override
  public OAuthTokenEntry fetchToken() {
    var now = Instant.now();
    if (shouldRenewToken(now)) {
      renewToken(now);
    }
    return token;
  }

  private void renewToken(Instant now) {
    token = tokenProvider.fetchToken();
    tokenTimestamp = now;
  }

  private boolean shouldRenewToken(Instant now) {
    return isNull(token) || now.isAfter(tokenTimestamp.plus(maxTokenAge));
  }
}
