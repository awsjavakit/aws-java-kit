package com.github.awsjavakit.apigateway;

import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomJson;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GatewayResponseTest {

  @Test
  void shouldProvideResponseBodyExactlyAsReceived() {
    var response = new GatewayResponse();
    String expectedResponseBody = randomJson();
    response.setBody(expectedResponseBody);
    assertThat(response.getBodyString()).isEqualTo(expectedResponseBody);
  }

}