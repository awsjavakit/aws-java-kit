package com.github.awsjavakit.attempt;

import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gtihub.awsjavakit.attempt.Failure;
import com.gtihub.awsjavakit.attempt.Try;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class SuccessTest {

  private static final String NOT_EXPECTED_MESSAGE = "Not expected message";
  private final Integer sample = 1;

  @Test
  @DisplayName("orElseThrow throws illegalStateException for null argument")
  public void shouldThrowIllegalStateExceptionForNullArgument() {

    Executable action =
      () -> Try.of(sample).orElseThrow((Function<Failure<Integer>, ? extends Exception>) null);
    IllegalStateException exception = assertThrows(IllegalStateException.class, action);
    assertThat(exception.getMessage(), is(equalTo(Failure.NULL_ACTION_MESSAGE)));
  }

  @Test
  @DisplayName("orElseThrow returns the calculated value and does not throw an exception")
  public void shouldReturnTheCalculatedValueAndNotThrowException()
    throws TestException {
    Integer actual = Try.of(sample)
      .orElseThrow(f -> new TestException(f.getException(), NOT_EXPECTED_MESSAGE));

    assertThat(actual, is(equalTo(sample)));
  }

  @Test
  @DisplayName("orElse returns the calculated value and does not return the defined value")
  public void shouldReturnTheCalculatedValueAndNotExecuteFailureScenario() {

    int expectedValue = 10;
    int unexpectedValue = 20;
    Integer actual = Try.of(expectedValue)
      .map(this::identity)
      .orElse(failure -> unexpectedValue);

    assertThat(actual, is(equalTo(expectedValue)));
  }

  @Test
  @DisplayName("orElse returns the calculated value and does not perform the defined action")
  public void orElseThrowsReturnsTheCalculatedValueAndDoesNotPerformTheDefinedAction()
    throws TestException {
    Integer actual = Try.of(sample)
      .orElse(f -> anotherIllegalAction(NOT_EXPECTED_MESSAGE));

    assertThat(actual, is(equalTo(sample)));
  }

  @Test
  @DisplayName("orElse throws IllegalStateException when the input arg is null")
  public void orElseThrowsIllegalStateExceptionWhenTheInputArgumentIsNull() {

    Executable action =
      () -> Try.of(sample)
        .orElse(null);

    assertThrows(IllegalStateException.class, action);
  }

  @Test
  @DisplayName("flatMap returns the value of the nested Try")
  public void flatMapReturnsAFailureWithTheFirstException() {
    Try<Integer> actual = Try.of(sample)
      .map(this::identity)
      .flatMap(this::tryIdentity);

    assertTrue(actual.isSuccess());
    assertThat(actual.get(), is(equalTo(sample)));
  }

  @Test
  @DisplayName("getException throws IllegalStateException")
  public void shouldThrowExceptionWhenTryingToGetTheExceptionOfSuccess() {
    Executable action = () -> Try.of(sample).getException();
    assertThrows(IllegalStateException.class, action);
  }

  @Test
  @DisplayName("stream returns an emptyStream")
  public void shouldReturnAStreamWithTheContainedValue() {
    List<Integer> list = Try.of(sample).stream()
      .collect(Collectors.toList());
    Integer actual = list.stream().findFirst().get();
    assertThat(list, is(not(empty())));
    assertThat(actual, is(sample));
  }

  @Test
  @DisplayName("isSuccess returns true")
  public void shouldReturnThatItisASuccess() {
    boolean actual = Try.of(sample).isSuccess();
    assertTrue(actual);
  }

  @Test
  @DisplayName("isFailure returns false")
  public void shouldReturnThatItisNotAFailure() {
    boolean actual = Try.of(sample).isFailure();
    assertFalse(actual);
  }

  @Test
  public void shouldReturnVoidSuccessWhenConsumingTheContainedValueSuccessfully() {
    Try<Void> result = Try.of(sample).forEach(this::consume);
    assertTrue(result.isSuccess());
  }

  @Test
  public void shouldReturnVoidFalureWhenConsumingTheContainedValueUnsuccessfully() {
    Try<Void> result = Try.of(sample).forEach(this::throwException);
    assertTrue(result.isFailure());
  }

  @Test
  public void toOptionalWithFunctionReturnsPresentOptional() {
    Try<Integer> success = Try.of(sample);
    assertThat(success.isSuccess(), is(true));
    Optional<Integer> value = success.toOptional(fail -> doNothing());
    assertThat(value.isPresent(), is(true));
    assertThat(value.get(), is(equalTo(sample)));
  }

  @Test
  public void toOptionalReturnsPresentOptionalWithTheContainedValue() {
    Try<Integer> success = Try.of(sample);
    assertThat(success.isSuccess(), is(true));
    Optional<Integer> value = success.toOptional();
    assertThat(value.isPresent(), is(true));
    assertThat(value.get(), is(equalTo(sample)));
  }

  @Test
  public void toOptionalWithFunctionDoesNotRunFunctionForFailure() {
    Try<Integer> success = Try.of(sample);
    AtomicBoolean actionAfterFailureRun = new AtomicBoolean(false);
    success.toOptional(fail -> actionAfterFailureRun.set(true));

    assertThat(success.isSuccess(), is(true));
    assertThat(actionAfterFailureRun.get(), is(equalTo(false)));
  }

  @Test
  public void orElseThrowThrowsRuntimeExceptionWhenTryisSuccess() {
    int expectedValue = 2;
    int actualValue = Try.of(expectedValue)
      .map(this::identity)
      .orElseThrow();
    assertThat(actualValue, is(equalTo(expectedValue)));
  }

  @Test
  void shouldBeEqualWhenContainingEqualObjects() {
    var contained = randomString();
    var left = Try.of(contained);
    var right = Try.of(contained);
    assertThat(left, is(equalTo(right)));
    assertThat(left.hashCode(), is(equalTo(right.hashCode())));
  }

  private void consume(int value) {
  }

  private void throwException(int value) {
    throw new IllegalArgumentException(Integer.toString(value));
  }

  private Integer identity(Integer input) {
    return input;
  }

  private Try<Integer> tryIdentity(Integer i) {
    return Try.attempt(() -> i);
  }

  private int anotherIllegalAction(String message) throws TestException {
    throw new TestException(message);
  }

  private void doNothing() {
    //NO-OP;
  }
}
