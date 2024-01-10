package com.github.awsjavakit.testingutils.aws;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.awsjavakit.misc.JacocoGenerated;
import java.util.Objects;

public final class SecretName implements Comparable<SecretName> {

  private final String value;

  @JsonCreator
  SecretName(String value) {
    this.value = value;
  }

  @JacocoGenerated
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
    if (!(o instanceof SecretName)) {
      return false;
    }

    var that = (SecretName) o;

    return Objects.equals(value, that.value);
  }

  @Override
  public int compareTo(SecretName o) {
    return this.getValue().compareTo(o.getValue());
  }
}
