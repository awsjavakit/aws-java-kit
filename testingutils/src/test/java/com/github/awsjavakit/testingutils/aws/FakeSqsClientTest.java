package com.github.awsjavakit.testingutils.aws;

import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomUri;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
    var message = validMessage();
    client.sendMessage(message);
    assertThat(client.getMessages()).containsExactly(message);
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
    var actualMessages = client.getMessages();
    assertThat(actualMessages)
      .containsExactlyInAnyOrder(expectedMessages.toArray(SendMessageRequest[]::new));
  }

  @Test
  void shouldReturnSomeServiceName() {
    assertThat(client.serviceName()).isNotNull();
  }

  @Test
  void shouldNotThrowExceptionWhenClosing() {
    assertDoesNotThrow(() -> client.close());
  }

  private static SendMessageRequest validMessage() {
    return SendMessageRequest.builder()
      .queueUrl(randomUri().toString())
      .messageBody(randomString())
      .build();
  }

  private SendMessageBatchRequest invalidMessageBatch() {
    return SendMessageBatchRequest.builder()
      .entries(List.of(randomEntry()))
      .build();
  }

  private List<SendMessageRequest> convertBatchEntriesToSingleRequests(
    SendMessageBatchRequest request) {
    return request.entries().stream()
      .map(entry -> toSendMessageRequest(entry, request)).toList();
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

  private SendMessageBatchRequestEntry randomEntry() {
    return SendMessageBatchRequestEntry.builder()
      .id(randomString())
      .messageBody(randomString())
      .build();
  }

  private SendMessageRequest invalidMessage() {
    return SendMessageRequest.builder()
      .messageBody(randomString())
      .build();
  }

}