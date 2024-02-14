package com.github.awsjavakit.stepfunctions.lambda;

import java.util.Locale;

public record SomeInputClass(String someString, Integer someNumber) {

  public SomeOutputClass transform() {
    int someOtherNumber = someNumber * 2;
    return new SomeOutputClass(someStringTransformation(), someOtherNumber, this);
  }

  private String someStringTransformation() {
    return someString.toUpperCase(Locale.getDefault());
  }

}
