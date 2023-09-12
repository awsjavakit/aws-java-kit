package com.github.awsjavakit.apigateway;

import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomInteger;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomJson;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GatewayResponseTest {

  private static final ObjectMapper JSON = new ObjectMapper();

  @Test
  void shouldProvideResponseBodyExactlyAsReceived() {
    var response = new GatewayResponse();
    var expectedResponseBody = randomJson();
    response.setBody(expectedResponseBody);
    assertThat(response.getBodyString()).isEqualTo(expectedResponseBody);
  }

  @Test
  void shouldReturnBodyAsIsWhenBodyIsString() throws JsonProcessingException {
    var body = randomString();
    var response = GatewayResponse.create(body, randomInteger(), randomMap(), JSON);
    var responseJsonString = response.toJsonString();
    var json = JSON.readTree(responseJsonString);
    assertThat(json.get("body").asText()).isEqualTo(body);
  }

  private Map<String, String> randomMap() {
    return Map.of(randomString(), randomString());
  }

}