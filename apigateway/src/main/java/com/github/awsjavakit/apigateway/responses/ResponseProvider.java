package com.github.awsjavakit.apigateway.responses;

import java.util.Map;

public interface ResponseProvider {

  static ResponseProvider jsonOk() {
    return new JsonOk();

  }

  Map<String, String> successHeaders();

  int statusCode();

}
