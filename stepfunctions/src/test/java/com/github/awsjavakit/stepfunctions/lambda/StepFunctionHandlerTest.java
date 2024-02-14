package com.github.awsjavakit.stepfunctions.lambda;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class StepFunctionHandlerTest {

  @Test
  void shouldThrowNotImplementedError() {
    var handler = new StepFunctionHandler();
    assertThrows(UnsupportedOperationException.class,
      () -> handler.handleRequest(null, null, null));
  }

}