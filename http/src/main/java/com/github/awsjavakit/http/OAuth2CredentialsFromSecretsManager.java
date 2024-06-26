package com.github.awsjavakit.http;

import static java.util.Objects.isNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.githhub.awsjavakit.secrets.SecretsReader;
import java.net.URI;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

/**
 * Class that fetches the OAuthCredentials from SecretsManager. Storing credentials in AWS
 * SecretsManager is a quite common pattern, and this is a sample implementation.
 */
public class OAuth2CredentialsFromSecretsManager implements OAuthCredentialsProvider {

  private final SecretsManagerClient secretsManagerClient;
  private final String secretName;
  private final ObjectMapper json;
  private OAuthCredentialsProvider credentials;

  public OAuth2CredentialsFromSecretsManager(SecretsManagerClient secretsManagerClient,
    String secretName, ObjectMapper json) {

    this.secretsManagerClient = secretsManagerClient;
    this.secretName = secretName;
    this.json = json;
  }

  @Override
  public String getClientId() {
    cacheCredentials();
    return credentials.getClientId();
  }

  @Override
  public String getClientSecret() {
    cacheCredentials();
    return credentials.getClientSecret();
  }

  @Override
  public URI getAuthorizationEndpoint() {
    cacheCredentials();
    return credentials.getAuthorizationEndpoint();
  }

  @Override
  public String getTag() {
    cacheCredentials();
    return credentials.getTag();
  }

  private void cacheCredentials() {
    if (isNull(credentials)) {
      credentials = fetchCredentials();
    }
  }

  private OAuthCredentialsProvider fetchCredentials() {

    var secretsReader = new SecretsReader(secretsManagerClient, json);
    var secret = secretsReader.fetchPlainTextSecret(secretName);
    return Oauth2Credentials.fromJson(secret, json);

  }

}
