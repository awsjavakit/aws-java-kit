package com.github.awsjavakit.testingutils.aws;

import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomUri;
import static com.github.awsjavakit.testingutils.aws.FakeSqsClient.AWS_HARD_LIMIT_ON_BATCH_SIZE;
import static com.github.awsjavakit.testingutils.aws.FakeSqsClient.BATCH_SIZE_ERROR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.amazonaws.services.lambda.runtime.events.SQSEvent.MessageAttribute;
import com.github.awsjavakit.misc.SingletonCollector;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

class FakeSqsClientTest {

  private FakeSqsClient client;

  @BeforeEach
  public void init() {
    this.client = new FakeSqsClient();
  }

  @Test
  void shouldReturnSendRequestsThatWereSent() {
    var client = new FakeSqsClient();
    var writeRequest = validMessage();

    client.sendMessage(writeRequest);

    var receiveMessageRequest = createReceiveMessageRequest(writeRequest);
    assertThrows(UnsupportedOperationException.class, () -> client.receiveMessage(receiveMessageRequest));

    assertThat(client.getSendMessageRequests()).containsExactly(writeRequest);

  }

  @Test
  void shouldThrowExceptionWhenSendingSingleMessageAndUriIsNotIncluded() {
    var message = invalidMessage();
    assertThrows(Exception.class, () -> client.sendMessage(message));
  }

  @Test
  void shouldThrowExceptionWhenSendingMessageBatchAndUriIsNotIncluded() {
    var message = invalidMessageBatch();
    assertThrows(Exception.class, () -> client.sendMessageBatch(message));
  }

  @Test
  void shouldTreatBatchRequestsAsManySingleSendRequests() {
    var request = sampleBatchRequest();
    client.sendMessageBatch(request);
    var expectedMessages = convertBatchEntriesToSingleRequests(request);
    var actualMessages = client.getSendMessageRequests();
    assertThat(actualMessages)
      .containsExactlyInAnyOrder(expectedMessages.toArray(SendMessageRequest[]::new));
  }

  @Test
  void shouldProvideSentMessagesAsSqsEventForSqsHandlers() {
    var sendRequest = validMessage();
    client.sendMessage(sendRequest);
    var event = client.createEvent();

    var messageAsInputToHandler = event.getRecords().stream().collect(SingletonCollector.collect());
    assertThat(messageAsInputToHandler.getBody()).isEqualTo(sendRequest.messageBody());

    for (var expectedAttribute : sendRequest.messageAttributes().entrySet()) {
      var actualAttributes = messageAsInputToHandler.getMessageAttributes();
      assertThat(actualAttributes).hasEntrySatisfying(
        expectedAttribute.getKey(), assertThatAttributeValuesAreEquivalent(expectedAttribute));
    }
  }

  @Test
  void shouldReturnSomeServiceName() {
    assertThat(client.serviceName()).isNotNull();
  }

  @Test
  void shouldNotThrowExceptionWhenClosing() {
    assertDoesNotThrow(() -> client.close());
  }

  @Test
  void shouldThrowErrorWhenSendingBatchWithSizeGreaterThanAllowedQuota() {
    var request = sampleBatchRequest(AWS_HARD_LIMIT_ON_BATCH_SIZE + 1);
    var exception =
      assertThrows(IllegalArgumentException.class, () -> client.sendMessageBatch(request));
    assertThat(exception.getMessage()).contains(BATCH_SIZE_ERROR);
  }

  private static ReceiveMessageRequest createReceiveMessageRequest(
    SendMessageRequest writeRequest) {
    return ReceiveMessageRequest.builder()
      .queueUrl(writeRequest.queueUrl())
      .build();
  }

  private static Consumer<MessageAttribute> assertThatAttributeValuesAreEquivalent(
    Entry<String, MessageAttributeValue> expectedAttribute) {
    return messageAttribute -> assertThat(messageAttribute.getStringValue()).isEqualTo(
      expectedAttribute.getValue().stringValue());
  }

  private static SendMessageRequest validMessage() {
    return SendMessageRequest.builder()
      .queueUrl(randomUri().toString())
      .messageBody(randomString())
      .messageAttributes(randomMessageAttributes())
      .build();
  }

  private static Map<String, MessageAttributeValue> randomMessageAttributes() {
    return Map.of(randomString(), randomMessageAttributeValue());
  }

  private static MessageAttributeValue randomMessageAttributeValue() {
    return MessageAttributeValue.builder()
      .dataType(randomString())
      .stringValue(randomString())
      .build();
  }

  private static SendMessageBatchRequest invalidMessageBatch() {
    return SendMessageBatchRequest.builder()
      .entries(List.of(randomEntry()))
      .build();
  }

  private static SendMessageBatchRequestEntry randomEntry() {
    return SendMessageBatchRequestEntry.builder()
      .id(randomString())
      .messageBody(randomString())
      .build();
  }

  private List<SendMessageRequest> convertBatchEntriesToSingleRequests(
    SendMessageBatchRequest request) {
    return request.entries().stream()
      .map(entry -> toSendMessageRequest(entry, request)).toList();
  }

  private SendMessageBatchRequest sampleBatchRequest(int batchSize) {
    var batch = createBatch(batchSize);
    return SendMessageBatchRequest.builder()
      .queueUrl(randomUri().toString())
      .entries(batch)
      .build();
  }

  private List<SendMessageBatchRequestEntry> createBatch(int batchSize) {
    return IntStream.range(0, batchSize)
      .boxed().map(ignored -> randomEntry())
      .toList();
  }

  private SendMessageBatchRequest sampleBatchRequest() {
    return SendMessageBatchRequest.builder()
      .queueUrl(randomUri().toString())
      .entries(List.of(randomEntry(), randomEntry()))
      .build();
  }

  private SendMessageRequest toSendMessageRequest(SendMessageBatchRequestEntry entry,
    SendMessageBatchRequest batchRequest) {
    return SendMessageRequest.builder()
      .queueUrl(batchRequest.queueUrl())
      .messageBody(entry.messageBody())
      .build();
  }

  private SendMessageRequest invalidMessage() {
    return SendMessageRequest.builder()
      .messageBody(randomString())
      .build();
  }

}