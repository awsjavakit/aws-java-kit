package com.github.awsjavakit.stepfunctions.lambda;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Locale;

public record SomeInputClass(@JsonProperty(value = "someString",required = true) String someString,
                             @JsonProperty(value = "someNumber",required = true)Integer someNumber) {

  public SomeOutputClass transform() {
    var someOtherNumber = someNumber * 2;
    return new SomeOutputClass(someStringTransformation(), someOtherNumber, this);
  }

  private String someStringTransformation() {
    return someString.toUpperCase(Locale.getDefault());
  }

}
