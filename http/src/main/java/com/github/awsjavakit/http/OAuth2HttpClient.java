package com.github.awsjavakit.http;

import static com.gtihub.awsjavakit.attempt.Try.attempt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.awsjavakit.misc.paths.UriWrapper;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

public class OAuth2HttpClient {

  public static final String JWT_TOKEN_FIELD = "access_token";
  public static final String AUTHORIZATION_HEADER = "Authorization";
  public static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";

  private static final ObjectMapper JSON = new ObjectMapper();
  private static final Map<String, String> GRANT_TYPE_CLIENT_CREDENTIALS = Map.of("grant_type",
    "client_credentials");
  private static final String CONTENT_TYPE_HEADER = "Content-Type";

  private final HttpClient httpClient;
  private final OAuthCredentialsProvider credentialsProvider;

  protected OAuth2HttpClient(HttpClient httpClient,
    OAuthCredentialsProvider credentialsProvider) {
    this.httpClient = httpClient;
    this.credentialsProvider = credentialsProvider;
  }

  public static OAuth2HttpClient create(HttpClient httpClient,
    OAuthCredentialsProvider credentialsProvider) {
    return new OAuth2HttpClient(httpClient, credentialsProvider);

  }

  public <T> HttpResponse<T> send(HttpRequest.Builder requestBuilder,
    BodyHandler<T> responseBodyHandler)
    throws IOException, InterruptedException {
    var bearerToken = authenticate();
    var authorizedRequest = requestBuilder.setHeader(AUTHORIZATION_HEADER, bearerToken).build();
    return httpClient.send(authorizedRequest, responseBodyHandler);
  }

  private static HttpRequest.BodyPublisher clientCredentialsAuthType() {
    var queryParameters = UriWrapper.fromHost("notimportant")
      .addQueryParameters(GRANT_TYPE_CLIENT_CREDENTIALS).getUri().getRawQuery();
    return HttpRequest.BodyPublishers.ofString(queryParameters);
  }

  private String authenticate() {
    var request = formatRequestForOauth2Token();
    return sendRequestAndExtractToken(request);
  }

  private HttpRequest formatRequestForOauth2Token() {
    return HttpRequest.newBuilder(credentialsProvider.getAuthorizationEndpoint())
      .setHeader(AUTHORIZATION_HEADER, credentialsProvider.getAuthorizationHeader())
      .setHeader(CONTENT_TYPE_HEADER, APPLICATION_X_WWW_FORM_URLENCODED)
      .POST(clientCredentialsAuthType())
      .build();
  }

  private String createBearerToken(String accessToken) {
    return "Bearer " + accessToken;
  }

  private String sendRequestAndExtractToken(HttpRequest request) {
    return attempt(
      () -> this.httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8)))
      .map(HttpResponse::body)
      .map(body -> JSON.readTree(body))
      .map(json -> json.get(JWT_TOKEN_FIELD))
      .map(JsonNode::textValue)
      .map(Objects::toString)
      .map(this::createBearerToken)
      .orElseThrow();
  }

}
