package com.github.awsjavakit.http;

import java.net.http.HttpClient;
import java.time.Duration;

public interface TokenProvider {

  /**
   * Creates a TokenProvider that every time it fetches a Bearer token, it requests the generation o
   * of a new token
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
    OAuthCredentialsProvider authCredentialsProvider,
    Duration maxTokenAge) {
    var tokenRefresher =
      NewTokenProvider.create(httpClient,authCredentialsProvider);
    return new CachedTokenProvider(tokenRefresher,maxTokenAge);
  }


  String fetchToken();
}
