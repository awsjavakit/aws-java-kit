package com.github.awsjavakit.apigateway;

import static com.github.awsjavakit.apigateway.IoUtils.inputStreamToString;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;

public abstract class ApiGatewayHandler<I, O> implements RequestStreamHandler {

  private final Class<I> iClass;
  private final ObjectMapper objectMapper;

  public ApiGatewayHandler(Class<I> iClass, ObjectMapper objectMapper) {
    this.iClass = iClass;
    this.objectMapper = objectMapper;
  }

  @Override
  public void handleRequest(InputStream input, OutputStream output, Context context)
    throws IOException {

    var inputString = inputStreamToString(input);
    var apiGatewayEvent = objectMapper.readValue(inputString, ApiGatewayEvent.class);
    var body = apiGatewayEvent.getBody();
    var parsedBody = parseBody(body);
    var o = processInput(parsedBody, apiGatewayEvent, context);
    writeSuccess(o, output);

  }

  public abstract O processInput(I body, ApiGatewayEvent apiGatewayEvent, Context context);

  protected abstract Map<String, String> getSuccessHeaders();

  protected abstract int getSuccessStatusCode();

  private void writeSuccess(O o, OutputStream outputStream) throws IOException {
    try (var writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
      var gateWayResponse =
        GatewayResponse.create(o, getSuccessStatusCode(), getSuccessHeaders(), objectMapper);
      writer.write(gateWayResponse.toJsonString());
    }
  }

  private I parseBody(String body) throws JsonProcessingException {
    var json = objectMapper.readTree(body);
    if (inputIsStringAndExpectedInputIsString(json)) {
      return iClass.cast(json.textValue());
    }
    if (inputIsJsonStringAsTextValueAndExpectedIsAnObject(json)) {
      var textualValue = json.textValue();
      return objectMapper.readValue(textualValue, iClass);
    } else {
      return objectMapper.readValue(body, iClass);
    }

  }

  private boolean inputIsJsonStringAsTextValueAndExpectedIsAnObject(JsonNode json) {
    return json.isTextual() && !iClass.equals(String.class);
  }

  private boolean inputIsStringAndExpectedInputIsString(JsonNode json) {
    return iClass.equals(String.class) && json.isTextual();
  }
}
