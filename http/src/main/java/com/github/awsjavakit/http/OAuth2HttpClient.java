package com.github.awsjavakit.http;

import static com.gtihub.awsjavakit.attempt.Try.attempt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.awsjavakit.misc.paths.UriWrapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

public class OAuth2HttpClient {

  private static final String JWT_TOKEN_FIELD = "access_token";
  private static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final Map<String, String> GRANT_TYPE_CLIENT_CREDENTIALS = Map.of("grant_type",
    "client_credentials");
  private static final String CONTENT_TYPE_HEADER = "Content-Type";

  private final HttpClient httpClient;
  private final URI authUrl;
  private final OAuthCredentialsProvider credentialsProvider;

  protected OAuth2HttpClient(HttpClient httpClient, URI authUrl,
    OAuthCredentialsProvider credentialsProvider) {
    this.httpClient = httpClient;
    this.authUrl = authUrl;
    this.credentialsProvider = credentialsProvider;
  }

  public static OAuth2HttpClient create(HttpClient httpClient,
    URI authUrl,
    OAuthCredentialsProvider credentialsProvider) {
    return new OAuth2HttpClient(httpClient, authUrl, credentialsProvider);

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

  private static URI standardOauth2TokenEndpoint(URI cognitoHost) {
    return UriWrapper.fromUri(cognitoHost).addChild("oauth2").addChild("token").getUri();
  }

  private String authenticate() {
    var tokenUri = standardOauth2TokenEndpoint(this.authUrl);
    var request = formatRequestForJwtToken(tokenUri);
    return sendRequestAndExtractToken(request);
  }

  private HttpRequest formatRequestForJwtToken(URI tokenUri) {
    return HttpRequest.newBuilder(tokenUri)
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
