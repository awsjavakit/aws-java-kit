package com.github.awsjavakit.eventbridge.models;

import com.github.awsjavakit.misc.JacocoGenerated;
import java.util.Objects;

public class AwsEventBridgeResponseContext {

  private Integer statusCode;
  private String executedVersion;

  @JacocoGenerated
  public Integer getStatusCode() {
    return statusCode;
  }

  @JacocoGenerated
  public void setStatusCode(Integer statusCode) {
    this.statusCode = statusCode;
  }

  @JacocoGenerated
  public String getExecutedVersion() {
    return executedVersion;
  }

  @JacocoGenerated
  public void setExecutedVersion(String executedVersion) {
    this.executedVersion = executedVersion;
  }

  @Override
  @JacocoGenerated
  public int hashCode() {
    return Objects.hash(getStatusCode(), getExecutedVersion());
  }

  @Override
  @JacocoGenerated
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AwsEventBridgeResponseContext that = (AwsEventBridgeResponseContext) o;
    return Objects.equals(getStatusCode(), that.getStatusCode())
      && Objects.equals(getExecutedVersion(), that.getExecutedVersion());
  }
}
