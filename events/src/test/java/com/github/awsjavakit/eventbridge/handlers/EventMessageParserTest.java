package com.github.awsjavakit.eventbridge.handlers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.awsjavakit.eventbridge.models.AwsEventBridgeEvent;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class EventMessageParserTest {

  private static final ObjectMapper JSON = new ObjectMapper();

  @Test
  void parseThrowsRuntimeExceptionWhenParsingFails() {
    String invalidJson = "invalidJson";
    EventParser<SampleEventDetail> eventParser = new EventParser<>(invalidJson, JSON);
    Executable action = () -> eventParser.parse(SampleEventDetail.class);
    RuntimeException exception = assertThrows(RuntimeException.class, action);
    assertThat(exception.getCause(), is(instanceOf(JsonParseException.class)));
  }

  @Test
  void parseParsesCorrectlyNestedGenericTypes() throws JsonProcessingException {
    OuterClass<MiddleClass<InnerClass<String>>> expectedDetail = createdNestedGenericsObject();
    AwsEventBridgeEvent<OuterClass<MiddleClass<InnerClass<String>>>> event = createEventWithDetail(
      expectedDetail);

    String eventJson = JSON.writeValueAsString(event);

    EventParser<OuterClass<MiddleClass<InnerClass<String>>>> parser = new EventParser<>(eventJson,
      JSON);

    AwsEventBridgeEvent<OuterClass<MiddleClass<InnerClass<String>>>> eventWithNestedTypes =
      parser.parse(OuterClass.class, MiddleClass.class, InnerClass.class, String.class);

    assertThat(eventWithNestedTypes.getDetail(), is(equalTo(expectedDetail)));
  }

  private AwsEventBridgeEvent<OuterClass<MiddleClass<InnerClass<String>>>> createEventWithDetail(
    OuterClass<MiddleClass<InnerClass<String>>> expectedDetail) {
    AwsEventBridgeEvent<OuterClass<MiddleClass<InnerClass<String>>>> event = new AwsEventBridgeEvent<>();
    event.setDetail(expectedDetail);
    event.setAccount("someAccount");
    event.setId("SomeId");
    return event;
  }

  private OuterClass<MiddleClass<InnerClass<String>>> createdNestedGenericsObject() {
    InnerClass<String> bottom = new InnerClass<>();
    bottom.setFieldC("Hello");
    MiddleClass<InnerClass<String>> middle = new MiddleClass<>();
    middle.setFieldB(bottom);
    OuterClass<MiddleClass<InnerClass<String>>> top = new OuterClass<>();
    top.setFieldA(middle);
    return top;
  }

  private static final class OuterClass<InputType> {

    private InputType fieldA;

    public InputType getFieldA() {
      return fieldA;
    }

    public void setFieldA(InputType fieldA) {
      this.fieldA = fieldA;
    }

    @Override
    public int hashCode() {
      return Objects.hash(getFieldA());
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      OuterClass<?> outerClass = (OuterClass<?>) o;
      return Objects.equals(getFieldA(), outerClass.getFieldA());
    }
  }

  private static final  class MiddleClass<InputType> {

    private InputType fieldB;

    public InputType getFieldB() {
      return fieldB;
    }

    public void setFieldB(InputType fieldB) {
      this.fieldB = fieldB;
    }

    @Override
    public int hashCode() {
      return Objects.hash(getFieldB());
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      MiddleClass<?> middleClass = (MiddleClass<?>) o;
      return Objects.equals(getFieldB(), middleClass.getFieldB());
    }
  }

  private static final class InnerClass<InputType> {

    private InputType fieldC;

    public InputType getFieldC() {
      return fieldC;
    }

    public void setFieldC(InputType fieldC) {
      this.fieldC = fieldC;
    }

    @Override
    public int hashCode() {
      return Objects.hash(getFieldC());
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      InnerClass<?> innerClass = (InnerClass<?>) o;
      return Objects.equals(getFieldC(), innerClass.getFieldC());
    }
  }
}