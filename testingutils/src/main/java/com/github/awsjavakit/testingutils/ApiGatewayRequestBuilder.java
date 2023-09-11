package com.github.awsjavakit.testingutils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.awsjavakit.apigateway.ApiGatewayEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class for building HttpRequests v1.0 for ApiGateway.
 */
public final class ApiGatewayRequestBuilder {

  private final ObjectMapper objectMapper;
  private final ApiGatewayEvent event;

  private ApiGatewayRequestBuilder(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    this.event = new ApiGatewayEvent();
  }

  /**
   * The default factory method.
   *
   * @param objectMapper the mapper for serializing the ApiGateway Request event.
   * @return an {@link ApiGatewayRequestBuilder}
   */
  public static ApiGatewayRequestBuilder create(ObjectMapper objectMapper) {
    return new ApiGatewayRequestBuilder(objectMapper);
  }

  /**
   * The request body.
   *
   * @param body the request body.
   * @param <I>  the body type
   * @return the builder.
   */
  public <I> ApiGatewayRequestBuilder withBody(I body) {
    try {
      var bodyString = objectMapper.writeValueAsString(body);
      event.setBody(bodyString);
      return this;
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * An {@link InputStream} that will be supplied to the
   * {@link com.github.awsjavakit.apigateway.ApiGatewayHandler#handleRequest} method
   *
   * @return an InputStream containing a serialized ApiGateway event;
   */
  public InputStream build() {
    try {
      var eventString = objectMapper.writeValueAsString(event);
      return new ByteArrayInputStream(eventString.getBytes());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
