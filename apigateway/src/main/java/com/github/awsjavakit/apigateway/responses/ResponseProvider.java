package com.github.awsjavakit.apigateway.responses;

import java.util.Map;

/**
 * A ResponseProvider supplies the headers and the status code that are going to be set in the
 * response sent by ApiGatewayHandler
 */
public interface ResponseProvider {

  /**
   * Default response provider when returning a JSON object.
   *
   * @return a {@link ResponseProvider} returning OK with header Content-Type= application/json.
   */
  static ResponseProvider jsonOk() {
    return new JsonOk();

  }

  /**
   * The headers that will be included in the response.
   * @return the headers that will be included in the response
   */
  Map<String, String> getHeaders();

  /**
   * The response status code.
   * @return the response status code.
   */
  int getStatusCode();

}
