package com.github.awsjavakit.apigateway;

import static com.github.awsjavakit.apigateway.IoUtils.inputStreamToString;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.awsjavakit.apigateway.bodyparsing.BodyParser;
import com.github.awsjavakit.apigateway.responses.ResponseProvider;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public abstract class ApiGatewayHandler<I, O> implements RequestStreamHandler {

  private final ObjectMapper objectMapper;
  private final ResponseProvider responseProvider;
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
    this.responseProvider = responseProvider;
    this.bodyParser = bodyParser;
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

  private void writeSuccess(O o, OutputStream outputStream) throws IOException {
    try (var writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
      var gateWayResponse =
        GatewayResponse.create(o,
          responseProvider.statusCode(),
          responseProvider.successHeaders(),
          objectMapper);
      writer.write(gateWayResponse.toJsonString());
    }
  }

  private I parseBody(String body) {
    return bodyParser.parseBody(body);
  }

}
