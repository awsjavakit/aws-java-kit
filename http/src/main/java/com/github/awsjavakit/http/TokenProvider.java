package com.github.awsjavakit.http;

import java.net.http.HttpClient;

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

  String fetchToken();
}
