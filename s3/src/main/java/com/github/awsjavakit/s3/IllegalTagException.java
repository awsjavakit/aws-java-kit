package com.github.awsjavakit.s3;

public class IllegalTagException extends RuntimeException {

  public IllegalTagException(String tagString) {
    super(createMessage(tagString));
  }

  private static String createMessage(String tagString) {
    return String.format("Illegal tag: %s", tagString);
  }
}
