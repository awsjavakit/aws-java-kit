package com.github.awsjavakit.testingutils.networking;

import com.github.awsjavakit.misc.JacocoGenerated;
import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import com.github.tomakehurst.wiremock.http.Cookie;
import com.github.tomakehurst.wiremock.http.FormParameter;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.QueryParameter;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class CustomRequest implements Request {

  private final Request request;
  private final Map<String, FormParameter> formParametersMap;

  public CustomRequest(Request request, Map<String,FormParameter> formParameters){
    this.request = request;
    this.formParametersMap = formParameters;
  }

  @Override
  @JacocoGenerated
  public String getUrl() {
    return request.getUrl();
  }

  @Override
  @JacocoGenerated
  public String getAbsoluteUrl() {
    return request.getAbsoluteUrl();
  }

  @Override
  @JacocoGenerated
  public RequestMethod getMethod() {
    return request.getMethod();
  }

  @Override
  @JacocoGenerated
  public String getScheme() {
    return request.getScheme();
  }

  @Override
  @JacocoGenerated
  public String getHost() {
    return request.getHost();
  }

  @Override
  @JacocoGenerated
  public int getPort() {
    return request.getPort();
  }

  @Override
  @JacocoGenerated
  public String getClientIp() {
    return request.getClientIp();
  }

  @Override
  @JacocoGenerated
  public String getHeader(String key) {
    return request.getHeader(key);
  }

  @Override
  @JacocoGenerated
  public HttpHeader header(String key) {
    return request.header(key);
  }

  @Override
  @JacocoGenerated
  public ContentTypeHeader contentTypeHeader() {
    return request.contentTypeHeader();
  }

  @Override
  @JacocoGenerated
  public HttpHeaders getHeaders() {
    return request.getHeaders();
  }

  @Override
  @JacocoGenerated
  public boolean containsHeader(String key) {
    return request.containsHeader(key);
  }

  @Override
  @JacocoGenerated
  public Set<String> getAllHeaderKeys() {
    return request.getAllHeaderKeys();
  }

  @Override
  @JacocoGenerated
  public QueryParameter queryParameter(String key) {
    return request.queryParameter(key);
  }

  @Override
  @JacocoGenerated
  public FormParameter formParameter(String key) {
    return formParametersMap.get(key);
  }

  @Override
  @JacocoGenerated
  public Map<String, FormParameter> formParameters() {
    return formParametersMap;
  }

  @Override
  @JacocoGenerated
  public Map<String, Cookie> getCookies() {
    return request.getCookies();
  }

  @Override
  @JacocoGenerated
  public byte[] getBody() {
    return request.getBody();
  }

  @Override
  @JacocoGenerated
  public String getBodyAsString() {
    return request.getBodyAsString();
  }

  @Override
  @JacocoGenerated
  public String getBodyAsBase64() {
    return request.getBodyAsBase64();
  }

  @Override
  @JacocoGenerated
  public boolean isMultipart() {
    return request.isMultipart();
  }

  @Override
  @JacocoGenerated
  public Collection<Part> getParts() {
    return request.getParts();
  }

  @Override
  @JacocoGenerated
  public Part getPart(String name) {
    return request.getPart(name);
  }

  @Override
  @JacocoGenerated
  public boolean isBrowserProxyRequest() {
    return request.isBrowserProxyRequest();
  }

  @Override
  @JacocoGenerated
  public Optional<Request> getOriginalRequest() {
    return request.getOriginalRequest();
  }

  @Override
  @JacocoGenerated
  public String getProtocol() {
    return request.getProtocol();
  }
}
