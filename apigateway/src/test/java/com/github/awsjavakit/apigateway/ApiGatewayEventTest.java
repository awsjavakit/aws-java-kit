package com.github.awsjavakit.apigateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ApiGatewayEventTest {

  public static final ObjectMapper JSON = new ObjectMapper();

  @Test
  void shouldParseApiGatewayEventVersion1() throws JsonProcessingException {
    var sampleEventString = IoUtils.
      stringFromResources(Path.of("apigateway", "aws-proxy-event.json"));
    var json = JSON.readTree(sampleEventString);
    var parsedEvent = JSON.readValue(sampleEventString, ApiGatewayEvent.class);
    var reserializedEvent = JSON.writeValueAsString(parsedEvent);
    var asJsonAgain = JSON.readTree(reserializedEvent);

    assertThat(asJsonAgain).isEqualTo(json);

  }

}