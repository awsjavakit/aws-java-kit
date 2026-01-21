package com.github.awsjavakit.attempt;

import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static com.github.awsjavakit.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class TryTest {

  public static final int PROVOKE_ERROR = 0;
  private static final String SOME_STRING = "SomeString";
  private static final String EXCEPTION_MESSAGE = "ExceptionMessage";
  private static final ArithmeticException SAMPLE_UNCHECKED_EXCEPTION = new ArithmeticException(
    EXCEPTION_MESSAGE);
  private static final Map<Integer, String> NUMBERS;

  static {
    NUMBERS = new HashMap<>();
    NUMBERS.put(1, "one");
    NUMBERS.put(2, "two");
    NUMBERS.put(3, "three");
    NUMBERS.put(4, "four");
    NUMBERS.put(5, "five");
    NUMBERS.put(6, "six");
    NUMBERS.put(7, "seven");
    NUMBERS.put(8, "eight");
    NUMBERS.put(9, "nine");
    NUMBERS.put(0, "zero");
  }

  @Test
  public void shouldReturnSuccessTypeWhenSuccessful() {
    Try<Integer> attempt = attempt(() -> divide(6, 3));
    assertThat(attempt.isSuccess(), is(equalTo(true)));
  }

  @Test
  public void shouldReturnFailureContainingThrownExceptionWhenThrowingException() {
    Try<Integer> attempt = attempt(() -> divide(6, 0));
    assertThat(attempt.isSuccess(), is(equalTo(false)));
    assertThat(attempt.isFailure(), is(equalTo(true)));
    Exception e = attempt.getException();
    assertTrue(e instanceof ArithmeticException);
  }

  @Test
  public void shouldReturnMappableResultWhenMapping() {
    Try<String> attempt = attempt(() -> divide(6, 3))
      .map(NUMBERS::get)
      .map(String::toUpperCase);
    assertThat(attempt.get(), is(equalTo("TWO")));
  }

  @Test
  public void mapReportsTheFirstException() {
    Try<String> attempt = attempt(() -> divide(6, 0))
      .map(NUMBERS::get)
      .map(String::toUpperCase);

    assertThat(attempt.isFailure(), is(true));
    assertThat(attempt.getException().getClass(), is(equalTo(ArithmeticException.class)));
  }

  @Test
  public void flatMapFlattensNestedTry() {
    Function<String, Try<String>> fun1 = attempt(String::toUpperCase);
    String input = "input";
    Try<String> nestedTry = attempt(() -> input).flatMap(fun1::apply);
    assertThat(nestedTry.get(), is(equalTo(input.toUpperCase(Locale.getDefault()))));
  }

  @Test
  public void attemptReturnsFailureWhenCheckedExceptionIsThrown() {
    Optional<Exception> exception = Stream.of(SOME_STRING)
      .map(attempt(input -> throwCheckedException()))
      .filter(Try::isFailure)
      .map(Try::getException)
      .findFirst();
    assertThat(exception.isPresent(), is(true));
    assertThat(exception.get().getClass(), is(equalTo(IOException.class)));
  }

  @Test
  public void attemptReturnsFailureWhenNonCheckedExceptionIsThrown() {
    Try<String> effort = attempt(this::throwUnCheckedException);
    assertThat(effort.isFailure(), is(true));
    assertThat(effort.getException().getClass(),
      is(equalTo(SAMPLE_UNCHECKED_EXCEPTION.getClass())));
  }

  @Test
  public void attemptReturnsFailureAtTheEndOfStreamProcessingWhenUncheckedExceptionIsThrown() {
    Optional<Exception> exception = Stream.of(SOME_STRING)
      .map(attempt(input -> throwUnCheckedException()))
      .filter(Try::isFailure)
      .map(Try::getException)
      .findFirst();
    assertThat(exception.isPresent(), is(true));
    assertThat(exception.get().getClass(), is(equalTo(SAMPLE_UNCHECKED_EXCEPTION.getClass())));
  }

  @Test
  public void orElseThrowsSpecifiedExceptionWhenUncheckedExceptionIsThrown() {
    Executable action = () -> attempt(this::throwUnCheckedException)
      .orElseThrow(fail -> new TestException());
    assertThrows(TestException.class, action);
  }

  @Test
  public void shouldThrowExceptionDiscardingTheCauseWhenThereIsFailureAndSuppliedWithExceptionInsteadOfFunction() {
    var expectedMessage = randomString();
    Executable action = () -> attempt(this::throwCheckedException)
      .orElseThrow(new RuntimeException(expectedMessage));
    var actualException = assertThrows(RuntimeException.class, action);
    assertThat(actualException.getMessage(), is(equalTo(expectedMessage)));
    assertThat(actualException.getCause(), is(nullValue()));
  }

  @Test
  public void shouldReturnValueWhenSuccessfulAndOrElseThrowsWithExceptionIsCalled() {
    var expectedMessage = randomString();
    var actualMessage = attempt(() -> expectedMessage)
      .orElseThrow(new RuntimeException(randomString()));

    assertThat(actualMessage, is(equalTo(expectedMessage)));
  }

  @Test
  public void failurePropagatesErrorThroughTheWholeChain() {
    int someInt = 2;
    Try<String> results = Stream.of(someInt)
      .map(NUMBERS::get)
      .map(String::toUpperCase)
      .map(attempt(ignored -> throwCheckedException()))
      .map(att -> att.map(s -> s.replaceAll("o", "cc")))
      .map(att -> att.map(String::trim))
      .filter(Try::isFailure)
      .findFirst().orElse(null);
    Exception exception = results.getException();
    assertThat(exception.getClass(), is(equalTo(IOException.class)));
    assertThat(exception.getMessage(), is(equalTo(EXCEPTION_MESSAGE)));
  }

  @Test
  @DisplayName("flatMap returns a failure with the first Exception")
  public void shouldPropagateTheFirstExceptionWhenFlatMapping() {
    Integer someInt = 2;
    Try<String> actual = Try.of(someInt)
      .map(Object::toString)
      .flatMap(ignored -> throwCheckedExceptionForFlatMap());

    assertTrue(actual.isFailure());
    assertThat(actual.getException().getMessage(), is(IsEqual.equalTo(EXCEPTION_MESSAGE)));
  }

  @Test
  void shouldReturnFistSuccessWhenOrIsCalled() {
    var firstSucceeds = attempt(() -> divide(1, 1))
      .or(() -> divide(2, 1))
      .or(() -> divide(3, 1))
      .orElseThrow();
    assertThat(firstSucceeds, is(equalTo(1)));

    var middleSucceeds = attempt(() -> divide(1, PROVOKE_ERROR))
      .or(() -> divide(2, 1))
      .or(() -> divide(3, 1))
      .orElseThrow();
    assertThat(middleSucceeds, is(equalTo(2)));

    var finalSucceeds = attempt(() -> divide(1, PROVOKE_ERROR))
      .or(() -> divide(2, PROVOKE_ERROR))
      .or(() -> divide(3, 1))
      .orElseThrow();
    assertThat(finalSucceeds, is(equalTo(3)));
  }

  private int divide(int x, int y) {
    return x / y;
  }

  private String throwCheckedException() throws IOException {
    throw new IOException(EXCEPTION_MESSAGE);
  }

  private Try<String> throwCheckedExceptionForFlatMap() throws IOException {
    throw new IOException(EXCEPTION_MESSAGE);
  }

  private String throwUnCheckedException() {
    throw SAMPLE_UNCHECKED_EXCEPTION;
  }
}
