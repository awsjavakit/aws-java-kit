package com.github.awsjavakit.apigateway;

import com.github.awsjavakit.apigateway.exception.ApiGatewayException;

public interface InputObserver {

  static InputObserver noOp() {
    return new InputObserver() {
      @Override
      public <I> void observe(I body) {
        //NO-OP
      }
    };
  }

  static InputObserver throwException(NotFoundException e) {
    return new ThrowExceptionObserver<>(e);
  }

  <I> void observe(I body) throws ApiGatewayException;
}
