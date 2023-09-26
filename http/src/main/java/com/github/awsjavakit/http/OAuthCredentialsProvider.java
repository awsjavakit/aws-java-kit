package com.github.awsjavakit.http;

import java.util.Base64;

public interface OAuthCredentialsProvider {

  String getUsername();

  String getPassword();

  default String getAuthorizationHeader(){
    return "Basic " + Base64.getEncoder().encodeToString(formatCredentialsForBasicAuth());
  }

  private byte[] formatCredentialsForBasicAuth() {
    return String.format("%s:%s", getUsername(), getPassword()).getBytes();
  }
}
