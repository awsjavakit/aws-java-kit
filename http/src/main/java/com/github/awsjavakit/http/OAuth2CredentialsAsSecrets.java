package com.github.awsjavakit.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.githhub.awsjavakit.secrets.SecretsReader;
import java.net.URI;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

public class OAuth2CredentialsAsSecrets implements OAuthCredentialsProvider {

  private final  Oauth2Credentials credentials;

  public OAuth2CredentialsAsSecrets(SecretsManagerClient secretsManagerClient,
    String secretName, ObjectMapper json){
    var secretsReader = new SecretsReader(secretsManagerClient,json);
    var secret= secretsReader.fetchPlainTextSecret(secretName);
    this.credentials = Oauth2Credentials.fromJson(secret,json);

  }

  @Override
  public String getUsername() {
    return credentials.clientId();
  }

  @Override
  public String getPassword() {
    return credentials.clientSecret();
  }

  @Override
  public URI getAuthServerUri() {
    return credentials.serverUri();
  }
}
