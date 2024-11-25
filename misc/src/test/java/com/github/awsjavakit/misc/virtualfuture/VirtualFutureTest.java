package com.github.awsjavakit.misc.virtualfuture;

import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomInteger;
import static com.gtihub.awsjavakit.attempt.Try.attempt;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.gtihub.awsjavakit.attempt.Try;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;
import org.apache.commons.lang3.IntegerRange;
import org.junit.jupiter.api.Test;

class VirtualFutureTest {

  public static final int MUCH_LESS_TIME_THAN_SUM_OF_DELAYS = 5;
  private static final Integer TASK_RESULT = randomInteger();

  @Test
  void shouldExecuteSuppliedTask() throws ExecutionException, InterruptedException {
    var future = VirtualFuture.supply(this::task);
    future.join();
    var result = future.get();
    assertThat(result).isEqualTo(TASK_RESULT);
  }

  @Test
  void shouldCombineMultipleFutures() throws ExecutionException, InterruptedException {
    var futures = List.of(
      VirtualFuture.supply(this::task),
      VirtualFuture.supply(this::task)
    );
    var awaitAllTasks =
      VirtualFuture.allOf(futures.toArray(VirtualFuture[]::new));
    awaitAllTasks.join();

    for (var future : futures) {
      assertThat(future.get()).isEqualTo(TASK_RESULT);
    }
  }

  @Test
  void shouldExecuteTasksInParallel() {
    var range = IntegerRange.of(0, 100);
    var startTime = Instant.now();

    var futures = IntStream.range(range.getMinimum(), range.getMaximum() + 1)
      .boxed()
      .map(number -> VirtualFuture.supply(() -> taskWithDelay(number)))
      .toList();

    VirtualFuture.allOf(futures.toArray(VirtualFuture[]::new)).join();

    var summationResult = sumResultsOfAllFutures(futures);
    var endTime = Instant.now();

    var taskDuration = Duration.between(startTime, endTime);
    var expectedResult = sumOfIntegerRange(range.getMinimum(), range.getMaximum());
    assertThat(summationResult).isEqualTo(expectedResult);
    assertThat(taskDuration)
      .isLessThanOrEqualTo(Duration.ofSeconds(MUCH_LESS_TIME_THAN_SUM_OF_DELAYS));

  }

  @Test
  void shouldNotHaveATaskWhenCombiningFutures() throws ExecutionException, InterruptedException {
    var future = VirtualFuture.supply(this::task);
    var combinedFuture = VirtualFuture.allOf(future);
    assertThat(combinedFuture.get()).isNull();
  }

  private static Integer sumResultsOfAllFutures(List<VirtualFuture<Integer>> futures) {
    return futures.stream()
      .map(attempt(VirtualFuture::get))
      .map(Try::orElseThrow)
      .reduce(Integer::sum)
      .orElseThrow();
  }

  private static int sumOfIntegerRange(int minInclusive, int maxInclusive) {
    var amountOfNumbers = 1 + maxInclusive - minInclusive;
    return (minInclusive + maxInclusive) * amountOfNumbers / 2;

  }

  private Integer task() {
    return TASK_RESULT;
  }

  private Integer taskWithDelay(int input) {
    delay();
    return input;
  }

  private void delay() {
    try {
      Thread.sleep(Duration.ofSeconds(1));
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

}