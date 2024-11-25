package com.github.awsjavakit.misc.virtualfuture;

import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomInteger;
import static com.gtihub.awsjavakit.attempt.Try.attempt;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.gtihub.awsjavakit.attempt.Try;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Supplier;
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
  void shouldNotHaveATaskWhenCombiningFutures() throws ExecutionException, InterruptedException {
    var future = VirtualFuture.supply(this::task);
    var combinedFuture = VirtualFuture.allOf(future);
    assertThat(combinedFuture.get()).isNull();
  }

  @Test
  void shouldRunSuppliedMappingFunction() throws ExecutionException, InterruptedException {
    Supplier<Integer> task1 = this::task;
    Function<Integer, String> task2 = VirtualFutureTest::evenOrOdd;
    var future = VirtualFuture.supply(task1).map(task2);
    future.join();

    assertThat(future.get()).isEqualTo(evenOrOdd(TASK_RESULT));

  }

  @Test
  void shouldExecuteAllChainedTasksOnAFutureOnTheSameThread()
    throws ExecutionException, InterruptedException {
    Supplier<Set<Long>> task1 = () -> taskReturningThreadId(Collections.emptySet());
    Function<Set<Long>, Set<Long>> task2 = this::taskReturningThreadId;
    var numberOfExecutionThreads = VirtualFuture.supply(task1).map(task2);
    numberOfExecutionThreads.join();

    assertThat(numberOfExecutionThreads.get().size()).isEqualTo(1);
  }

  @Test
  void shouldExecuteAllTasksInParallel(){
    var range = IntegerRange.of(0, 100);
    var startTime = Instant.now();

    var futures = IntStream.range(range.getMinimum(), range.getMaximum() + 1)
      .boxed()
      .map(number -> VirtualFuture.supply(() -> taskWithDelay(number)))
      .map(future->future.map(VirtualFutureTest::anotherTaskWihDelay))
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

  private static String evenOrOdd(Integer input) {
    return input % 2 == 0 ? "even" : "odd";
  }

  private static Integer anotherTaskWihDelay(Integer input) {
    delay();
    return input;
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

  private HashSet<Long> taskReturningThreadId(Set<Long> threadIds) {
    var set = new HashSet<>(threadIds);
    set.add(Thread.currentThread().threadId());
    return set;
  }

  private static void delay() {
    try {
      Thread.sleep(Duration.ofSeconds(1));
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

}