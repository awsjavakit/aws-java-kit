package com.github.awsjavakit.testingutils.aws;

import static java.util.Objects.isNull;

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

public class FakeSqsClient implements SqsClient {

  private final List<SendMessageRequest> sentMessages;

  public FakeSqsClient() {
    this.sentMessages = new ArrayList<>();
  }

  @Override
  public String serviceName() {
    return "sqsclient";
  }

  @Override
  public void close() {
    //NO-OP
  }

  @Override
  public SendMessageResponse sendMessage(SendMessageRequest sendMessageRequest) {
    validateMessageRequest(sendMessageRequest);
    sentMessages.add(sendMessageRequest);
    return SendMessageResponse.builder().build();
  }

  @Override
  public SendMessageBatchResponse sendMessageBatch(SendMessageBatchRequest sendMessageBatchRequest) {
    sendMessageBatchRequest.entries().stream()
      .map(entry -> toSendMessageRequest(entry, sendMessageBatchRequest))
      .forEach(this::sendMessage);
    return SendMessageBatchResponse.builder().build();
  }

  public List<SendMessageRequest> getSentMessages() {
    return sentMessages;
  }

  private SendMessageRequest toSendMessageRequest(SendMessageBatchRequestEntry entry,
    SendMessageBatchRequest sendMessageBatchRequest) {
    return SendMessageRequest.builder()
      .messageBody(entry.messageBody())
      .messageAttributes(entry.messageAttributes())
      .queueUrl(sendMessageBatchRequest.queueUrl())
      .build();
  }

  private void validateMessageRequest(SendMessageRequest sendMessageRequest) {
    if (isNull(sendMessageRequest.queueUrl())) {
      throw new IllegalArgumentException("queueUrl cannot be null");
    }
  }
}
