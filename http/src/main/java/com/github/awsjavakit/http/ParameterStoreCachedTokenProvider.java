package com.github.awsjavakit.http;

import static java.util.Objects.isNull;

import java.time.Duration;

public class ParameterStoreCachedTokenProvider implements TokenProvider {

  private final NewTokenProvider newTokenProvider;
  private final Duration tokenAge;
  private String token;

  public ParameterStoreCachedTokenProvider(NewTokenProvider newTokenProvider,
    Duration tokenAge ) {

    this.newTokenProvider = newTokenProvider;
    this.tokenAge = tokenAge;
  }

  @Override
  public String fetchToken() {
    if (isNull(token)) {
      token = newTokenProvider.fetchToken();
    }
    return token;
  }
}
