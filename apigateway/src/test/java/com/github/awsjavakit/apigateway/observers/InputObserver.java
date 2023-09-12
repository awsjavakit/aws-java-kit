package com.github.awsjavakit.apigateway.observers;

import com.github.awsjavakit.apigateway.NotFoundException;
import com.github.awsjavakit.apigateway.exception.ApiGatewayException;

public interface InputObserver {

  static InputObserver noOp() {
    return new InputObserver() {
      @Override
      public <I> void observe(I body) throws ApiGatewayException {
        //NO-OP
      }
    };
  }

  static InputObserver throwException(NotFoundException e) {
    return new ThrowExceptionObserver<>(e);
  }

  <I> void observe(I body) throws ApiGatewayException;
}
