package com.github.awsjavakit.apigateway.observers;

public class BodyObserver<S> implements InputObserver {

  private S body;

  @Override
  public <I> void observe(I body) {
    this.body = (S) body;
  }

  public S getBody() {
    return body;
  }
}
