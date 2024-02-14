package com.github.awsjavakit.stepfunctions.lambda;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomInteger;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static com.gtihub.awsjavakit.attempt.Try.attempt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.github.awsjavakit.misc.SingletonCollector;
import com.github.awsjavakit.misc.ioutils.IoUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class StepFunctionHandlerTest {

  public static final ObjectMapper JSON = new ObjectMapper();
  public static final Context EMPTY_CONTEXT = null;
  public static final String MISSING_FIELD_NAME = "someString";
  private ByteArrayOutputStream outputStream;

  public static Stream<BaseType> polymorphicProvider() {
    return Stream.of(new SubTypeA(), new SubTypeB());
  }

  @BeforeEach
  public void init() {
    this.outputStream = new ByteArrayOutputStream();
  }

  @ParameterizedTest
  @MethodSource("polymorphicProvider")
  void shouldAllowParsingOfPolymorphicTypes(BaseType input) throws IOException {
    var handler = new GenericHandler<>(BaseType.class, JSON);
    var event = createEvent(input, JSON);
    handler.handleRequest(event, outputStream, EMPTY_CONTEXT);
    var output = fromJson(outputStream.toString(), BaseType.class);
    assertThat(output).isEqualTo(input);

  }

  @Test
  void shouldAcceptSpecifiedInputType() throws IOException {
    var handler = new EchoHandler(JSON);
    var input = new SomeInputClass(randomString(), randomInteger());
    var inputStream = createEvent(input, JSON);

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
    var event = createEvent(input, JSON);
    handler.handleRequest(event, outputStream, EMPTY_CONTEXT);
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

  @SuppressWarnings("resource")
  @Test
  void shouldRevealJsonParsingErrors() {
    var mapper = mapperFailingOnMissingRequiredProperties();

    var input = new SomeInputClass(null, randomInteger());
    var handler = new EchoHandler(mapper);
    var event = createEvent(input, mapper);
    var exception = assertThrows(RuntimeException.class,
      () -> handler.handleRequest(event, outputStream, EMPTY_CONTEXT));
    assertThat(exception.getCause().getMessage()).contains(MISSING_FIELD_NAME);
  }

  private static JsonMapper mapperFailingOnMissingRequiredProperties() {
    return JsonMapper.builder()
      .enable(FAIL_ON_MISSING_CREATOR_PROPERTIES)
      .serializationInclusion(Include.NON_ABSENT)
      .build();
  }

  private <I> I fromJson(String json, Class<I> inputClass) {
    return attempt(() -> JSON.readValue(json, inputClass)).orElseThrow();
  }

  private <I> InputStream createEvent(I input, ObjectMapper mapper) {
    return IoUtils.stringToStream(toJson(input, mapper));
  }

  private <I> String toJson(I input, ObjectMapper objectMapper) {
    return attempt(() -> objectMapper.writeValueAsString(input)).orElseThrow();
  }

  @JsonSubTypes(value = {
    @JsonSubTypes.Type(value = SubTypeA.class, name = SubTypeA.TYPE),
    @JsonSubTypes.Type(value = SubTypeB.class, name = SubTypeB.TYPE)
  })
  @JsonTypeInfo(use = Id.NAME, include = As.EXISTING_PROPERTY, property = "type")
  public interface BaseType {

    @JsonProperty(value = "type", access = Access.READ_ONLY)
    String getType();

  }

  @JsonTypeInfo(use = Id.NAME, include = As.EXISTING_PROPERTY, property = "type")
  @JsonTypeName(SubTypeA.TYPE)
  public record SubTypeA() implements BaseType {

    public static final String TYPE = "SubTypeA";

    @Override
    public String getType() {
      return TYPE;
    }

  }

  @JsonTypeInfo(use = Id.NAME, include = As.EXISTING_PROPERTY, property = "type")
  @JsonTypeName(SubTypeA.TYPE)
  public record SubTypeB() implements BaseType {

    public static final String TYPE = "SubTypeB";

    @Override
    public String getType() {
      return TYPE;
    }

  }
}

