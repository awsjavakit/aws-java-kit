package com.github.awsjavakit.stepfunctions.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EchoHandler extends StepFunctionHandler<SomeInputClass,SomeInputClass>{

  public EchoHandler(ObjectMapper objectMapper) {
    super(SomeInputClass.class, objectMapper);
  }

  @Override
  protected SomeInputClass processInput(SomeInputClass input, Context context) {
    return input;
  }
}
