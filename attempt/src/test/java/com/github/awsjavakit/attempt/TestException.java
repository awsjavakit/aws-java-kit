package com.github.awsjavakit.attempt;

@SuppressWarnings("PMD.TestClassWithoutTestCases")
public class TestException extends Exception {

  public TestException() {
    super();
  }

  public TestException(String message) {
    super(message);
  }

  public TestException(Exception cause, String message) {
    super(message, cause);
  }
}
