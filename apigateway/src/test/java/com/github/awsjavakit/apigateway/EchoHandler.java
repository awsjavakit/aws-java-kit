package com.github.awsjavakit.apigateway;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EchoHandler extends ApiGatewayHandler<String, String> {

  protected EchoHandler(ObjectMapper objectMapper) {
    super(String.class, objectMapper);
  }

  @Override
  public String processInput(String body, ApiGatewayEvent apiGatewayEvent, Context context) {
    return body;
  }
}
