package com.github.awsjavakit.testingutils.aws;

import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static com.gtihub.awsjavakit.attempt.Try.attempt;
import static com.spotify.hamcrest.jackson.JsonMatchers.jsonObject;
import static com.spotify.hamcrest.jackson.JsonMatchers.jsonText;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.githhub.awsjavakit.secrets.SecretsReader;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.PutSecretValueRequest;

class FakeSecretsManagerClientTest {

  private static final ObjectMapper JSON = new ObjectMapper();
  private FakeSecretsManagerClient secretsClient;

  @BeforeEach
  public void init() {
    this.secretsClient = new FakeSecretsManagerClient(JSON);
  }

  @Test
  void shouldBeUsableWithSecretsReader() {
    var secretName = randomString();
    var secretKey1 = randomString();
    var secretValue1 = randomString();
    var secretKey2 = randomString();
    var secretValue2 = randomString();

    secretsClient.putSecretValue(createRequest(secretName, secretKey1, secretValue1));
    secretsClient.putSecretValue(createRequest(secretName, secretKey2, secretValue2));

    var secretsReader = new SecretsReader(secretsClient, JSON);
    assertThat(secretsReader.fetchSecret(secretName, secretKey1), is(equalTo(secretValue1)));
    assertThat(secretsReader.fetchSecret(secretName, secretKey2), is(equalTo(secretValue2)));
  }

  @Test
  void shouldBeUsableWithSecretsReaderUsingPlainText() {
    var plainTextSecretName = randomString();
    var plainTextSecretValue = randomString();

    secretsClient.putSecretValue(createRequest(plainTextSecretName, plainTextSecretValue));

    var secretsReader = new SecretsReader(secretsClient, JSON);

    var secretValue = secretsReader.fetchPlainTextSecret(plainTextSecretName);
    assertThat(secretValue, is(equalTo(plainTextSecretValue)));
  }

  @Test
  void shouldReplaceWholeSecretWhenReplacingAJsonLikeSecretWithPlainTextSecret() {
    var secretName = randomString();
    var secretKey = randomString();
    var secretValue = randomString();

    secretsClient.putSecretValue(createRequest(secretName, secretKey, secretValue));
    var plainTextValue = randomString();
    secretsClient.putSecretValue(createRequest(secretName, plainTextValue));
    var actualValue = fetchSecret(secretName, secretsClient);
    assertThat(actualValue, is(equalTo(plainTextValue)));

  }

  @Test
  void shouldReplaceSecretWhenAddingAJsonSecretInThePlainTextSecret()
    throws JsonProcessingException {
    var secretName = randomString();
    var secretKey = randomString();
    var secretValue = randomString();
    var plainTextValue = randomString();

    secretsClient.putSecretValue(createRequest(secretName, plainTextValue));
    secretsClient.putSecretValue(createRequest(secretName, secretKey, secretValue));
    var actualValue = fetchSecret(secretName, secretsClient);
    var node = (ObjectNode) JSON.readTree(actualValue);
    assertThat(node, is(jsonObject().where(secretKey, is(jsonText(secretValue)))));

  }

  //TODO: addressing a weird Exception where Jackson complains that these values are not implemtning Comparable
  //     The issue appears only in client code.
  @Test
  void shouldUseHelpingClassedThatImplementComparable(){
    var secrteKey = new SecretKey(randomString());
    var secretValue = new SecretName(randomString());
    assertThat(secrteKey.compareTo(secrteKey),is(equalTo(0)));
    assertThat(secretValue.compareTo(secretValue),is(equalTo(0)));
  }

  private static String fetchSecret(String secretName, FakeSecretsManagerClient secretsClient) {
    var request = GetSecretValueRequest.builder()
      .secretId(secretName)
      .build();
    return secretsClient.getSecretValue(request).secretString();
  }

  private PutSecretValueRequest createRequest(String secretName, String secretKey1,
    String secretValue1) {
    return PutSecretValueRequest.builder()
      .secretId(secretName)
      .secretString(toJson(Map.of(secretKey1, secretValue1)))
      .build();
  }

  private <T> String toJson(T object) {
    return attempt(() -> JSON.writeValueAsString(object)).orElseThrow();
  }

  private PutSecretValueRequest createRequest(String plainTextSecretName,
    String plainTextSecretValue) {
    return PutSecretValueRequest.builder()
      .secretId(plainTextSecretName)
      .secretString(plainTextSecretValue)
      .build();
  }
}




