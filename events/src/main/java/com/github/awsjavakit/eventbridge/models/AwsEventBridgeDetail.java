package com.github.awsjavakit.eventbridge.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.awsjavakit.misc.JacocoGenerated;
import java.util.Objects;

@JsonIgnoreProperties({"requestPayload", "region"})
public final class AwsEventBridgeDetail<I> {

  @JsonProperty("version")
  private String version;
  @JsonProperty("timestamp")
  private String timestamp;
  @JsonProperty("requestContext")
  private JsonNode requestContext;
  @JsonProperty("responseContext")
  private AwsEventBridgeResponseContext responseContext;
  @JsonProperty("responsePayload")
  private I responsePayload;

  public AwsEventBridgeDetail() {

  }

  private AwsEventBridgeDetail(Builder<I> builder) {
    setVersion(builder.version);
    setTimestamp(builder.timestamp);
    setRequestContext(builder.requestPayload);
    setResponseContext(builder.responseContext);
    setResponsePayload(builder.responsePayload);
  }

  public static <I> Builder<I> newBuilder() {
    return new Builder<>();
  }

  public Builder<I> copy() {
    return AwsEventBridgeDetail.<I>newBuilder()
      .withVersion(this.getVersion())
      .withTimestamp(this.getTimestamp())
      .withRequestPayload(this.getRequestContext())
      .withResponseContext(this.getResponseContext())
      .withResponsePayload(this.getResponsePayload());
  }

  @JacocoGenerated
  @Override
  public int hashCode() {
    return Objects.hash(getVersion(), getTimestamp(), getRequestContext(), getResponseContext(),
      getResponsePayload());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AwsEventBridgeDetail<?> that = (AwsEventBridgeDetail<?>) o;
    return Objects.equals(getVersion(), that.getVersion())
      && Objects.equals(getTimestamp(), that.getTimestamp())
      && Objects.equals(getRequestContext(), that.getRequestContext())
      && Objects.equals(getResponseContext(), that.getResponseContext())
      && Objects.equals(getResponsePayload(), that.getResponsePayload());
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  public JsonNode getRequestContext() {
    return requestContext;
  }

  public  void setRequestContext(JsonNode requestContext) {
    this.requestContext = requestContext;
  }

  public AwsEventBridgeResponseContext getResponseContext() {
    return responseContext;
  }

  public void setResponseContext(AwsEventBridgeResponseContext responseContext) {
    this.responseContext = responseContext;
  }

  public I getResponsePayload() {
    return responsePayload;
  }

  public void setResponsePayload(I responsePayload) {
    this.responsePayload = responsePayload;
  }

  public static final class Builder<I> {

    private String version;
    private String timestamp;
    private JsonNode requestPayload;
    private AwsEventBridgeResponseContext responseContext;
    private I responsePayload;

    private Builder() {
    }

    public Builder<I> withVersion(String version) {
      this.version = version;
      return this;
    }

    public Builder<I> withTimestamp(String timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public Builder<I> withRequestPayload(JsonNode requestPayload) {
      this.requestPayload = requestPayload;
      return this;
    }

    public Builder<I> withResponseContext(AwsEventBridgeResponseContext responseContext) {
      this.responseContext = responseContext;
      return this;
    }

    public Builder<I> withResponsePayload(I responsePayload) {
      this.responsePayload = responsePayload;
      return this;
    }

    public AwsEventBridgeDetail<I> build() {
      return new AwsEventBridgeDetail<>(this);
    }
  }
}
