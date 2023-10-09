package com.github.awsjavakit.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.githhub.awsjavakit.secrets.SecretsReader;
import java.net.URI;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

/**
 * Class that fetches the OAuthCredentials from SecretsManager.
 * Storing credentials in AWS SecretsManager is a quite common pattern,
 * and this is a sample implementation.
 */
public class OAuth2CredentialsFromSecretsManager implements OAuthCredentialsProvider {

  private final Oauth2Credentials credentials;

  public OAuth2CredentialsFromSecretsManager(SecretsManagerClient secretsManagerClient,
    String secretName, ObjectMapper json) {
    var secretsReader = new SecretsReader(secretsManagerClient, json);
    var secret = secretsReader.fetchPlainTextSecret(secretName);
    this.credentials = Oauth2Credentials.fromJson(secret, json);

  }

  @Override
  public String getClientId() {
    return credentials.clientId();
  }

  @Override
  public String getClientSecret() {
    return credentials.clientSecret();
  }

  @Override
  public URI getAuthEndpointUri() {
    return credentials.authEndpointUri();
  }


}
