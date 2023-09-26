package com.github.awsjavakit.eventbridge.models;

import static com.github.awsjavakit.eventbridge.handlers.SampleEventDetail.propertyNamesOfEmptyFields;
import static com.github.awsjavakit.hamcrest.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static com.github.awsjavakit.jsonconfig.JsonConfig.JSON;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.awsjavakit.eventbridge.handlers.SampleEventDetail;
import com.github.awsjavakit.misc.ioutils.IoUtils;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AwsEventBridgeEventTest {

    private static final String EVENT_JSON = IoUtils.stringFromResources(Path.of("validEventBridgeEvent.json"));

    @Test
    void objectMapperReturnsAwsEverBridgeDetailObjectForValidJson() throws JsonProcessingException {
        var event = parseEvent();
        assertThat(event, is(not(nullValue())));
        assertThat(event,

                   doesNotHaveEmptyValuesIgnoringFields(propertyNamesOfEmptyFields("detail")));
    }

    @Test
    void equalsReturnsTrueForEquivalentFields() throws JsonProcessingException {
        var left = parseEvent();
        var right = parseEvent();
        assertThat(left, is(equalTo(right)));
    }

    @Test
    void shouldReturnValidJsonStringAsStringRepresentation() throws JsonProcessingException {
        var expected = parseEvent();
        var actual = parseEvent(expected.toString());
        assertThat(actual.toString(), is(equalTo(expected.toString())));
    }

    private AwsEventBridgeEvent<SampleEventDetail> parseEvent()
        throws JsonProcessingException {

        return parseEvent(EVENT_JSON);
    }

    private AwsEventBridgeEvent<SampleEventDetail> parseEvent(String eventString)
        throws JsonProcessingException {
        TypeReference<AwsEventBridgeEvent<SampleEventDetail>> detailTypeReference =
            new TypeReference<>() {
            };
        return JSON.readValue(eventString, detailTypeReference);
    }
}