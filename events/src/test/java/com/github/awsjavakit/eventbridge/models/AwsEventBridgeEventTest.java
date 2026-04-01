package com.github.awsjavakit.eventbridge.models;

import static com.github.awsjavakit.eventbridge.handlers.SampleEventDetail.propertyNamesOfEmptyFields;
import static com.github.awsjavakit.hamcrest.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static com.github.awsjavakit.jsonconfig.JsonConfig.JSON;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

import tools.jackson.core.type.TypeReference;
import com.github.awsjavakit.eventbridge.handlers.SampleEventDetail;
import com.github.awsjavakit.misc.ioutils.IoUtils;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AwsEventBridgeEventTest {

  private static final String EVENT_JSON = IoUtils.stringFromResources(
    Path.of("validEventBridgeEvent.json"));

  @Test
  void objectMapperReturnsAwsEverBridgeDetailObjectForValidJson() {
    var event = parseEvent();
    assertThat(event, is(not(nullValue())));
    assertThat(event,

      doesNotHaveEmptyValuesIgnoringFields(propertyNamesOfEmptyFields("detail")));
  }

  @Test
  void equalsReturnsTrueForEquivalentFields() {
    var left = parseEvent();
    var right = parseEvent();
    assertThat(left, is(equalTo(right)));
  }

  @Test
  void shouldReturnValidJsonStringAsStringRepresentation() {
    var expected = parseEvent();
    var actual = parseEvent(expected.toString());
    assertThat(actual.toString(), is(equalTo(expected.toString())));
  }

  private AwsEventBridgeEvent<SampleEventDetail> parseEvent(){

    return parseEvent(EVENT_JSON);
  }

  private AwsEventBridgeEvent<SampleEventDetail> parseEvent(String eventString) {
    TypeReference<AwsEventBridgeEvent<SampleEventDetail>> detailTypeReference =
      new TypeReference<>() {
      };
    return JSON.readValue(eventString, detailTypeReference);
  }
}