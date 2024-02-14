package com.github.awsjavakit.stepfunctions.lambda;

import static com.gtihub.awsjavakit.attempt.Try.attempt;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public abstract class StepFunctionHandler<I, O> implements RequestStreamHandler {

  private final Class<I> inputClass;
  private final ObjectMapper objectMapper;

  public StepFunctionHandler(Class<I> inputClass, ObjectMapper objectMapper) {
    this.inputClass = inputClass;
    this.objectMapper = objectMapper;
  }

  @Override
  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
    throws IOException {
    var input = attempt(() -> objectMapper.readValue(inputStream, inputClass)).orElseThrow();
    var output = processInput(input, context);
    writeOutput(output, outputStream);
  }

  protected abstract O processInput(I input, Context context);

  private void writeOutput(O output, OutputStream outputStream) throws IOException {
    try (var writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
      var json = attempt(() -> objectMapper.writeValueAsString(output)).orElseThrow();
      writer.write(json);
      writer.flush();
    }
  }
}
