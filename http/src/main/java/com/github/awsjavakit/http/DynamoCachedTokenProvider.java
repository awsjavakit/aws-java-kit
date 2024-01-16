package com.github.awsjavakit.http;

import com.github.awsjavakit.http.token.OAuthTokenEntry;
import com.github.awsjavakit.http.updatestrategies.TokenCacheUpdateStrategy;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

public abstract class DynamoCachedTokenProvider implements TokenProvider {

  private final TokenProvider newTokenProvider;
  private final DynamoDbClient dynamoClient;
  private final String tableName;
  private final TokenCacheUpdateStrategy<OAuthTokenEntry> strategy;

  public DynamoCachedTokenProvider(TokenProvider newTokenProvider,
    DynamoDbClient dynamoClient,
    String tableName,
    TokenCacheUpdateStrategy<OAuthTokenEntry> strategy) {
    this.newTokenProvider = newTokenProvider;
    this.dynamoClient = dynamoClient;
    this.tableName = tableName;
    this.strategy = strategy;
  }

  public DynamoCachedTokenProvider(TokenProvider newTokenProvider,
    DynamoDbClient dynamoClient,
    String tableName) {
    this(newTokenProvider, dynamoClient, tableName, TokenProvider.defaultUpdateStrategy());
  }

  @Override
  public OAuthTokenEntry fetchToken() {
    return strategy.fetchAndUpdate(this::fetchCachedEntry, this::updateCache);
  }

  @Override
  public final String tag() {
    return newTokenProvider.tag();
  }

  protected abstract Optional<OAuthTokenEntry> fromGetResponse(GetItemResponse response);

  protected abstract Map<String, AttributeValue> createSearchKey();

  protected abstract Map<String, AttributeValue> toItem(OAuthTokenEntry token);

  private void persistToken(OAuthTokenEntry token) {
    var item = toItem(token);
    var request = PutItemRequest.builder()
      .tableName(tableName)
      .item(item)
      .build();
    dynamoClient.putItem(request);
  }

  private OAuthTokenEntry updateCache() {
    var newToken = newTokenProvider.fetchToken();
    persistToken(newToken);
    return newToken;
  }

  private OAuthTokenEntry fetchCachedEntry() {
    return fromGetResponse(dynamoClient.getItem(createGetItemRequest())).orElse(null);
  }

  private GetItemRequest createGetItemRequest() {
    return GetItemRequest.builder()
      .key(createSearchKey())
      .tableName(tableName)
      .build();
  }
}
