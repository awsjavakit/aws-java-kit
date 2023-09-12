package com.github.awsjavakit.apigateway;

import static com.github.awsjavakit.apigateway.IoUtils.inputStreamToString;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.awsjavakit.apigateway.bodyparsing.BodyParser;
import com.github.awsjavakit.apigateway.exception.ApiGatewayException;
import com.github.awsjavakit.apigateway.responses.ResponseProvider;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public abstract class ApiGatewayHandler<I, O> implements RequestStreamHandler {

  protected final ResponseProvider successResponseProvider;
  private final ObjectMapper objectMapper;
  private final BodyParser<I> bodyParser;

  protected ApiGatewayHandler(Class<I> iClass, ObjectMapper objectMapper) {
    this(objectMapper,
      ResponseProvider.jsonOk(),
      BodyParser.jsonParser(iClass, objectMapper)
    );
  }

  protected ApiGatewayHandler(
    ObjectMapper objectMapper,
    ResponseProvider responseProvider,
    BodyParser<I> bodyParser) {
    this.objectMapper = objectMapper;
    this.successResponseProvider = responseProvider;
    this.bodyParser = bodyParser;
  }

  @Override
  public void handleRequest(InputStream input, OutputStream output, Context context)
    throws IOException {
    try {
      var inputString = inputStreamToString(input);
      var apiGatewayEvent = objectMapper.readValue(inputString, ApiGatewayEvent.class);
      var body = apiGatewayEvent.getBody();
      var parsedBody = parseBody(body);
      var o = processInput(parsedBody, apiGatewayEvent, context);
      writeSuccess(o, output);
    } catch (ApiGatewayException expectedException) {
      writeFailure(output, expectedException);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

  }

  public abstract O processInput(I body, ApiGatewayEvent apiGatewayEvent, Context context)
    throws ApiGatewayException;

  private void writeFailure(OutputStream outputStream, ApiGatewayException expectedException)
    throws IOException {
    write(outputStream, expectedException.message(), expectedException);
  }

  private void writeSuccess(O o, OutputStream outputStream) throws IOException {
    write(outputStream, o, successResponseProvider);
  }

  private <ResponseBody> void write(OutputStream outputStream,
    ResponseBody responseBody, ResponseProvider responseProvider) throws IOException {
    try (var writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
      var gateWayResponse =
        GatewayResponse.create(responseBody,
          responseProvider.getStatusCode(),
          responseProvider.getHeaders(),
          objectMapper);
      writer.write(gateWayResponse.toJsonString());
    }
  }

  private I parseBody(String body) {
    return bodyParser.parseBody(body);
  }

}
