package com.github.awsjavakit.testingutils;

import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.awsjavakit.apigateway.ApiGatewayEvent;
import com.github.awsjavakit.apigateway.HttpMethod;
import com.github.awsjavakit.misc.ioutils.IoUtils;
import com.github.awsjavakit.misc.paths.UnixPath;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ApiGatewayRequestBuilderTest {

  public static final String BODY_FIELD = "body";
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
      .withMethod(HttpMethod.POST)
      .build();
    var generatedAsString = IoUtils.streamToString(generatedInputStream);
    var generatedDeserialized = JSON.readValue(generatedAsString, ApiGatewayEvent.class);

    assertThat(generatedDeserialized)
      .usingRecursiveComparison()
      .comparingOnlyFields("path", BODY_FIELD, "queryParameters", "headers", "multiValueHeader",
        "method")
      .isEqualTo(deserialized);
  }

  @Test
  void shouldWriteBodyAsIsWhenItIsString() throws IOException {
    var expectedBodyContent = randomString();
    var event = ApiGatewayRequestBuilder.create(JSON)
      .withBody(expectedBodyContent)
      .build();
    var json = JSON.readTree(event);

    assertThat(json.get(BODY_FIELD).isTextual()).isTrue();
    assertThat(json.get(BODY_FIELD).asText()).isEqualTo(expectedBodyContent);
  }

  @Test
  void shouldWriteBodyAsValidJsonStringWhenBodyIsAnObject() throws IOException {
    var expectedBodyContent = new SampleInput(randomString(), randomString());
    var event = ApiGatewayRequestBuilder.create(JSON)
      .withBody(expectedBodyContent)
      .build();
    var json = JSON.readTree(event);
    var bodyString = json.get(BODY_FIELD).textValue();
    var deserializedBody = JSON.readValue(bodyString, SampleInput.class);

    assertThat(json.get(BODY_FIELD).isTextual()).isTrue();
    assertThat(deserializedBody).isEqualTo(expectedBodyContent);
  }

  @Test
  void shouldSetBodyToNullWhenBodyIsNull() throws IOException {
    var event = ApiGatewayRequestBuilder.create(JSON)
      .withBody(null)
      .build();
    var json = JSON.readTree(event);
    assertThat(json.get(BODY_FIELD).isNull()).isTrue();
  }

  private record SampleInput(String fieldA, String fieldB) {

  }
}