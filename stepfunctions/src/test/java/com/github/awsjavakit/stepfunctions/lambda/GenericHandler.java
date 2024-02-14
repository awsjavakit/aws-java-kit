package com.github.awsjavakit.stepfunctions.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GenericHandler<T> extends StepFunctionHandler<T, T> {

  public GenericHandler(Class<T> type, ObjectMapper json) {
    super(type, json);
  }

  @Override
  protected T processInput(T input, Context context) {
    return input;
  }
}
