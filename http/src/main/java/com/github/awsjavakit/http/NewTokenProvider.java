package com.github.awsjavakit.http;

import static com.gtihub.awsjavakit.attempt.Try.attempt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.awsjavakit.misc.paths.UriWrapper;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

/**
 * Class that fetches a new Bearer token, using an {@link OAuthCredentialsProvider}.
 */
public class NewTokenProvider implements TokenProvider {

  public static final String JWT_TOKEN_FIELD = "access_token";
  public static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
  public static final String AUTHORIZATION_HEADER = "Authorization";
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final Map<String, String> GRANT_TYPE_CLIENT_CREDENTIALS = Map.of("grant_type",
    "client_credentials");
  private static final String CONTENT_TYPE_HEADER = "Content-Type";
  public static final String DUMMY_HOST = "notimportant";

  private final HttpClient httpClient;
  private final OAuthCredentialsProvider credentialsProvider;

  protected NewTokenProvider(HttpClient httpClient,
    OAuthCredentialsProvider credentialsProvider) {
    this.httpClient = httpClient;
    this.credentialsProvider = credentialsProvider;
  }

  public static NewTokenProvider create(HttpClient httpClient,
    OAuthCredentialsProvider credentialsProvider){
    return new NewTokenProvider(httpClient,credentialsProvider);
  }

  @Override
  public String fetchToken() {
    return authenticate();
  }

  private String authenticate() {
    var request = formatRequestForOauth2Token();
    return sendRequestAndExtractToken(request);
  }

  private HttpRequest formatRequestForOauth2Token() {
    return HttpRequest.newBuilder(credentialsProvider.getAuthorizationEndpoint())
      .header(AUTHORIZATION_HEADER, credentialsProvider.getAuthorizationHeader())
      .header(CONTENT_TYPE_HEADER, APPLICATION_X_WWW_FORM_URLENCODED)
      .POST(clientCredentialsInXWwwFormUrlEncodedBody())
      .build();
  }

  private HttpRequest.BodyPublisher clientCredentialsInXWwwFormUrlEncodedBody() {
    var queryParameters = UriWrapper.fromHost(DUMMY_HOST)
      .addQueryParameters(GRANT_TYPE_CLIENT_CREDENTIALS).getUri().getRawQuery();
    return HttpRequest.BodyPublishers.ofString(queryParameters);
  }

  private String sendRequestAndExtractToken(HttpRequest request) {
    return attempt(
      () -> this.httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8)))
      .map(HttpResponse::body)
      .map(JSON::readTree)
      .map(json -> json.get(JWT_TOKEN_FIELD))
      .map(JsonNode::textValue)
      .map(Objects::toString)
      .orElseThrow();
  }
}
