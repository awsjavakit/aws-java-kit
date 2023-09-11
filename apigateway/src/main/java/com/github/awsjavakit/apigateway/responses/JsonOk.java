package com.github.awsjavakit.apigateway.responses;

import static com.github.awsjavakit.apigateway.HeaderValues.APPLICATION_JSON;

import com.github.awsjavakit.apigateway.Headers;
import java.net.HttpURLConnection;
import java.util.Map;

public class JsonOk implements ResponseProvider {

  @Override
  public Map<String, String> successHeaders() {
    return Map.of(Headers.CONTENT_TYPE, APPLICATION_JSON);
  }

  @Override
  public int statusCode() {
    return HttpURLConnection.HTTP_OK;
  }
}


