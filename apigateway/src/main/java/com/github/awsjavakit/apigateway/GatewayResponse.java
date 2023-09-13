package com.github.awsjavakit.apigateway;

import static com.gtihub.awsjavakit.attempt.Try.attempt;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.Objects;

public class GatewayResponse {

  @JsonIgnore
  private transient ObjectMapper objectMapper;
  @JsonProperty("isBase64Encoded")
  private Boolean isBase64Encoded = false;
  @JsonProperty("statusCode")
  private int statusCode;
  @JsonProperty("headers")
  private Map<String, String> headers;
  @JsonProperty("body")
  private String body;

  public GatewayResponse() {

  }

  private GatewayResponse(Boolean isBase64Encoded, int statusCode, Map<String, String> headers,
    Object body, ObjectMapper objectMapper) {
    this.isBase64Encoded = isBase64Encoded;
    this.statusCode = statusCode;
    this.headers = headers;
    this.objectMapper = objectMapper;
    this.body = serialize(objectMapper, body);

  }

  public static GatewayResponse create(Object body, int statusCode, Map<String, String> headers,
    ObjectMapper objectMapper) {
    return new GatewayResponse(false, statusCode, headers, body, objectMapper);
  }

  public static GatewayResponse fromOutputStream(ByteArrayOutputStream outputStream,
    ObjectMapper objectMapper) {
    var jsonString = outputStream.toString();
    try {
      return objectMapper.readValue(jsonString, GatewayResponse.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GatewayResponse that = (GatewayResponse) o;
    return statusCode == that.statusCode && Objects.equals(objectMapper, that.objectMapper)
      && Objects.equals(isBase64Encoded, that.isBase64Encoded) && Objects.equals(
      headers, that.headers) && Objects.equals(body, that.body);
  }

  @Override
  public int hashCode() {
    return Objects.hash(objectMapper, isBase64Encoded, statusCode, headers, body);
  }

  public Boolean getBase64Encoded() {
    return isBase64Encoded;
  }

  public void setBase64Encoded(Boolean base64Encoded) {
    isBase64Encoded = base64Encoded;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public void setStatusCode(int statusCode) {
    this.statusCode = statusCode;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public void setHeaders(Map<String, String> headers) {
    this.headers = headers;
  }

  public <I> I getBody(ObjectMapper objectMapper, Class<I> bodyClass) {
    return bodyClass.equals(String.class)
      ? bodyClass.cast(body)
      : attempt(() -> objectMapper.readValue(body, bodyClass)).orElseThrow();
  }

  @JsonIgnore
  public String getBodyString() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public String toJsonString() {
    return attempt(() -> objectMapper.writeValueAsString(this)).orElseThrow();
  }

  private static String serializeBody(ObjectMapper objectMapper, Object object) {
    return attempt(() -> objectMapper.writeValueAsString(object)).orElseThrow();
  }

  private String serialize(ObjectMapper objectMapper, Object object) {
    return object instanceof String string
      ? string
      : serializeBody(objectMapper, object);
  }
}
