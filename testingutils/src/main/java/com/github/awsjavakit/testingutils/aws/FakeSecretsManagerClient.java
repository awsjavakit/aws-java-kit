package com.github.awsjavakit.testingutils.aws;

/**
 * Copied from https://github.com/BIBSYSDEV/nva-commons.
 */

import static com.gtihub.awsjavakit.attempt.Try.attempt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.awsjavakit.misc.JacocoGenerated;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.PutSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.PutSecretValueResponse;

public class FakeSecretsManagerClient implements SecretsManagerClient {

  private final Map<SecretName, String> secrets = new ConcurrentHashMap<>();
  private final ObjectMapper json;

  public FakeSecretsManagerClient(ObjectMapper json) {
    this.json = json;
  }

  /**
   * Deprecated: use putSercetValue instead.
   *
   * @param name  the secretId
   * @param key   the secretKey
   * @param value the value.
   * @return the client for adding more secrets
   */
  //TODO: make private
  @Deprecated(since = "2024-01-10")
  public FakeSecretsManagerClient putSecret(String name, String key, String value) {
    var secretName = new SecretName(name);
    if (secrets.containsKey(secretName)) {
      addSecretValueToExistingSecret(new SecretKey(key), value, secretName);
    } else {
      createNewSecret(key, value, secretName);
    }
    return this;
  }

  /**
   * Deprecated: use putSercetValue instead.
   *
   * @param name  the secretId
   * @param value the value.
   * @return the client for adding more secrets
   */
  @Deprecated(since = "2024-01-10")
  //TODO: make private
  public FakeSecretsManagerClient putPlainTextSecret(String name, String value) {
    var secretName = new SecretName(name);
    secrets.put(secretName, value);
    return this;
  }

  @Override
  public GetSecretValueResponse getSecretValue(GetSecretValueRequest getSecretValueRequest) {
    return Optional.ofNullable(getSecretValueRequest.secretId())
      .map(SecretName::new)
      .flatMap(this::resolveSecret)
      .map(secretContents -> addSecretContents(secretContents, getSecretValueRequest))
      .orElseThrow();
  }

  @Override
  public PutSecretValueResponse putSecretValue(PutSecretValueRequest putSecretValueRequest) {
    attempt(() -> putSecretAsJsonObject(putSecretValueRequest))
      .orElse(fail -> putPlainTextSecret(putSecretValueRequest.secretId(), putSecretValueRequest.secretString()));
    return PutSecretValueResponse.builder().name(putSecretValueRequest.secretId()).build();
  }

  @JacocoGenerated
  @Override
  public String serviceName() {
    return null;
  }

  @JacocoGenerated
  @Override
  public void close() {
    //NO-OP
  }

  private static GetSecretValueResponse addSecretContents(String secretContents,
    GetSecretValueRequest getSecretValueRequest) {
    return GetSecretValueResponse.builder()
      .secretString(secretContents)
      .name(getSecretValueRequest.secretId())
      .build();
  }

  private FakeSecretsManagerClient putSecretAsJsonObject(PutSecretValueRequest putSecretValueRequest)
    throws JsonProcessingException {
    final var secretName = putSecretValueRequest.secretId();
    var objectNode = (ObjectNode) json.readTree(putSecretValueRequest.secretString());
    var keys = objectNode.fieldNames();
    while (keys.hasNext()) {
      var key = keys.next();
      putSecret(secretName, key, asString(objectNode.get(key)));
    }

    //TODO: return Void when public deprecated methods have become private
    return this;
  }

  private String asString(JsonNode jsonNode) {
    return jsonNode.isTextual() ? jsonNode.textValue() : toJson(jsonNode);
  }

  private Optional<String> resolveSecret(SecretName secretName) {
    return Optional.ofNullable(secrets.get(secretName));
  }

  private void createNewSecret(String key, String value, SecretName secretName) {

    var secretContents = new ConcurrentHashMap<SecretKey, String>();

    secretContents.put(new SecretKey(key), value);
    secrets.put(secretName, toJson(secretContents));
  }

  private <T> String toJson(T object) {
    return attempt(() -> json.writeValueAsString(object)).orElseThrow();
  }

  private void addSecretValueToExistingSecret(SecretKey key, String value, SecretName secretName) {
    var jsonString = secrets.get(secretName);
    var object = (ObjectNode) attempt(() -> json.readTree(jsonString))
      .orElse(fail -> json.createObjectNode());
    object.put(key.getValue(), value);
    secrets.put(secretName, toJson(object));
  }

}
