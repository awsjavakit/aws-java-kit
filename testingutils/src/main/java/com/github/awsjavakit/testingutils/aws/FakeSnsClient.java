package com.github.awsjavakit.testingutils.aws;

import com.github.awsjavakit.misc.JacocoGenerated;
import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishBatchRequest;
import software.amazon.awssdk.services.sns.model.PublishBatchRequestEntry;
import software.amazon.awssdk.services.sns.model.PublishBatchResponse;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

public class FakeSnsClient implements SnsClient {

  private final List<PublishRequest> publishRequests;

  public FakeSnsClient() {
    this.publishRequests = new ArrayList<>();
  }


  @JacocoGenerated
  @Override
  public String serviceName() {
    return "FakeSnsClient";
  }

  @JacocoGenerated
  @Override
  public void close() {
    //NO-OP;
  }

  public List<PublishRequest> getPublishRequests() {
    return publishRequests;
  }

  @Override
  public PublishResponse publish(PublishRequest publishRequest) {
    this.publishRequests.add(publishRequest);
    return PublishResponse.builder().build();
  }

  @Override
  public PublishBatchResponse publishBatch(PublishBatchRequest publishBatchRequest) {
    var batchEntries = publishBatchRequest.publishBatchRequestEntries();
    var requests = batchEntries.stream()
      .map(batchEntry -> toRequest(batchEntry, publishBatchRequest)).toList();
    this.publishRequests.addAll(requests);
    return PublishBatchResponse.builder().build();
  }

  private PublishRequest toRequest(PublishBatchRequestEntry batchEntry,
                                   PublishBatchRequest publishBatchRequest) {
    return PublishRequest.builder()
      .topicArn(publishBatchRequest.topicArn())
      .message(batchEntry.message())
      .messageGroupId(batchEntry.messageGroupId())
      .messageStructure(batchEntry.messageStructure())
      .messageAttributes(batchEntry.messageAttributes())
      .messageDeduplicationId(batchEntry.messageDeduplicationId())
      .subject(batchEntry.subject())
      .build();
  }
}
