package com.github.awsjavakit.apigateway.observers;

import com.github.awsjavakit.apigateway.exception.ApiGatewayException;

public class ThrowExceptionObserver<E extends ApiGatewayException> implements InputObserver {

  public E exception;

  public ThrowExceptionObserver(E exception) {
    this.exception = exception;
  }

  @Override
  public <I> void observe(I body) throws ApiGatewayException {
    throw exception;
  }
}
