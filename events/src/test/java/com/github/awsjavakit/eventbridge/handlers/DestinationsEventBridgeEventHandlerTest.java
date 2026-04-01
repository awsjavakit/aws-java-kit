package com.github.awsjavakit.eventbridge.handlers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.annotation.JsonInclude.Value;
import tools.jackson.core.JsonPointer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.json.JsonMapper.Builder;
import tools.jackson.databind.node.ObjectNode;
import com.github.awsjavakit.eventbridge.models.AwsEventBridgeDetail;
import com.github.awsjavakit.eventbridge.models.AwsEventBridgeEvent;
import com.github.awsjavakit.misc.ioutils.IoUtils;
import com.github.awsjavakit.testingutils.aws.FakeContext;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DestinationsEventBridgeEventHandlerTest extends AbstractEventHandlerTest {

  private static final String VALID_AWS_EVENT_BRIDGE_EVENT = IoUtils.stringFromResources(
    Path.of("validAwsEventBridgeEvent.json"));
  private static final ObjectMapper JSON = jsonBuilder().build();
  private static final ObjectMapper JSON_INCLUDING_EMPTY_VALUES = jsonBuilder()
    .changeDefaultPropertyInclusion(ignored -> Value.ALL_ALWAYS)
    .build();
  private static final ObjectMapper JSON_OMITTING_EMPTY_VALUES = jsonBuilder()
    .changeDefaultPropertyInclusion(ignored -> Value.ALL_NON_EMPTY)
    .build();
  private static final JsonPointer RESPONSE_PAYLOAD_POINTER = JsonPointer.compile(
    "/detail/responsePayload");

  private ByteArrayOutputStream outputStream;
  private Context context;

  @BeforeEach
  public void init() {
    this.outputStream = new ByteArrayOutputStream();
    this.context = new FakeContext();
  }

  @Test
  public void handleRequestAcceptsValidEvent()  {
    DestinationsHandlerTestClass handler = new DestinationsHandlerTestClass(JSON);
    InputStream requestInput = IoUtils.stringToStream(VALID_AWS_EVENT_BRIDGE_EVENT);
    handler.handleRequest(requestInput, outputStream, context);
    SampleEventDetail expectedInput = extractInputFromValidAwsEventBridgeEvent(
      VALID_AWS_EVENT_BRIDGE_EVENT);
    assertThat(handler.inputBuffer.get(), is(equalTo(expectedInput)));
  }

  @Test
  public void handleRequestSerializesObjectsWithoutOmittingEmptyValuesWhenSuchMapperHasBeenSet() {
    final InputStream input = IoUtils.stringToStream(VALID_AWS_EVENT_BRIDGE_EVENT);
    DestinationsHandlerTestClass handler = new DestinationsHandlerTestClass(
      JSON_INCLUDING_EMPTY_VALUES);
    handler.handleRequest(input, outputStream, context);
    ObjectNode outputObject = (ObjectNode) JSON_INCLUDING_EMPTY_VALUES.readTree(
      outputStream.toString());
    assertThatJsonObjectContainsEmptyFields(outputObject);
  }

  @Test
  public void handleRequestSerializesObjectsOmittingEmptyValuesWhenSuchMapperHasBeenSet() {
    final InputStream input = IoUtils.stringToStream(VALID_AWS_EVENT_BRIDGE_EVENT);

    DestinationsHandlerTestClass handler = new DestinationsHandlerTestClass(
      JSON_OMITTING_EMPTY_VALUES);
    handler.handleRequest(input, outputStream, context);
    ObjectNode outputObject = (ObjectNode) JSON_OMITTING_EMPTY_VALUES.readTree(
      outputStream.toString());
    assertThatJsonNodeDoesNotContainEmptyFields(outputObject);
  }

  private static Builder jsonBuilder() {
    return JsonMapper.builder();
  }

  private SampleEventDetail extractInputFromValidAwsEventBridgeEvent(String awsEventBridgeEvent) {
    JsonNode inputNode = extractResponseObjectFromAwsEventBridgeEvent(awsEventBridgeEvent);
    return JSON.convertValue(inputNode, SampleEventDetail.class);
  }

  private ObjectNode extractResponseObjectFromAwsEventBridgeEvent(String awsEventBridgeEvent) {
    ObjectNode tree = (ObjectNode) JSON.readTree(awsEventBridgeEvent);
    return (ObjectNode) tree.at(RESPONSE_PAYLOAD_POINTER);
  }

  private static class DestinationsHandlerTestClass
    extends DestinationsEventBridgeEventHandler<SampleEventDetail, SampleEventDetail> {

    private final AtomicReference<SampleEventDetail> inputBuffer = new AtomicReference<>();
    private final AtomicReference<AwsEventBridgeEvent<AwsEventBridgeDetail<SampleEventDetail>>> eventBuffer =
      new AtomicReference<>();

    protected DestinationsHandlerTestClass(ObjectMapper objectMapper) {
      super(SampleEventDetail.class, objectMapper);
    }

    @Override
    protected SampleEventDetail processInputPayload(
      SampleEventDetail input,
      AwsEventBridgeEvent<AwsEventBridgeDetail<SampleEventDetail>> event,
      Context context
    ) {
      this.inputBuffer.set(input);
      this.eventBuffer.set(event);
      return SampleEventDetail.eventWithEmptyFields();
    }
  }
}