package com.github.awsjavakit.testingutils.aws;

import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomJson;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishBatchRequest;
import software.amazon.awssdk.services.sns.model.PublishBatchRequestEntry;
import software.amazon.awssdk.services.sns.model.PublishRequest;

class FakeSnsClientTest {

  private FakeSnsClient client;
  private String message;
  private String topicArn;
  private PublishRequest publishRequest;
  private String batchId;

  @BeforeEach
  public void init() {
    this.client = new FakeSnsClient();
    this.message = randomJson();
    this.topicArn = randomString();
    this.batchId = UUID.randomUUID().toString();
    this.publishRequest =  PublishRequest.builder()
      .messageDeduplicationId(randomString())
      .messageAttributes(randomMap())
      .messageStructure(randomString())
      .messageGroupId(randomString())
      .subject(randomString())
      .message(message)
      .topicArn(topicArn)
      .build();
  }

  @Test
  void shouldAllowPublishingMessages() {
    client.publish(publishRequest);

    assertThat(client.getPublishRequests(), contains(publishRequest));
  }

  @Test
  void shouldAllowPublishingBatchMessages() {
    var sampleBatchRequest = createSampleBatchRequest();

    client.publishBatch(sampleBatchRequest);

    assertThat(client.getPublishRequests(), contains(publishRequest));

  }


  private PublishBatchRequest createSampleBatchRequest() {

    var entries = PublishBatchRequestEntry.builder()
      .message(message)
      .id(batchId)
      .messageStructure(publishRequest.messageStructure())
      .messageDeduplicationId(publishRequest.messageDeduplicationId())
      .subject(publishRequest.subject())
      .messageAttributes(publishRequest.messageAttributes())
      .messageGroupId(publishRequest.messageGroupId())
      .build();
    return PublishBatchRequest.builder()
      .topicArn(publishRequest.topicArn())
      .publishBatchRequestEntries(entries)
      .build();
  }



  private Map<String, MessageAttributeValue> randomMap() {
    return Map.of(randomString(), MessageAttributeValue.builder().stringValue(randomString()).build());
  }

}