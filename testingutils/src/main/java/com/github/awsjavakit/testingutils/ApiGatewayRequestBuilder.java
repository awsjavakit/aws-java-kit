package com.github.awsjavakit.testingutils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.awsjavakit.apigateway.ApiGatewayEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ApiGatewayRequestBuilder {

  private final ObjectMapper objectMapper;
  private final ApiGatewayEvent event;

  public ApiGatewayRequestBuilder(ObjectMapper objectMapper) {

    this.objectMapper = objectMapper;
    this.event = new ApiGatewayEvent();
  }

  public static ApiGatewayRequestBuilder create(ObjectMapper objectMapper) {
    return new ApiGatewayRequestBuilder(objectMapper);
  }

  public <I> ApiGatewayRequestBuilder withBody(I body) {
    try {
      var bodyString = objectMapper.writeValueAsString(body);
      event.setBody(bodyString);
      return this;
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public InputStream build() {
    try {
      var eventString = objectMapper.writeValueAsString(event);
      return new ByteArrayInputStream(eventString.getBytes());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }
}
