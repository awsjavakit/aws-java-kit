package com.github.awsjavakit.http;

import static com.github.awsjavakit.http.JsonConfig.JSON;
import static com.github.awsjavakit.http.JsonConfig.toJson;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomUri;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.github.awsjavakit.testingutils.aws.FakeSecretsManagerClient;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.PutSecretValueRequest;

class OAuth2CredentialsFromSecretsManagerTest {

  @Test
  void fetchesCredentialsFromSecrets() {
    var someCredentials = new Oauth2Credentials(randomUri(), randomString(), randomString(),
      randomString());
    var secretName = randomString();
    var secretsClient = new FakeSecretsManagerClient(JSON);
    var request = PutSecretValueRequest.builder()
      .secretString(toJson(someCredentials))
      .secretId(secretName)
      .build();
    secretsClient.putSecretValue(request);
    var credentialsProvider = new OAuth2CredentialsFromSecretsManager(secretsClient, secretName,
      JSON);
    assertThat(credentialsProvider.getClientId()).isEqualTo(someCredentials.getClientId());
    assertThat(credentialsProvider.getClientSecret()).isEqualTo(someCredentials.getClientSecret());
    assertThat(credentialsProvider.getAuthorizationEndpoint()).isEqualTo(someCredentials.getAuthorizationEndpoint());
    assertThat(credentialsProvider.getTag()).isEqualTo(someCredentials.getTag());
  }

  @Test
  void shouldNotEagerlyCallSecretsManager() {
    var someCredentials = new Oauth2Credentials(randomUri(), randomString(), randomString(),
      randomString());
    var secretName = randomString();
    var secretsClient = spy(new FakeSecretsManagerClient(JSON));
    var request = PutSecretValueRequest.builder()
      .secretString(toJson(someCredentials))
      .secretId(secretName)
      .build();
    secretsClient.putSecretValue(request);
    new OAuth2CredentialsFromSecretsManager(secretsClient, secretName, JSON);
    verify(secretsClient,times(0)).getSecretValue(any(GetSecretValueRequest.class));

  }

}