package com.github.awsjavakit.dlq;

import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomJson;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static com.gtihub.awsjavakit.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.awsjavakit.testingutils.aws.FakeFirehoseClient;
import com.gtihub.awsjavakit.attempt.Try;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.services.firehose.FirehoseClient;

class DlqHandlerTest {

  public static final String DLQ_IS_SOURCE = "aws:sqs";
  public static final Context EMPTY_CONTEXT = null;
  public static final String SOME_FIREHOSE = "someFirehose";
  private static final String DLQ_ARN = randomString();
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private FakeFirehoseClient firehoseClient;
  private SampleDlqHandler handler;

  public static Stream<Arguments> eventProvider() {
    return IntStream.of(1, 2, 10, 245)
      .boxed()
      .map(numberOfMessages -> Arguments.of(
        Named.of(numberOfMessages + " messages", randomJsons(numberOfMessages))));
  }

  @BeforeEach
  public void init() {
    this.firehoseClient = new FakeFirehoseClient();
    this.handler = new SampleDlqHandler(firehoseClient, SOME_FIREHOSE);
  }

  @ParameterizedTest
  @MethodSource("eventProvider")
  void shouldPushFailedEventsToFirehoseForAutomaticallyStoringThemInS3AndOrganizeThemByTime(
    Set<JsonNode> failedEvents) {

    var dlqEvent = createDlqEventContainingFailedEvents(failedEvents);
    handler.handleRequest(dlqEvent, EMPTY_CONTEXT);
    var pushedRecords = firehoseClient.extractPushedContent(this::parseJsonString)
      .collect(Collectors.toSet());

    assertThat(failedEvents, is(equalTo(pushedRecords)));
  }

  private static Set<JsonNode> randomJsons(Integer numberOfMessages) {
    return IntStream.range(0, numberOfMessages).boxed()
      .map(ignored -> randomJson())
      .map(attempt(OBJECT_MAPPER::readTree))
      .map(Try::orElseThrow)
      .collect(Collectors.toSet());
  }

  private static SQSEvent createDlqEventContainingFailedEvents(Set<JsonNode> failedEvents) {
    var eventMessages =
      failedEvents.stream().map(DlqHandlerTest::createEventMessage).collect(Collectors.toList());
    var event = new SQSEvent();
    event.setRecords(eventMessages);
    return event;
  }

  private static SQSMessage createEventMessage(JsonNode failedEvent) {
    var sqsMessage = new SQSMessage();
    sqsMessage.setBody(attempt(() -> OBJECT_MAPPER.writeValueAsString(failedEvent)).orElseThrow());
    sqsMessage.setEventSource(DLQ_IS_SOURCE);
    sqsMessage.setEventSourceArn(DLQ_ARN);
    sqsMessage.setMd5OfBody(randomString());
    sqsMessage.setMessageId(randomString());
    return sqsMessage;
  }

  private JsonNode parseJsonString(String jsonString) {
    return attempt(() -> OBJECT_MAPPER.readTree(jsonString)).orElseThrow();
  }

  private static class SampleDlqHandler extends DlqHandler {

    public SampleDlqHandler(FirehoseClient firehoseClient, String deliveryStreamName) {
      super(new PushToFirehoseService(firehoseClient, deliveryStreamName));
    }
  }
}