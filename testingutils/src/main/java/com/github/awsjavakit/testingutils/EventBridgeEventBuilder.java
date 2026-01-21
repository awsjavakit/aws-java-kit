package com.github.awsjavakit.testingutils;

import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomElement;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomInstant;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.awsjavakit.eventbridge.models.AwsEventBridgeDetail;
import com.github.awsjavakit.eventbridge.models.AwsEventBridgeEvent;
import com.github.awsjavakit.misc.JacocoGenerated;
import com.github.awsjavakit.misc.ioutils.IoUtils;
import com.github.awsjavakit.attempt.Try;
import java.io.InputStream;
import java.util.List;
import software.amazon.awssdk.regions.Region;

@JacocoGenerated
@SuppressWarnings("FeatureEnvy")
public final class EventBridgeEventBuilder {

  private static final ObjectMapper JSON = new ObjectMapper();
  public static final ObjectNode EMPTY_OBJECT = JSON.createObjectNode();

  private EventBridgeEventBuilder() {

  }

  @JacocoGenerated
  public static <T> InputStream sampleLambdaDestinationsEvent(T eventBody,
    ObjectMapper objectMapper) {
    var detail = createDestinationsEventDetailBody(eventBody);
    var event = sampleEventObject(detail);
    return Try.of(event)
      .map(objectMapper::writeValueAsString)
      .map(IoUtils::stringToStream)
      .orElseThrow();
  }

  @JacocoGenerated
  public static <T> InputStream sampleEvent(T detail, ObjectMapper objectMapper) {
    return Try.of(sampleEventObject(detail))
      .map(objectMapper::writeValueAsString)
      .map(IoUtils::stringToStream)
      .orElseThrow();
  }

  @JacocoGenerated
  public static <T> AwsEventBridgeEvent<T> sampleEventObject(T detail) {
    var event = new AwsEventBridgeEvent<T>();
    event.setDetail(detail);
    event.setVersion(randomString());
    event.setResources(List.of(randomString()));
    event.setId(randomString());
    event.setRegion(randomElement(Region.regions()));
    event.setTime(randomInstant());
    event.setSource(randomString());
    event.setAccount(randomString());
    return event;
  }

  @JacocoGenerated
  private static <T> AwsEventBridgeDetail<T> createDestinationsEventDetailBody(T eventBody) {
    return AwsEventBridgeDetail.<T>newBuilder()
      .withRequestPayload(EMPTY_OBJECT)
      .withTimestamp(randomInstant().toString())
      .withVersion(randomString())
      .withResponsePayload(eventBody)
      .build();
  }
}

