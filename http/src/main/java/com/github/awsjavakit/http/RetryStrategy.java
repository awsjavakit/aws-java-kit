package com.github.awsjavakit.http;

import static com.gtihub.awsjavakit.attempt.Try.attempt;

import java.util.concurrent.Callable;

@FunctionalInterface
public interface RetryStrategy {

  int DEFAULT_WAITING_TIME = 1000;

  static RetryStrategy defaultStrategy() {
    return new DefaultRetryStrategy(DEFAULT_WAITING_TIME);
  }

  <T> T apply(Callable<T> trial);

  class DefaultRetryStrategy implements RetryStrategy {

    private final int waitingTime;

    public DefaultRetryStrategy(int waitingTime) {
      this.waitingTime = waitingTime;
    }

    @Override
    public <T> T apply(Callable<T> trial) {
      return attempt(trial).orElse(fail -> retry(trial));
    }

    private <T> T retry(Callable<T> trial) {
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
