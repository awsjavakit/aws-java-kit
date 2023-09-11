package com.github.awsjavakit.apigateway;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;



public class ApiGatewayEvent {

  @JsonProperty("body")
  private String body;
  @JsonProperty("resource")
  private String resource;
  @JsonProperty("path")
  private String path;
  @JsonProperty("httpMethod")
  private HttpMethod httpMethod;
  @JsonProperty("isBase64Encoded")
  private Boolean base64Encoded;
  @JsonProperty("queryStringParameters")
  private Map<String, String> queryParameters;
  @JsonProperty("pathParameters")
  private Map<String, String> pathParameters;

  private final Map<String, Object> otherFields;

  public ApiGatewayEvent() {
    this.otherFields = new ConcurrentHashMap<>();
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public String getResource() {
    return resource;
  }

  public void setResource(String resource) {
    this.resource = resource;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public HttpMethod getHttpMethod() {
    return httpMethod;
  }

  public void setHttpMethod(HttpMethod httpMethod) {
    this.httpMethod = httpMethod;
  }

  public Boolean getBase64Encoded() {
    return base64Encoded;
  }

  public void setBase64Encoded(Boolean base64Encoded) {
    this.base64Encoded = base64Encoded;
  }

  public Map<String, String> getQueryParameters() {
    return queryParameters;
  }

  public void setQueryParameters(Map<String, String> queryParameters) {
    this.queryParameters = queryParameters;
  }

  public Map<String, String> getPathParameters() {
    return pathParameters;
  }

  public void setPathParameters(Map<String, String> pathParameters) {
    this.pathParameters = pathParameters;
  }

  @JsonAnySetter
  public void setOtherField(String key, Object value) {
    this.otherFields.put(key, value);
  }

  @JsonAnyGetter
  public Map<String, Object> getOtherField() {
    return this.otherFields;
  }
}
