package com.github.awsjavakit.http;

import com.github.awsjavakit.http.token.OAuthTokenEntry;
import com.github.awsjavakit.http.updatestrategies.DefaultTokenCacheUpdateStrategy;
import com.github.awsjavakit.http.updatestrategies.TokenCacheUpdateStrategy;
import java.net.http.HttpClient;

public interface TokenProvider extends Tagged {

  /**
   * Creates a TokenProvider that every time it fetches a Bearer token, it requests the generation o
   * of a new token
   *
   * @param httpClient              a simple HttpClient.
   * @param authCredentialsProvider an OAuthCredentials provider supplying the credentials for
   *                                generating new tokens
   * @return a TokenProvider
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
    return new CachedTokenProvider(tokenRefresher, CachedTokenProvider.defaultStrategy());
  }

  static TokenCacheUpdateStrategy defaultUpdateStrategy() {
    return new DefaultTokenCacheUpdateStrategy(
      DefaultTokenCacheUpdateStrategy.MINIMUM_SLEEP_AMOUNT,
      DefaultTokenCacheUpdateStrategy.MAX_SLEEP_AMOUNT);
  }

  OAuthTokenEntry fetchToken();

}
