package com.github.awsjavakit.apigateway;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

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
    this.body = serializeObject(objectMapper, body);

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
    try {
      return objectMapper.readValue(body, bodyClass);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

  }

  public void setBody(String body) {
    this.body = body;
  }

  public String toJsonString() {
    try {
      return objectMapper.writeValueAsString(this);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private String serializeObject(ObjectMapper objectMapper, Object object) {
    try {
      return objectMapper.writeValueAsString(object);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

}
