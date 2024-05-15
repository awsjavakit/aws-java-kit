package com.github.awsjavakit.http;

import static com.gtihub.awsjavakit.attempt.Try.attempt;

import com.gtihub.awsjavakit.attempt.FunctionWithException;

@FunctionalInterface
public interface RetryStrategy {

  <I, O, E extends Exception> O apply(FunctionWithException<I, O, E> trial, I input);

  class DefaultRetryStrategy implements RetryStrategy {

    private final int waitingTime;

    public DefaultRetryStrategy(int waitingTime) {
      this.waitingTime = waitingTime;
    }

    @Override
    public <I, O, E extends Exception> O apply(FunctionWithException<I, O, E> function, I input) {
      return attempt(() -> function.apply(input)).orElse(fail->retry(function,input));
    }

    private <I, O, E extends Exception> O retry(FunctionWithException<I, O, E> function, I input) {
      pause();
      return attempt(() -> function.apply(input)).orElseThrow();
    }

    private void pause() {
      try {
        Thread.sleep(waitingTime);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
