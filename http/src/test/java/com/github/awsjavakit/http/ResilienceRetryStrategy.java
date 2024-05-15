package com.github.awsjavakit.http;

import com.gtihub.awsjavakit.attempt.FunctionWithException;
import io.github.resilience4j.core.functions.CheckedSupplier;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;

public class ResilienceRetryStrategy implements RetryStrategy {

  private final RetryRegistry registry;

  public ResilienceRetryStrategy() {
    var config = RetryConfig.custom()
      .maxAttempts(2)
      .waitDuration(Duration.ofMillis(1))
      .build();
    this.registry = RetryRegistry.of(config);

  }

  @Override
  public <I, O, E extends Exception> O apply(FunctionWithException<I, O, E> trial, I input) {
    var checkedSupplier = new CheckedSupplier<O>() {
      @Override
      public O get() throws Throwable {
        return trial.apply(input);
      }
    };
    try {
      return registry.retry(input.toString()).executeCheckedSupplier(checkedSupplier);
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }

  }
}
