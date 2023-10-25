package com.github.awsjavakit.http.token;

import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomInteger;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class OAuthTokenResponseTest {

  private static final ObjectMapper JSON = JsonMapper.builder()
    .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
    .build();

  @Test
  void shouldAcceptEntryWithOtherFieldsEvenWhenJsonIsConfiguredOtherWise()
    throws JsonProcessingException {
    var accessToken = randomString();
    var validityPeriod = (long) randomInteger();
    var json = entryWithUnknownFields(accessToken, validityPeriod);
    var deserialized = JSON.readValue(json, OAuthTokenResponse.class);
    assertThat(deserialized.getAccessToken()).isEqualTo(accessToken);
    assertThat(deserialized.getValidityPeriodInSeconds()).isEqualTo(validityPeriod);

  }

  @ParameterizedTest
  @ValueSource(strings = {"access_token", "expires_in"})
  void shouldThrowExceptionWhenRequiredFieldIsMissing(String missingField)
    throws JsonProcessingException {
    var accessToken = randomString();
    var validityPeriod = (long) randomInteger();
    var json = entryWithMissingFields(accessToken, validityPeriod, missingField);
    Executable action = () -> JSON.readValue(json, OAuthTokenResponse.class);
    var exception = assertThrows(Exception.class, action);
    assertThat(exception.getMessage()).contains(missingField);

  }

  private static String entryWithUnknownFields(String accessToken, long validityPeriod)
    throws JsonProcessingException {
    var entry = JSON.createObjectNode();
    entry.put("access_token", accessToken);
    entry.put("expires_in", validityPeriod);
    entry.put(randomString(), randomString());
    return JSON.writeValueAsString(entry);
  }

  private String entryWithMissingFields(String accessToken,
    long validityPeriod,
    String missingField) throws JsonProcessingException {
    var entry = JSON.createObjectNode();
    entry.put("access_token", accessToken);
    entry.put("expires_in", validityPeriod);
    entry.remove(missingField);
    return JSON.writeValueAsString(entry);
  }

}