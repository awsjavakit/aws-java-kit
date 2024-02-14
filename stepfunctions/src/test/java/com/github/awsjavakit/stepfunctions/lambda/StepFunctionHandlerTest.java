package com.github.awsjavakit.stepfunctions.lambda;

import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomInteger;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static com.gtihub.awsjavakit.attempt.Try.attempt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.awsjavakit.misc.SingletonCollector;
import com.github.awsjavakit.misc.ioutils.IoUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StepFunctionHandlerTest {

  public static final ObjectMapper JSON = new ObjectMapper();
  public static final Context EMPTY_CONTEXT = null;
  private ByteArrayOutputStream outputStream;

  @BeforeEach
  public void init() {
    this.outputStream = new ByteArrayOutputStream();
  }

  @Test
  void shouldAcceptSpecifiedInputType() throws IOException {
    var handler = new EchoHandler(JSON);
    var input = new SomeInputClass(randomString(), randomInteger());
    var inputStream = createEvent(input);

    handler.handleRequest(inputStream, outputStream, EMPTY_CONTEXT);
    var output = fromJson(outputStream.toString(), SomeInputClass.class);
    assertThat(output).isEqualTo(input);
  }

  @Test
  void shouldExpectDefinitionOfTransformingInputToOutput() {
    var transformationMethod =
      Arrays.stream(StepFunctionHandler.class.getDeclaredMethods())
        .filter(method -> "processInput".equals(method.getName()))
        .collect(SingletonCollector.collect());
    var isAbstract = Modifier.isAbstract(transformationMethod.getModifiers());
    assertThat(isAbstract)
      .withFailMessage(() -> "Expecting abstract transformation method with name 'processInput'")
      .isEqualTo(true);
  }

  @Test
  void shouldApplyTransformationMethodToReturnTheProducedOutput() throws IOException {
    var handler = new SampleHandler(JSON);
    var input = new SomeInputClass(randomString(), randomInteger());
    handler.handleRequest(createEvent(input), outputStream, EMPTY_CONTEXT);
    var actualOutput = fromJson(outputStream.toString(), SomeOutputClass.class);
    var expectedOutput = input.transform();
    assertThat(actualOutput).isEqualTo(expectedOutput);
  }

  @Test
  void shouldNotAllowTheTransformationMethodToThrowAnyCheckedExceptions() {
    var transformationMethod =
      Arrays.stream(StepFunctionHandler.class.getDeclaredMethods())
        .filter(method -> "processInput".equals(method.getName()))
        .collect(SingletonCollector.collect());
    var checkedExceptions = transformationMethod.getExceptionTypes();
    assertThat(checkedExceptions).isEmpty();
  }

  @Test
  void shouldNotFailOnParsingEmptyInputWhenEmptyInputIsAcceptable() {
    var handler = new StepFunctionHandler<Void, Void>(Void.class, JSON) {
      @Override
      public Void processInput(Void input, Context context) {
        return null;
      }
    };
    assertDoesNotThrow(
      () -> handler.handleRequest(InputStream.nullInputStream(), outputStream, EMPTY_CONTEXT));

  }

  @Test
  void shouldThrowRuntimeExceptionThrownByTransformationMethodAsIs() {
    var expectedMessage = randomString();
    var expectedException = new RuntimeException(expectedMessage);
    var handler = new StepFunctionHandler<Void, Void>(Void.class, JSON) {
      @Override
      public Void processInput(Void input, Context context) {
        throw expectedException;
      }
    };

    var actualException = assertThrows(RuntimeException.class,
      () -> handler.handleRequest(InputStream.nullInputStream(), outputStream, EMPTY_CONTEXT));

    assertThat(actualException).isSameAs(expectedException);

  }

  private <I> I fromJson(String json, Class<I> inputClass) {
    return attempt(() -> JSON.readValue(json, inputClass)).orElseThrow();
  }

  private InputStream createEvent(SomeInputClass input) {
    return IoUtils.stringToStream(toJson(input));
  }

  private String toJson(SomeInputClass input) {
    return attempt(() -> JSON.writeValueAsString(input)).orElseThrow();
  }

}