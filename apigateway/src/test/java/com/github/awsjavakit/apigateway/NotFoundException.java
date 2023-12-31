package com.github.awsjavakit.apigateway;

import com.github.awsjavakit.apigateway.exception.ApiGatewayException;
import java.net.HttpURLConnection;
import java.util.Map;

public class NotFoundException extends ApiGatewayException {

  @Override
  public Map<String, String> getHeaders() {
    return Headers.CONTENT_TYPE_APPLICATION_JSON;
  }

  @Override
  public int getStatusCode() {
    return HttpURLConnection.HTTP_NOT_FOUND;
  }

  @Override
  public String message() {
    return "Resource not found";
  }
}
