package com.github.awsjavakit.apigateway;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.awsjavakit.apigateway.exception.ApiGatewayException;
import java.util.Arrays;
import java.util.List;

public class EchoHandler extends ApiGatewayHandler<String, String> {

  private final List<InputObserver> observers;

  protected EchoHandler(ObjectMapper objectMapper, InputObserver... observers) {
    super(String.class, objectMapper);
    this.observers = Arrays.asList(observers);
  }

  @Override
  public String processInput(String body, ApiGatewayEvent apiGatewayEvent, Context context)
    throws ApiGatewayException {
    for (var observer : observers) {
      observer.observe(body);
    }
    return body;
  }
}
