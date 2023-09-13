package com.githhub.awsjavakit.secrets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.invocation.InvocationOnMock;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException;

class SecretsReaderTest {

  static final String SECRET_VALUE = "SECRET_VALUE";
  static final String SECRET_KEY = "SECRET_KEY";
  static final String SECRET_NAME = "SECRET_NAME";
  static final String PLAIN_TEXT_SECRET_NAME = "PLAIN_TEXT_SECRET_NAME";
  static final String PLAIN_TEXT_SECRET_VALUE = "PLAIN_TEXT_SECRET_VALUE";
  static final String JSON_SECRET_NAME = "JSON_SECRET_NAME";
  static final String JSON_SECRET_VALUE = "{\"username\":\"name\", \"password\":\"pass\"}";
  static final String JSON_LIST_SECRET_NAME = "JSON_LIST_SECRET_NAME";
  static final String JSON_LIST_SECRET_VALUE = "[{\"key1\":\"val1\"},{\"key1\":\"val2\"}]";
  static final String JSON_SECRET_VALUE_USERNAME = "name";
  static final String JSON_SECRET_VALUE_PASSWORD = "pass";
  static final String WRONG_SECRET_NAME = "WRONG_SECRET_NAME";
  static final String WRONG_SECRET_KEY = "WRONG_KEY";
  private static final ObjectMapper JSON = new ObjectMapper();
  private final SecretsReader secretsReader;

  SecretsReaderTest() {
    secretsReader = createSecretsReaderMock();
  }

  @Test
  void shouldThrowExceptionContainingTheNonExistingSecretName() {
    Executable action = () -> secretsReader.fetchSecret(WRONG_SECRET_NAME, SECRET_KEY);
    var exception = assertThrows(ErrorReadingSecretException.class, action);
    assertThat(exception.getMessage(), containsString(WRONG_SECRET_NAME));
  }

  @Test
  void shouldThrowExceptionWhenWrongSecretKeyIsGiven() {
    Executable action = () -> secretsReader.fetchSecret(SECRET_NAME, WRONG_SECRET_KEY);
    var exception = assertThrows(ErrorReadingSecretException.class, action);
    assertThat(exception.getMessage(), containsString(WRONG_SECRET_KEY));
  }

  @Test
  void fetchSecretReturnsSecretValueWhenSecretNameAndSecretKeyAreCorrect() {
    var value = secretsReader.fetchSecret(SECRET_NAME, SECRET_KEY);
    assertThat(value, is(equalTo(SECRET_VALUE)));
  }

  @Test
  void shouldReturnInputSecretAsInputClassWhenInputSecretExistsAndIsStructuredLikeInputSecretClass() {
    var credentials = secretsReader.fetchClassSecret(JSON_SECRET_NAME, Credentials.class);
    assertThat(credentials.username(), is(equalTo(JSON_SECRET_VALUE_USERNAME)));
    assertThat(credentials.password(), is(equalTo(JSON_SECRET_VALUE_PASSWORD)));
  }

  @Test
  void shouldNotExposeSecretInThrowWhenInputSecretExistsButIsPlaintext() {
    Executable action = () -> secretsReader.fetchClassSecret(PLAIN_TEXT_SECRET_NAME,
      Credentials.class);
    var thrown = assertThrows(ErrorReadingSecretException.class, action);
    assertThat(thrown.getMessage(), not(containsString(PLAIN_TEXT_SECRET_VALUE)));
  }

  @Test
  void shouldNotExposeSecretInThrowWhenInputSecretExistsButIsJsonList() {
    Executable action = () -> secretsReader.fetchClassSecret(JSON_LIST_SECRET_NAME,
      Credentials.class);
    var thrown = assertThrows(ErrorReadingSecretException.class, action);
    assertThat(thrown.getMessage(), not(containsString(PLAIN_TEXT_SECRET_VALUE)));
  }

  @Test
  void fetchPlainTextSecretWhenSecretNameIsCorrect() {
    var value = secretsReader.fetchPlainTextSecret(PLAIN_TEXT_SECRET_NAME);
    assertThat(value, is(equalTo(PLAIN_TEXT_SECRET_VALUE)));
  }

  @Test
  void shouldSupplyWrongSecretNameWhenFetchingPlainTextAndWrongSecretNameIsGiven() {
    Executable action = () -> secretsReader.fetchPlainTextSecret(WRONG_SECRET_NAME);
    var exception = assertThrows(ErrorReadingSecretException.class, action);
    assertThat(exception.getMessage(), containsString(WRONG_SECRET_NAME));
  }

  private SecretsReader createSecretsReaderMock() {
    var secretsManager = mock(SecretsManagerClient.class);
    when(secretsManager.getSecretValue(any(GetSecretValueRequest.class)))
      .thenAnswer(this::provideGetSecretValueResult);
    return new SecretsReader(secretsManager, JSON);
  }

  private GetSecretValueResponse provideGetSecretValueResult(InvocationOnMock invocation)
    throws JsonProcessingException {
    String providedSecretName = getSecretNameFromRequest(invocation);

    switch (providedSecretName) {
      case SECRET_NAME:
        String secretString = createSecretJsonObject();
        return createGetSecretValueResult(secretString);
      case PLAIN_TEXT_SECRET_NAME:
        return createGetSecretValueResult(PLAIN_TEXT_SECRET_VALUE);
      case JSON_SECRET_NAME:
        return createGetSecretValueResult(JSON_SECRET_VALUE);
      case JSON_LIST_SECRET_NAME:
        return createGetSecretValueResult(JSON_LIST_SECRET_VALUE);
      default:
        throw ResourceNotFoundException.create("Secret not found",
          new RuntimeException("exception in mock"));
    }

  }

  private String getSecretNameFromRequest(InvocationOnMock invocation) {
    GetSecretValueRequest request = invocation.getArgument(0);
    return request.secretId();
  }

  private GetSecretValueResponse createGetSecretValueResult(String secretString) {
    return GetSecretValueResponse.builder()
      .secretString(secretString)
      .name(SECRET_NAME)
      .build();
  }

  private String createSecretJsonObject() throws JsonProcessingException {
    var secret = Map.of(SECRET_KEY, SECRET_VALUE);
    return JSON.writeValueAsString(secret);
  }

}