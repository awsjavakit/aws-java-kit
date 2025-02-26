package com.github.awsjavakit.apigateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.awsjavakit.misc.ioutils.IoUtils;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ApiGatewayEventTest {

  public static final ObjectMapper JSON = new ObjectMapper();
  public static final String SAMPLE_APIGATEWAY_EVENT =
    IoUtils.stringFromResources(Path.of( "aws-proxy-event.json"));
  public static final String APIGATEWAY_EVENT_WITH_NULL_VALUES =
    IoUtils.stringFromResources(Path.of("event-with-null-values.json"));

  @Test
  void shouldParseApiGatewayEventVersion1() throws JsonProcessingException {
    var json = JSON.readTree(SAMPLE_APIGATEWAY_EVENT);
    var parsedEvent = JSON.readValue(SAMPLE_APIGATEWAY_EVENT, ApiGatewayEvent.class);
    var reserializedEvent = JSON.writeValueAsString(parsedEvent);
    var asJsonAgain = JSON.readTree(reserializedEvent);

    assertThat(asJsonAgain).isEqualTo(json);
  }

  @Test
  void shouldBeAbleToHandleNullValues() {
    assertDoesNotThrow(
      () -> JSON.readValue(APIGATEWAY_EVENT_WITH_NULL_VALUES, ApiGatewayEvent.class));
  }
}