package com.github.awsjavakit.eventbridge.models;

import static com.github.awsjavakit.eventbridge.handlers.SampleEventDetail.propertyNamesOfEmptyFields;
import static com.github.awsjavakit.hamcrest.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static com.github.awsjavakit.jsonconfig.JsonConfig.JSON;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.awsjavakit.eventbridge.handlers.SampleEventDetail;
import com.github.awsjavakit.misc.ioutils.IoUtils;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AwsEventBridgeDetailTest {

  public static final String RESPONSE_PAYLOAD_FIELD = "responsePayload";
  private static final String SAMPLE_EVENT_DETAIL = IoUtils.stringFromResources(
    Path.of("validEventBridgeDetailSample.json"));

  @Test
  void objectMapperReturnsAwsEverBridgeDetailObjectForValidJson() throws JsonProcessingException {
    var detail = parseSampleEventDetail();
    assertThat(detail, is(not(nullValue())));
    var emptyFields = propertyNamesOfEmptyFields(RESPONSE_PAYLOAD_FIELD);
    assertThat(detail, doesNotHaveEmptyValuesIgnoringFields(emptyFields));
  }

  @Test
  void copyCreatesEqualObject() throws JsonProcessingException {
    var original = parseSampleEventDetail();
    var copy = original.copy().build();
    assertThat(copy, is(equalTo(original)));
    assertThat(copy, is(not(sameInstance(original))));
  }

  private AwsEventBridgeDetail<SampleEventDetail> parseSampleEventDetail()
    throws JsonProcessingException {
    TypeReference<AwsEventBridgeDetail<SampleEventDetail>> detailTypeReference = new TypeReference<>() {
    };
    return JSON.readValue(SAMPLE_EVENT_DETAIL, detailTypeReference);
  }
}