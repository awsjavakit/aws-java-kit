package com.github.awsjavakit.http;

import com.github.awsjavakit.http.token.OAuthTokenEntry;
import com.github.awsjavakit.http.updatestrategies.DefaultTokenCacheUpdateStrategy;
import com.github.awsjavakit.http.updatestrategies.TokenCacheUpdateStrategy;
import java.net.http.HttpClient;
import software.amazon.awssdk.services.ssm.SsmClient;

public interface TokenProvider {

  /**
   * Creates a TokenProvider that every time it fetches a Bearer token, it requests the generation o
   * of a new token
   *
   * @param httpClient
   * @param authCredentialsProvider
   * @return
   */
  static TokenProvider defaultProvider(
    HttpClient httpClient,
    OAuthCredentialsProvider authCredentialsProvider) {
    return NewTokenProvider.create(httpClient, authCredentialsProvider);
  }

  static TokenProvider locallyCachedTokenProvider(
    HttpClient httpClient,
    OAuthCredentialsProvider authCredentialsProvider) {
    var tokenRefresher =
      NewTokenProvider.create(httpClient, authCredentialsProvider);
    return new CachedTokenProvider(tokenRefresher);
  }

  static TokenCacheUpdateStrategy<OAuthTokenEntry> defaultUpdateStrategy() {
    return new DefaultTokenCacheUpdateStrategy(
      DefaultTokenCacheUpdateStrategy.MINIMUM_SLEEP_AMOUNT,
      DefaultTokenCacheUpdateStrategy.MAX_SLEEP_AMOUNT);
  }

  static ParameterStoreCachedTokenProvider parameterStoreCachedProvider(
    TokenProvider newTokenProvider,
    String parameterName,
    SsmClient ssmClient,
    TokenCacheUpdateStrategy<OAuthTokenEntry> tokenCacheEntryUpdateStrategy) {
    return new ParameterStoreCachedTokenProvider(newTokenProvider,
      parameterName,
      ssmClient,
      tokenCacheEntryUpdateStrategy);
  }

  OAuthTokenEntry fetchToken();
}
