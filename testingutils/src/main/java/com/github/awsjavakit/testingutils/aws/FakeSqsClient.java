package com.github.awsjavakit.testingutils.aws;

import static java.util.Objects.isNull;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.MessageAttribute;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

public class FakeSqsClient implements SqsClient {

  public static final int AWS_HARD_LIMIT_ON_BATCH_SIZE = 10;
  public static final String BATCH_SIZE_ERROR = "Batch size should not be greater than "+
    AWS_HARD_LIMIT_ON_BATCH_SIZE;
  private final List<SendMessageRequest> messages;

  public FakeSqsClient() {
    this.messages = new ArrayList<>();
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
    messages.add(sendMessageRequest);
    return SendMessageResponse.builder().build();
  }

  @Override
  public SendMessageBatchResponse sendMessageBatch(
    SendMessageBatchRequest sendMessageBatchRequest) {
    validateBatch(sendMessageBatchRequest);
    sendMessageBatchRequest.entries().stream()
      .map(entry -> toSendMessageRequest(entry, sendMessageBatchRequest))
      .forEach(this::sendMessage);
    return SendMessageBatchResponse.builder().build();
  }

  public List<SendMessageRequest> getSendMessageRequests() {
    return messages;
  }

  public SQSEvent createEvent() {
    var event = new SQSEvent();
    event.setRecords(convertMessagesToRecords());
    return event;
  }

  private static void validateBatch(SendMessageBatchRequest sendMessageBatchRequest) {
    if (isFollowingAwsQuota(sendMessageBatchRequest)) {
      throw new IllegalArgumentException(BATCH_SIZE_ERROR);
    }
  }

  private static boolean isFollowingAwsQuota(SendMessageBatchRequest sendMessageBatchRequest) {
    return sendMessageBatchRequest.entries().size() > AWS_HARD_LIMIT_ON_BATCH_SIZE;
  }

  private List<SQSMessage> convertMessagesToRecords() {
    return messages.stream().map(this::convertSendRequestToSqsMessage).toList();
  }

  private SQSMessage convertSendRequestToSqsMessage(SendMessageRequest request) {
    var message = new SQSEvent.SQSMessage();
    message.setBody(request.messageBody());
    message.setMessageAttributes(convertMessageAttributes(request.messageAttributes()));
    return message;
  }

  private Map<String, MessageAttribute> convertMessageAttributes(
    Map<String, MessageAttributeValue> messageAttributeValues) {
    return messageAttributeValues.entrySet().stream()
      .map(entry -> Map.entry(entry.getKey(), attributeValueToAttribute(entry.getValue())))
      .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

  }

  private MessageAttribute attributeValueToAttribute(MessageAttributeValue value) {
    var attribute = new MessageAttribute();
    attribute.setStringValue(value.stringValue());
    return attribute;
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
