package com.github.awsjavakit.http;

import static com.gtihub.awsjavakit.attempt.Try.attempt;

import com.gtihub.awsjavakit.attempt.SupplierWithException;

@FunctionalInterface
public interface RetryStrategy {

  static RetryStrategy defaultStrategy() {
    return new DefaultRetryStrategy(500);
  }

  <T> T apply(SupplierWithException<T, Exception> trial);

  class DefaultRetryStrategy implements RetryStrategy {

    private final int waitingTime;

    public DefaultRetryStrategy(int waitingTime) {
      this.waitingTime = waitingTime;
    }

    @Override
    public <T> T apply(SupplierWithException<T, Exception> trial) {
      return attempt(trial).orElse(fail -> retry(trial));
    }

    private <T> T retry(SupplierWithException<T, Exception> trial) {
      pause();
      return attempt(trial).orElseThrow();
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
