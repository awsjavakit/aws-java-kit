package com.github.awsjavakit.misc.lists;

import java.util.List;
import java.util.function.BiFunction;

public class Benchmark<T, R extends List<List<T>>> {

  public static final int REPETITIONS = 10;
  private final List<T> sample;
  private final int numberOfPartitions;
  private final BiFunction<List<T>, Integer, R> listCreator;
  private BenchmarkResult result;

  public Benchmark(List<T> sample, int partitions, BiFunction<List<T>, Integer, R> listCreator) {
    this.sample = sample;
    this.numberOfPartitions = partitions;
    this.listCreator = listCreator;
  }

  public BenchmarkResult run() {
    long processingTime = 0;
    int elementsCount = 0;
    for (int i = 0; i < REPETITIONS; i++) {
      long start = System.currentTimeMillis();
      var partitioned = listCreator.apply(sample, numberOfPartitions);
      elementsCount = processElements(partitioned, elementsCount);
      long end = System.currentTimeMillis();
      processingTime += end - start;
    }
    double averageProcessingTime = (double) processingTime / (double) REPETITIONS;

    this.result = new BenchmarkResult(elementsCount, averageProcessingTime);
    return this.result;
  }

  public BenchmarkResult getResult() {
    return result;
  }

  private static <T, R extends List<List<T>>> Integer processElements(R partitioned,
    int lastCount) {
    var currentCount = partitioned.stream().parallel()
      .map(List::size).reduce(Integer::sum).orElseThrow();
    validateCount(lastCount, currentCount);
    return currentCount;
  }

  private static void validateCount(int lastCount, Integer currentCount) {
    if (currentCount != lastCount && notFirstExecution(lastCount)) {
      throw new IllegalStateException(
        "Current number of objects is not the same as in the last execution");
    }
  }

  private static boolean notFirstExecution(int lastCount) {
    return lastCount != 0;
  }

  public record BenchmarkResult(int resultSize, double averageProcessingTime) {

  }
}
