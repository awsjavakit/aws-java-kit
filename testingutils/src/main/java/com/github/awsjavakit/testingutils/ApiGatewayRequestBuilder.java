package com.github.awsjavakit.testingutils;

import static com.gtihub.awsjavakit.attempt.Try.attempt;
import static java.util.Objects.nonNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.awsjavakit.apigateway.ApiGatewayEvent;
import com.github.awsjavakit.apigateway.HttpMethod;
import com.github.awsjavakit.misc.paths.UnixPath;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

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
    if (nonNull(body)) {
      return attempt(() -> addBodyToEvent(body)).orElseThrow();
    }
    event.setBody(null);
    return this;
  }


  /**
   * Add the query path.
   *
   * @param path the path.
   * @return the builder.
   */
  public ApiGatewayRequestBuilder withPath(UnixPath path) {
    event.setPath(path.addRoot().toString());
    return this;
  }

  /**
   * Add queryParameters.
   *
   * @param queryParameters map
   * @return the builder.
   */
  public ApiGatewayRequestBuilder withQueryParameters(Map<String, String> queryParameters) {
    event.setQueryParameters(queryParameters);
    return this;
  }

  public ApiGatewayRequestBuilder withHeaders(Map<String, String> headers) {
    event.setHeaders(Collections.unmodifiableMap(headers));
    var mulitValueHeaders = convertToMultiValueHeaders(headers);
    event.setMultiValueHeaders(mulitValueHeaders);
    return this;
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

  public ApiGatewayRequestBuilder withMethod(HttpMethod httpMethod) {
    event.setHttpMethod(httpMethod);
    return this;
  }

  private <I> ApiGatewayRequestBuilder addBodyToEvent(I body) throws JsonProcessingException {
    if (body instanceof String string) {
      event.setBody(string);
    } else {
      event.setBody(objectMapper.writeValueAsString(body));
    }
    return this;
  }

  private Map<String, List<String>> convertToMultiValueHeaders(Map<String, String> headers) {
    return headers.entrySet().stream()
      .map(entry -> Map.entry(entry.getKey(), createSingleItemList(entry.getValue())))
      .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

  }

  private List<String> createSingleItemList(String value) {
    return Collections.singletonList(value);
  }
}
