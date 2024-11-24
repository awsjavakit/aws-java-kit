package com.github.awsjavakit.misc.virtualfuture;

import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomInteger;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;

class VirtualFutureTest {

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

  private Integer task() {
    return TASK_RESULT;
  }

}