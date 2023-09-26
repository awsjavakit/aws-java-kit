package com.github.awsjavakit.eventbridge.handlers;

import static com.github.awsjavakit.misc.exceptions.ExceptionUtils.stackTraceInSingleLine;
import static com.gtihub.awsjavakit.attempt.Try.attempt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.awsjavakit.eventbridge.models.AwsEventBridgeEvent;
import com.gtihub.awsjavakit.attempt.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventParser<I> {

    public static final String ERROR_PARSING_INPUT = "Could not parse input: ";
    public static final int SKIP_BOTTOM_TYPE = 2;
    public static final String RAWTYPES = "rawtypes";
    private static final Logger logger = LoggerFactory.getLogger(EventParser.class);
    private final String input;
    private final ObjectMapper objectMapper;

    public EventParser(String input, ObjectMapper objectMapper) {
        this.input = input;
        this.objectMapper = objectMapper;
    }

    public AwsEventBridgeEvent<I> parse(Class<I> iclass) {
        return attempt(() -> parseJson(iclass)).orElseThrow(this::handleParsingError);
    }

    /**
     * Given the nested parameter classes ClassA,ClassB,ClassC,...,ClassZ, this method returns the an
     * AwsEventBridgeEvent where the detail object in of type {@literal ClassA<ClassB<ClassC<...ClassZ>>..>}.
     *
     * @param nestedParameterClasses the classes of the nested generic type of the detail object.
     * @return an AwsEventBridgeEvent with a nested generic type as detail object.
     */
    /*
      Using raw types because of Java's type erasure for nested generic classes.
      I.e. one cannot retrieve the java.lang.Class instance for the type ClassA<ClassB>.
    */
    @SuppressWarnings(RAWTYPES)
    public AwsEventBridgeEvent parse(Class... nestedParameterClasses) {
        return attempt(() -> parseJson(nestedParameterClasses)).orElseThrow(this::handleParsingError);
    }

    private AwsEventBridgeEvent<I> parseJson(Class<I> iclass) throws JsonProcessingException {
        JavaType javaType =
            objectMapper.getTypeFactory().constructParametricType(AwsEventBridgeEvent.class, iclass);
        return objectMapper.readValue(input, javaType);
    }

    private AwsEventBridgeEvent<?> parseJson(Class<?>... nestedClasses)
        throws JsonProcessingException {
        JavaType nestedJavaTypes = nestedGenericTypesToJavaType(nestedClasses);
        JavaType eventBridgeJavaType = constructAwsEventBridgeDataTypeWithAllNestedTypes(nestedJavaTypes);
        return objectMapper.readValue(input, eventBridgeJavaType);
    }

    private <OutputType> RuntimeException handleParsingError(Failure<OutputType> fail) {
        logger.error(ERROR_PARSING_INPUT + input);
        logger.error(stackTraceInSingleLine(fail.getException()));
        return new RuntimeException(fail.getException());
    }

    /*
     * Given an array of {@link Class} generic classes ClassA,ClassB,ClassC,...ClassZ,
     * it creates a {@link JavaType} for the object  ClassA<ClassB<ClassC...<ClassZ>>>>
     */
    @SuppressWarnings(RAWTYPES)
    private JavaType nestedGenericTypesToJavaType(Class... classes) {
        //Variables not inlined for readability purposes.
        JavaType mostRecentType = objectMapper.getTypeFactory().constructType(innermostType(classes));
        for (int index = classes.length - SKIP_BOTTOM_TYPE; index >= 0; index--) {
            Class<?> currentClass = classes[index];
            mostRecentType = createGenericClassContainingAllPreviousTypes(mostRecentType, currentClass);
        }
        return mostRecentType;
    }

    private JavaType createGenericClassContainingAllPreviousTypes(JavaType mostRecentType, Class<?> currentClass) {
        return objectMapper.getTypeFactory().constructParametricType(currentClass, mostRecentType);
    }

    @SafeVarargs
    private <T> T innermostType(T... classes) {
        return classes[classes.length - 1];
    }

    private JavaType constructAwsEventBridgeDataTypeWithAllNestedTypes(JavaType mostRecentType) {
        return objectMapper.getTypeFactory()
            .constructParametricType(AwsEventBridgeEvent.class, mostRecentType);
    }
}
