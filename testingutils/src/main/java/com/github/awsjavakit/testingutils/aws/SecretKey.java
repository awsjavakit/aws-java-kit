package com.github.awsjavakit.testingutils.aws;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.awsjavakit.misc.JacocoGenerated;
import java.util.Objects;

public final class SecretKey implements Comparable<SecretKey> {

  private final String value;

  @JsonCreator
  public SecretKey(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JacocoGenerated
  @Override
  public int hashCode() {
    return value == null ? 0 : value.hashCode();
  }

  @JacocoGenerated
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SecretKey)) {
      return false;
    }

    SecretKey secretKey = (SecretKey) o;

    return Objects.equals(value, secretKey.value);
  }

  @Override
  public int compareTo(SecretKey other) {
    return this.getValue().compareTo(other.getValue());
  }
}
