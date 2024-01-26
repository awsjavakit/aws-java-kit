package com.github.awsjavakit.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.githhub.awsjavakit.secrets.SecretsReader;
import java.net.URI;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

/**
 * Class that fetches the OAuthCredentials from SecretsManager. Storing credentials in AWS
 * SecretsManager is a quite common pattern, and this is a sample implementation.
 */
public class OAuth2CredentialsFromSecretsManager implements OAuthCredentialsProvider {

  private final OAuthCredentialsProvider credentials;

  public OAuth2CredentialsFromSecretsManager(SecretsManagerClient secretsManagerClient,
    String secretName, ObjectMapper json) {
    var secretsReader = new SecretsReader(secretsManagerClient, json);
    var secret = secretsReader.fetchPlainTextSecret(secretName);
    this.credentials = Oauth2Credentials.fromJson(secret, json);

  }

  @Override
  public String getClientId() {
    return credentials.getClientId();
  }

  @Override
  public String getClientSecret() {
    return credentials.getClientSecret();
  }

  @Override
  public URI getAuthorizationEndpoint() {
    return credentials.getAuthorizationEndpoint();
  }

  @Override
  public String getTag() {
    return credentials.getTag();
  }

}
