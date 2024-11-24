package com.github.awsjavakit.misc.virtualfuture;

import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomInteger;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

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

  private Integer task() {
    return TASK_RESULT;
  }

}