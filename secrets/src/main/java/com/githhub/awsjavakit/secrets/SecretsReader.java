package com.githhub.awsjavakit.secrets;

import static com.gtihub.awsjavakit.attempt.Try.attempt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gtihub.awsjavakit.attempt.Failure;
import com.gtihub.awsjavakit.attempt.Try;

import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

public class SecretsReader {

  private static final String SECRET_NAME_KEY_DELIMITER = ":";
  private final SecretsManagerClient awsSecretsManager;
  private final ObjectMapper json;

  public SecretsReader(SecretsManagerClient awsSecretsManager, ObjectMapper objectMapper) {
    this.awsSecretsManager = awsSecretsManager;
    this.json = objectMapper;
  }

  /**
   * Fetches a secret String from AWS Secrets Manager.
   *
   * @param secretName the user-friendly id of the secret or the secret ARN
   * @param secretKey  the key in the encrypted key-value map.
   * @return the value for the specified key
   * @throws ErrorReadingSecretException when any error occurs.
   */
  public String fetchSecret(String secretName, String secretKey) {

    return attempt(() -> fetchSecretFromAws(secretName))
      .map(fetchResult -> extractKey(fetchResult, secretKey, secretName))
      .orElseThrow(fail->errorReadingSecret(fail,secretName,secretKey));
  }



  /**
   * Fetches a plain-text secret from AWS Secrets Manager.
   *
   * @param secretName the user-friendly id of the secret or the secret ARN
   * @return the plain text value for the specified secret name
   * @throws ErrorReadingSecretException when any error occurs.
   */
  public String fetchPlainTextSecret(String secretName) {

    return attempt(() -> fetchSecretFromAws(secretName))
      .map(GetSecretValueResponse::secretString)
      .orElseThrow(fail->errorReadingSecret(fail,secretName));
  }

  /**
   * Fetches a json secret from AWS Secrets Manager as a class.
   *
   * @param secretName the user-friendly id of the secret or the secret ARN
   * @param tclass     the class or interface of the class to be returned
   * @param <T>        the type of the class or interface of the class to be returned
   * @return Class of the object we want to extract the secret to
   * @throws ErrorReadingSecretException when any error occurs.
   */
  public <T> T fetchClassSecret(String secretName, Class<T> tclass) {

    return attempt(() -> fetchSecretFromAws(secretName))
      .map(GetSecretValueResponse::secretString)
      .map(str -> json.readValue(str, tclass))
      .orElseThrow((Failure<T> fail) -> errorReadingSecret(fail, secretName));
  }

  private GetSecretValueResponse fetchSecretFromAws(String secretName) {
    return awsSecretsManager
      .getSecretValue(GetSecretValueRequest.builder().secretId(secretName).build());
  }

  private String extractKey(GetSecretValueResponse getSecretResult, String secretKey,
    String secretName) {

    return Try.of(getSecretResult)
      .map(GetSecretValueResponse::secretString)
      .flatMap(this::readStringAsJsonObject)
      .map(secretJson -> secretJson.get(secretKey))
      .map(JsonNode::textValue)
      .orElseThrow((Failure<String> fail) -> errorReadingSecret(fail, secretName, secretKey));
  }

  private <T> ErrorReadingSecretException errorReadingSecret(Failure<T> fail,
    String... secretDetails) {
    var secretDetailsString = String.join(SECRET_NAME_KEY_DELIMITER, secretDetails);
    return new ErrorReadingSecretException(secretDetailsString, fail.getException());
  }

  private Try<JsonNode> readStringAsJsonObject(String secretString) {
    return attempt(() -> json.readTree(secretString));
  }

}