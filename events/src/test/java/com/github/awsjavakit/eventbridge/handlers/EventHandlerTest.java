package com.github.awsjavakit.eventbridge.handlers;

import static com.github.awsjavakit.jsonconfig.JsonConfig.JSON;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.text.IsEmptyString.emptyString;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.awsjavakit.eventbridge.models.AwsEventBridgeEvent;
import com.github.awsjavakit.logutils.LogUtils;
import com.github.awsjavakit.logutils.TestAppender;
import com.github.awsjavakit.misc.ioutils.IoUtils;
import com.github.awsjavakit.testingutils.aws.FakeContext;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class EventHandlerTest extends AbstractEventHandlerTest {

  public static final String AWS_EVENT_BRIDGE_EVENT =
    IoUtils.stringFromResources(Path.of("validEventBridgeEvent.json"));

  public static final String EXCEPTION_MESSAGE = "EXCEPTION_MESSAGE";
  private ByteArrayOutputStream outputStream;
  private Context context;

  @BeforeEach
  public void init() {
    this.outputStream = new ByteArrayOutputStream();
    this.context = new FakeContext();
  }

  @Test
  public void handleRequestAcceptsValidEvent() throws JsonProcessingException {
    EventHandlerTestClass handler = new EventHandlerTestClass();
    final InputStream input = sampleInputStream(AWS_EVENT_BRIDGE_EVENT);
    handler.handleRequest(input, outputStream, context);
    AwsEventBridgeEvent<SampleEventDetail> expectedEvent = parseEventFromSampleEventString();
    AwsEventBridgeEvent<SampleEventDetail> actualEvent = handler.eventBuffer.get();
    assertThat(actualEvent, is(equalTo(expectedEvent)));
  }

  @Test
  public void handleRequestLogsErrorWhenExceptionIsThrown() {
    TestAppender appender = LogUtils.getTestingAppender(EventHandler.class);
    var handler = new EventHandlerThrowingException();
    final InputStream input = sampleInputStream(AWS_EVENT_BRIDGE_EVENT);
    Executable action = () -> handler.handleRequest(input, outputStream, context);
    assertThrows(RuntimeException.class, action);
    assertThat(appender.getMessages(), containsString(EXCEPTION_MESSAGE));
  }

  @Test
  public void handleRequestReThrowsExceptionWhenExceptionIsThrown() {
    var handler = new EventHandlerThrowingException();
    Executable action = () -> handler.handleRequest(sampleInputStream(AWS_EVENT_BRIDGE_EVENT),
      outputStream,
      context);
    RuntimeException exception = assertThrows(RuntimeException.class, action);
    assertThat(exception.getMessage(), is(equalTo(EXCEPTION_MESSAGE)));
  }

  @Test
  public void handleRequestSerializesObjectsWithoutOmittingEmptyValuesWhenSuchMapperHasBeenSet()
    throws JsonProcessingException {
    final InputStream input = sampleInputStream(AWS_EVENT_BRIDGE_EVENT);
    EventHandlerTestClass handler = new EventHandlerTestClass(JSON);
    ObjectNode objectNode = sendEventAndCollectOutputAsJsonObject(input, handler);
    assertThatJsonObjectContainsEmptyFields(objectNode);
  }

  private ObjectNode sendEventAndCollectOutputAsJsonObject(InputStream input,
    EventHandlerTestClass handler)
    throws JsonProcessingException {
    handler.handleRequest(input, outputStream, context);
    String output = outputStream.toString();
    assertThat(output, is(not(emptyString())));
    return (ObjectNode) JSON.readTree(output);
  }

  private InputStream sampleInputStream(String filename) {
    return IoUtils.stringToStream(filename);
  }

  private AwsEventBridgeEvent<SampleEventDetail> parseEventFromSampleEventString()
    throws JsonProcessingException {
    JavaType javatype = JSON.getTypeFactory()
      .constructParametricType(AwsEventBridgeEvent.class, SampleEventDetail.class);
    return JSON.readValue(AWS_EVENT_BRIDGE_EVENT, javatype);
  }

  private static class EventHandlerTestClass extends
    EventHandler<SampleEventDetail, SampleEventDetail> {

    private final AtomicReference<AwsEventBridgeEvent<SampleEventDetail>> eventBuffer = new AtomicReference<>();
    private final AtomicReference<SampleEventDetail> inputBuffer = new AtomicReference<>();

    protected EventHandlerTestClass(ObjectMapper objectMapper) {
      super(SampleEventDetail.class, objectMapper);
    }

    protected EventHandlerTestClass() {
      super(SampleEventDetail.class, JSON);
    }

    @Override
    protected SampleEventDetail processInput(SampleEventDetail input,
      AwsEventBridgeEvent<SampleEventDetail> event,
      Context context) {
      eventBuffer.set(event);
      inputBuffer.set(input);

      return SampleEventDetail.eventWithEmptyFields();
    }
  }

  private static class EventHandlerThrowingException extends EventHandler<SampleEventDetail, Void> {

    protected EventHandlerThrowingException() {
      super(SampleEventDetail.class, JSON);
    }

    @Override
    protected Void processInput(SampleEventDetail input,
      AwsEventBridgeEvent<SampleEventDetail> event,
      Context context) {
      throw new RuntimeException(EXCEPTION_MESSAGE);
    }
  }
}