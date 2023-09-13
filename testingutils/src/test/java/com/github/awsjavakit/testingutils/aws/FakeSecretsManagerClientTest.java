package com.github.awsjavakit.testingutils.aws;

import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.githhub.awsjavakit.secrets.SecretsReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class FakeSecretsManagerClientTest {

  private final ObjectMapper JSON = new ObjectMapper();

  @Test
  void shouldBeUsableWithSecretsReader() {
    var secretName = randomString();
    var secretKey1 = randomString();
    var secretValue1 = randomString();
    var secretKey2 = randomString();
    var secretValue2 = randomString();

    var secretsClient =
      new FakeSecretsManagerClient(JSON)
        .putSecret(secretName, secretKey1, secretValue1)
        .putSecret(secretName, secretKey2, secretValue2);
    var secretsReader = new SecretsReader(secretsClient, JSON);
    assertThat(secretsReader.fetchSecret(secretName, secretKey1), is(equalTo(secretValue1)));
    assertThat(secretsReader.fetchSecret(secretName, secretKey2), is(equalTo(secretValue2)));
  }

  @Test
  void shouldBeUsableWithSecretsReaderUsingPlainText() {
    var plainTextSecretName = randomString();
    var plainTextSecretValue = randomString();

    var secretsClient =
      new FakeSecretsManagerClient(JSON).putPlainTextSecret(plainTextSecretName,
        plainTextSecretValue);
    var secretsReader = new SecretsReader(secretsClient, JSON);

    String secretValue = secretsReader.fetchPlainTextSecret(plainTextSecretName);
    assertThat(secretValue, is(equalTo(plainTextSecretValue)));
  }

  @Test
  void shouldThrowExceptionWhenTryingToAddPlainTextSecretWithSameNameAsExistingKeyValueSecret() {
    var secretName = randomString();
    var secretKey = randomString();
    var secretValue = randomString();

    try (var secretsClient = new FakeSecretsManagerClient(JSON)) {
      secretsClient.putSecret(secretName, secretKey, secretValue);

      Executable action = () -> secretsClient.putPlainTextSecret(secretName, secretValue);
      assertThrows(IllegalArgumentException.class, action);
    }
  }

  @Test
  void shouldThrowExceptionWhenTryingToAddKeyValueSecretWithSameNameAsExistingPlainTextSecret() {
    var secretName = randomString();
    var secretKey = randomString();
    var secretValue = randomString();

    try (var secretsClient = new FakeSecretsManagerClient(JSON)) {
      secretsClient.putPlainTextSecret(secretName, secretValue);

      Executable action = () -> secretsClient.putSecret(secretName, secretKey, secretValue);
      assertThrows(IllegalArgumentException.class, action);
    }
  }
}


