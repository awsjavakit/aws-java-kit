package com.github.awsjavakit.stepfunctions.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import tools.jackson.databind.ObjectMapper;

public class SampleHandler extends StepFunctionHandler<SomeInputClass, SomeOutputClass> {

  public SampleHandler(ObjectMapper objectMapper) {
    super(SomeInputClass.class, objectMapper);
  }

  @Override
  protected SomeOutputClass processInput(SomeInputClass input, Context context) {
    return input.transform();
  }
}
