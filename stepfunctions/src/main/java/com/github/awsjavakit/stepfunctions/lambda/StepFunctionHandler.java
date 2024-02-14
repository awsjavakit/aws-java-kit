package com.github.awsjavakit.stepfunctions.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.InputStream;
import java.io.OutputStream;

public class StepFunctionHandler implements RequestStreamHandler {

  @Override
  public void handleRequest(InputStream input, OutputStream output, Context context) {
     throw new UnsupportedOperationException("Not implemented yet");
  }
}
