package com.github.awsjavakit.apigateway;

import static com.github.awsjavakit.apigateway.HeaderValues.APPLICATION_JSON;

import java.util.Map;

public final class Headers {

  /**
   * Content-Type header
   */
  public static final String CONTENT_TYPE = "Content-Type";

  public static final Map<String, String> CONTENT_TYPE_APPLICATION_JSON =
    Map.of(CONTENT_TYPE, APPLICATION_JSON);

  private Headers() {

  }

}
