package com.github.awsjavakit.apigateway.exception;

import com.github.awsjavakit.apigateway.responses.ResponseProvider;

public abstract class ApiGatewayException extends Exception implements ResponseProvider {

  public abstract String message();

}
