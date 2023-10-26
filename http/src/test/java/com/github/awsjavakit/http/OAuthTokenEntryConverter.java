package com.github.awsjavakit.http;

import static com.github.awsjavakit.http.DynamoTest.PARTITION_KEY;
import static com.github.awsjavakit.http.DynamoTest.SORT_KEY;
import static com.github.awsjavakit.http.JsonConfig.fromJson;
import static com.github.awsjavakit.http.JsonConfig.toJson;

import com.github.awsjavakit.http.token.OAuthTokenEntry;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public final class OAuthTokenEntryConverter {

  public static final String BODY_FIELD = "body";

  private OAuthTokenEntryConverter() {

  }

  public static Map<String, AttributeValue> toItem(OAuthTokenEntry token) {
    var body = EnhancedDocument.fromJson(toJson(token)).toMap();
    var item = new ConcurrentHashMap<String, AttributeValue>();
    item.put(PARTITION_KEY, AttributeValue.builder().s(OAuthTokenEntry.TYPE).build());
    item.put(SORT_KEY, AttributeValue.builder().s(OAuthTokenEntry.TYPE).build());
    item.put(BODY_FIELD, AttributeValue.builder().m(body).build());
    return item;
  }

  public static OAuthTokenEntry fromItem(Map<String, AttributeValue> item) {
    var body = item.get(BODY_FIELD);
    var json = EnhancedDocument.fromAttributeValueMap(body.m()).toJson();
    return fromJson(json, OAuthTokenEntry.class);
  }
}


