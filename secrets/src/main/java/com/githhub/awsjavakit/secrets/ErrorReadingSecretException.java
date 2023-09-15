package com.githhub.awsjavakit.secrets;

public class ErrorReadingSecretException extends RuntimeException {

  public ErrorReadingSecretException(String secretName, Throwable cause) {
    super(errorMessage(secretName), cause);
  }

  private static String errorMessage(String secretName) {
    return String.format("Could not read secret: %s", secretName);
  }
}
