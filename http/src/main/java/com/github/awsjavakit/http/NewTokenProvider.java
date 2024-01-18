package com.github.awsjavakit.http;

import static com.github.awsjavakit.http.HttpConstants.HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED;
import static com.github.awsjavakit.http.HttpConstants.HttpHeaders.AUTHORIZATION;
import static com.github.awsjavakit.http.HttpConstants.HttpHeaders.CONTENT_TYPE;
import static com.github.awsjavakit.http.JsonConfig.fromJson;
import static com.gtihub.awsjavakit.attempt.Try.attempt;

import com.github.awsjavakit.http.token.OAuthTokenEntry;
import com.github.awsjavakit.http.token.OAuthTokenResponse;
import com.github.awsjavakit.misc.paths.UriWrapper;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Class that fetches a new Bearer token, using an {@link OAuthCredentialsProvider}.
 */
public class NewTokenProvider implements TokenProvider {

  private static final String DUMMY_HOST = "notimportant";

  private static final Map<String, String> GRANT_TYPE_CLIENT_CREDENTIALS = Map.of("grant_type",
    "client_credentials");
  private static final String AUTH_REQUEST_BODY =
    formatPostParametersAsXWwwFormUrlEncodedBody();

  private final HttpClient httpClient;
  private final OAuthCredentialsProvider credentialsProvider;

  protected NewTokenProvider(HttpClient httpClient,
    OAuthCredentialsProvider credentialsProvider) {
    this.httpClient = httpClient;
    this.credentialsProvider = credentialsProvider;
  }

  public static NewTokenProvider create(HttpClient httpClient,
    OAuthCredentialsProvider credentialsProvider) {
    return new NewTokenProvider(httpClient, credentialsProvider);
  }

  @Override
  public OAuthTokenEntry fetchToken() {
    return OAuthTokenEntry.fromResponse(authenticate(), credentialsProvider.getTag());
  }

  @Override
  public String getTag() {
    return credentialsProvider.getTag();
  }

  private static String formatPostParametersAsXWwwFormUrlEncodedBody() {
    return UriWrapper.fromHost(DUMMY_HOST)
      .addQueryParameters(GRANT_TYPE_CLIENT_CREDENTIALS).getUri().getRawQuery();
  }

  private OAuthTokenResponse authenticate() {
    var request = formatRequestForOauth2Token();
    return sendRequestAndExtractToken(request);
  }

  private HttpRequest formatRequestForOauth2Token() {
    return HttpRequest.newBuilder(credentialsProvider.getAuthorizationEndpoint())
      .header(AUTHORIZATION, credentialsProvider.getAuthorizationHeader())
      .header(CONTENT_TYPE, APPLICATION_X_WWW_FORM_URLENCODED)
      .POST(clientCredentialsInXWwwFormUrlEncodedBody())
      .build();
  }

  private HttpRequest.BodyPublisher clientCredentialsInXWwwFormUrlEncodedBody() {
    return HttpRequest.BodyPublishers.ofString(AUTH_REQUEST_BODY);
  }

  private OAuthTokenResponse sendRequestAndExtractToken(HttpRequest request) {
    return attempt(
      () -> this.httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8)))
      .map(HttpResponse::body)
      .map(body -> fromJson(body, OAuthTokenResponse.class))
      .orElseThrow();
  }
}
