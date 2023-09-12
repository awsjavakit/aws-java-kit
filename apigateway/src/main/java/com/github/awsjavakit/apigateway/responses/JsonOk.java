package com.github.awsjavakit.apigateway.responses;

import static com.github.awsjavakit.apigateway.HeaderValues.APPLICATION_JSON;

import com.github.awsjavakit.apigateway.Headers;
import java.net.HttpURLConnection;
import java.util.Map;

public class JsonOk implements ResponseProvider {

  /**
   * Return the header "ContentType=application/json".
   *
   * @return the header "ContentType=application/json".
   */
  @Override
  public Map<String, String> getHeaders() {
    return Map.of(Headers.CONTENT_TYPE, APPLICATION_JSON);
  }

  /**
   * OK.
   *
   * @return OK.
   */
  @Override
  public int getStatusCode() {
    return HttpURLConnection.HTTP_OK;
  }
}


