package com.github.awsjavakit.attempt;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gtihub.awsjavakit.attempt.Failure;
import com.gtihub.awsjavakit.attempt.Try;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class FailureTest {

  public static final String EXPECTED_EXCEPTION_MESSAGE = "Expected exception message";
  public static final String NESTED_EXCEPTION_MESSAGE = "Nested exception message";
  public static final String NOT_EXPECTED_MESSAGE = "NotExpectedMessage";
  private static final Integer DEFAULT_VALUE = 100;
  private static final Integer SAMPLE = 1;

  @Test
  @DisplayName("orElseThrow throws illegalStateException for null argument")
  public void orElseThrowsIllegalStateExceptionForNullArgument() {

    Executable action =
      () -> Try.of(SAMPLE)
        .map(i -> throwCheckedException(NOT_EXPECTED_MESSAGE))
        .orElseThrow((Function<Failure<Integer>, ? extends Exception>) null);
    IllegalStateException exception = assertThrows(IllegalStateException.class, action);
    assertThat(exception.getMessage(), is(equalTo(Failure.NULL_ACTION_MESSAGE)));
  }

  @Test
  @DisplayName("orElseThrow throws the specified exception when checked exception is thrown")
  public void orElseThrowsTheSpecifiedExceptionWhenCheckedExceptionIsThrown() {
    Executable action =
      () -> Try.of(SAMPLE)
        .map(i -> throwCheckedException(NESTED_EXCEPTION_MESSAGE))
        .orElseThrow(f -> new TestException(f.getException(), EXPECTED_EXCEPTION_MESSAGE));

    TestException exception = assertThrows(TestException.class, action);
    assertThat(exception.getMessage(), is(equalTo(EXPECTED_EXCEPTION_MESSAGE)));
  }

  @Test
  @DisplayName("orElseThrow throws the specified exception when unchecked exception is thrown")
  public void orElseThrowThrowsTheSpecifiedExceptionWhenUncheckedExceptionIsThrown() {
    Executable action =
      () -> Try.of(SAMPLE)
        .map(i -> throwUnCheckedException(NESTED_EXCEPTION_MESSAGE))
        .orElseThrow(f -> new TestException(f.getException(), EXPECTED_EXCEPTION_MESSAGE));

    TestException exception = assertThrows(TestException.class, action);
    assertThat(exception.getMessage(), is(equalTo(EXPECTED_EXCEPTION_MESSAGE)));
  }

  @Test
  @DisplayName("orElse returns the specified value")
  public void orElseReturnsTheSpecifiedValue() {

    Integer actual = Try.of(SAMPLE)
      .map(i -> throwCheckedException(NESTED_EXCEPTION_MESSAGE))
      .orElse(f -> DEFAULT_VALUE);

    assertThat(actual, is(equalTo(DEFAULT_VALUE)));
  }

  @Test
  @DisplayName("orElse throws exception when the final method throws an exception")
  public void orElseThrowsExceptionWhenTheFinalMethodThrowsAnException() {

    Executable action =
      () -> Try.of(SAMPLE)
        .map(i -> throwCheckedException(NESTED_EXCEPTION_MESSAGE))
        .orElse(f -> throwAnotherCheckedException());

    TestException exception = assertThrows(TestException.class, action);
    assertThat(exception.getMessage(), is(equalTo(EXPECTED_EXCEPTION_MESSAGE)));
  }

  @Test
  @DisplayName("orElse throws IllegalStateException when the input arg is null")
  public void orElseThrowsIllegalStateExceptionWhenTheInputArgumentIsNull() {

    Executable action =
      () -> Try.of(SAMPLE)
        .map(i -> throwCheckedException(NESTED_EXCEPTION_MESSAGE))
        .orElse(null);

    assertThrows(IllegalStateException.class, action);
  }

  @Test
  public void orElseThrowThrowsRuntimeExceptionWhenActionThrowsCheckedException() {
    String someString = "hello";
    Executable action = () -> Try.of(someString)
      .map(str -> throwCheckedException(EXPECTED_EXCEPTION_MESSAGE))
      .orElseThrow();

    RuntimeException exception = assertThrows(RuntimeException.class, action);
    assertThat(exception.getCause().getMessage(), is(equalTo(EXPECTED_EXCEPTION_MESSAGE)));
  }

  @Test
  public void orElseThrowThrowsOriginalExceptionWhenActionThrowsUncheckedException() {
    String someString = "hello";
    Executable action = () -> Try.of(someString)
      .map(str -> throwUnCheckedException(EXPECTED_EXCEPTION_MESSAGE))
      .orElseThrow();

    RuntimeException exception = assertThrows(RuntimeException.class, action);
    assertThat(exception.getMessage(), is(equalTo(EXPECTED_EXCEPTION_MESSAGE)));
  }

  @Test
  @DisplayName("flatMap returns a failure with the first Exception")
  public void flatMapReturnsAFailureWithTheFirstException() {
    Try<Integer> actual = Try.of(SAMPLE)
      .map(i -> throwCheckedException(EXPECTED_EXCEPTION_MESSAGE))
      .flatMap(this::anotherTry);

    assertTrue(actual.isFailure());
    assertThat(actual.getException().getMessage(),
      is(equalTo(EXPECTED_EXCEPTION_MESSAGE)));
  }

  @Test
  @DisplayName("get throws IllegalStateException")
  public void shouldThrowIllegalStateException() {
    Executable action = () -> Try.of(SAMPLE).map(i -> throwCheckedException(NOT_EXPECTED_MESSAGE))
      .get();
    assertThrows(IllegalStateException.class, action);
  }

  @Test
  @DisplayName("stream returns an emptyStream")
  public void streamReturnsAnEmptyStream() {
    List<Integer> list = Try.of(SAMPLE).map(i -> throwCheckedException(NOT_EXPECTED_MESSAGE))
      .stream().collect(Collectors.toList());
    assertThat(list, is(empty()));
  }

  @Test
  @DisplayName("isSuccess returns false")
  public void isSuccessReturnsFalse() {
    boolean actual = Try.of(SAMPLE).map(i -> throwCheckedException(NOT_EXPECTED_MESSAGE))
      .isSuccess();
    assertFalse(actual);
  }

  @Test
  @DisplayName("isFailure returns true")
  public void isFailureReturnsTrue() {
    boolean actual = Try.of(SAMPLE).map(i -> throwCheckedException(NOT_EXPECTED_MESSAGE))
      .isFailure();
    assertTrue(actual);
  }

  @Test
  public void forEachDoesNotExecuteTheFunction() {
    Try<Integer> failure = Try.of(SAMPLE)
      .map(i -> throwCheckedException(EXPECTED_EXCEPTION_MESSAGE));
    Assertions.assertDoesNotThrow(() -> failure.forEach(entry -> consumeWithException()));
  }

  @Test
  public void forEachReturnsFailure() {
    Try<Integer> failure = Try.of(SAMPLE)
      .map(i -> throwCheckedException(EXPECTED_EXCEPTION_MESSAGE));
    Try<Void> actual = failure.forEach(entry -> consumeWithException());
    assertTrue(actual.isFailure());
  }

  @Test
  public void shouldReturnEmptyOptionalOnFailure() {
    Try<Integer> failure = Try.of(SAMPLE)
      .map(i -> throwCheckedException(EXPECTED_EXCEPTION_MESSAGE));

    assertThat(failure.isFailure(), is(true));
    Optional<Integer> value = failure.toOptional(fail -> doNothing());
    assertThat(value.isEmpty(), is(true));
  }

  @Test
  public void shouldRunSuppliedFailureFunctionFunctionWhenConvertingToOptional() {
    Try<Integer> failure = Try.of(SAMPLE)
      .map(i -> throwCheckedException(EXPECTED_EXCEPTION_MESSAGE));
    AtomicBoolean actionAfterFailureRun = new AtomicBoolean(false);
    failure.toOptional(fail -> actionAfterFailureRun.set(true));

    assertThat(failure.isFailure(), is(true));
    assertThat(actionAfterFailureRun.get(), is(true));
  }

  @Test
  void shouldReturnEmptyOptional() {
    var expectedEmpty = Try.of(SAMPLE)
      .map(i -> throwCheckedException(EXPECTED_EXCEPTION_MESSAGE))
      .toOptional();
    assertThat(expectedEmpty.isEmpty(), is(true));
  }

  private void consumeWithException() {
    throw new RuntimeException(NOT_EXPECTED_MESSAGE);
  }

  private Try<Integer> anotherTry(Integer i) {
    return Try.attempt(() -> i + 1);
  }

  private int throwCheckedException(String exceptionMessage) throws IOException {
    throw new IOException(exceptionMessage);
  }

  private int throwUnCheckedException(String message) {
    throw new RuntimeException(message);
  }

  private int throwAnotherCheckedException() throws TestException {
    throw new TestException(EXPECTED_EXCEPTION_MESSAGE);
  }

  private void doNothing() {
    //NO-OP;
  }
}
