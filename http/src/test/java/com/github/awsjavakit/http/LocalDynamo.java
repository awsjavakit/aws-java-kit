package com.github.awsjavakit.http;


import java.util.Collection;
import java.util.List;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.dynamodb.services.local.embedded.DynamoDBEmbedded;
import software.amazon.dynamodb.services.local.shared.access.AmazonDynamoDBLocal;

public class LocalDynamo {

  public static final String PARTITION_KEY = "PK";
  public static final String SORT_KEY = "SK";
  protected DynamoDbClient dynamoClient;

  public void initDatabase(String tableName) {
    var dynamo = localDynamo();
    this.dynamoClient = dynamo.dynamoDbClient();
    setupTable(tableName);
  }

  public AmazonDynamoDBLocal localDynamo() {
    return DynamoDBEmbedded.create();
  }

  private static KeySchemaElement createKey(String attributeName, KeyType keyType) {
    return KeySchemaElement.builder()
      .attributeName(attributeName)
      .keyType(keyType)
      .build();
  }

  private void setupTable(String tableName) {
    dynamoClient.createTable(CreateTableRequest.builder()
      .tableName(tableName)
      .billingMode(BillingMode.PAY_PER_REQUEST)
      .attributeDefinitions(createAttributeDefinitions())
      .keySchema(createKeySchema())
      .build());
  }

  private Collection<KeySchemaElement> createKeySchema() {
    return List.of(createKey(PARTITION_KEY, KeyType.HASH),
      createKey(SORT_KEY, KeyType.RANGE));
  }

  private List<AttributeDefinition> createAttributeDefinitions() {
    var partitionKey = AttributeDefinition.builder()
      .attributeName(PARTITION_KEY)
      .attributeType(ScalarAttributeType.S)
      .build();
    var sortKey = AttributeDefinition.builder()
      .attributeName(SORT_KEY)
      .attributeType(ScalarAttributeType.S)
      .build();
    return List.of(partitionKey, sortKey);
  }

}
