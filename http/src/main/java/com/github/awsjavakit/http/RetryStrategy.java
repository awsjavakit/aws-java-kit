package com.github.awsjavakit.http;

import static com.gtihub.awsjavakit.attempt.Try.attempt;

import com.gtihub.awsjavakit.attempt.FunctionWithException;
import java.time.Duration;

@FunctionalInterface
public interface RetryStrategy {

  static RetryStrategy defaultStrategy(Duration waitingTime) {
    return new DefaultRetryStrategy(waitingTime);
  }

  //We cannot avoid throwing the generic Exception because external libraries
  // (e.g. resilience4j) throw it, and we need to integrate with them
  @SuppressWarnings("PMD.SignatureDeclareThrowsException")
  <I, O, E extends Exception> O apply(FunctionWithException<I, O, E> trial, I input)
    throws Exception;

  class DefaultRetryStrategy implements RetryStrategy {

    private final Duration waitingTime;

    public DefaultRetryStrategy(Duration waitingTime) {
      this.waitingTime = waitingTime;
    }

    @Override
    public <I, O, E extends Exception> O apply(FunctionWithException<I, O, E> function, I input)
      throws E {
      return attempt(() -> function.apply(input)).orElse(fail -> retry(function, input));
    }

    private <I, O, E extends Exception> O retry(FunctionWithException<I, O, E> function, I input)
      throws E {
      pause();
      return function.apply(input);
    }

    private void pause() {
      try {
        Thread.sleep(waitingTime.toMillis());
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
