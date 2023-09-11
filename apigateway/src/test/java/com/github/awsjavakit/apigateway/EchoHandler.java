package com.github.awsjavakit.apigateway;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.awsjavakit.apigateway.exception.ApiGatewayException;
import com.github.awsjavakit.apigateway.observers.InputObserver;
import java.util.Arrays;
import java.util.List;

public class EchoHandler<I> extends ApiGatewayHandler<I, I> {

  private final List<InputObserver> observers;

  protected EchoHandler(Class<I> iClass,
    ObjectMapper objectMapper,
    InputObserver... observers) {
    super(iClass, objectMapper);
    this.observers = Arrays.asList(observers);
  }

  @Override
  public I processInput(I body, ApiGatewayEvent apiGatewayEvent, Context context)
    throws ApiGatewayException {
    for (var observer : observers) {
      observer.observe(body);
    }
    return body;
  }
}
