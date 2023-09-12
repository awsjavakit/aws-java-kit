package com.github.awsjavakit.testingutils;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.awsjavakit.apigateway.ApiGatewayEvent;
import com.github.awsjavakit.misc.ioutils.IoUtils;
import com.github.awsjavakit.misc.paths.UnixPath;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ApiGatewayRequestBuilderTest {

  private static final ObjectMapper JSON = new ObjectMapper();

  @Test
  void shouldGenerateValidEvent() throws JsonProcessingException {
    var sampleEvent = IoUtils.stringFromResources(Path.of("apigateway",
      "aws-proxy-event.json"));
    var deserialized = JSON.readValue(sampleEvent, ApiGatewayEvent.class);
    var generatedInputStream = ApiGatewayRequestBuilder
      .create(JSON)
      .withQueryParameters(deserialized.getQueryParameters())
      .withHeaders(deserialized.getHeaders())
      .withBody(deserialized.getBody())
      .withPath(UnixPath.fromString(deserialized.getPath()))
      .build();
    var generatedAsString = IoUtils.streamToString(generatedInputStream);
    var generatedDeserialized = JSON.readValue(generatedAsString, ApiGatewayEvent.class);

    assertThat(generatedDeserialized.getPath()).isEqualTo(deserialized.getPath());
    assertThat(generatedDeserialized.getBody()).isEqualTo(deserialized.getBody());
    assertThat(generatedDeserialized.getQueryParameters()).isEqualTo(
      deserialized.getQueryParameters());
    assertThat(generatedDeserialized.getHeaders()).isEqualTo(deserialized.getHeaders());
    assertThat(generatedDeserialized.getMultiValueHeaders().keySet()).isEqualTo(
      deserialized.getMultiValueHeaders().keySet());
    for (var header : generatedDeserialized.getMultiValueHeaders().keySet()) {
      assertThat(generatedDeserialized.getMultiValueHeaders().get(header))
        .isEqualTo(deserialized.getMultiValueHeaders().get(header));
    }
  }

}