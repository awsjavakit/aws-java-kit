package com.github.awsjavakit.eventbridge.models;

import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;

import com.github.awsjavakit.jsonconfig.JsonConfig;
import java.net.URI;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class EventReferenceTest {

  @Test
  void shouldSerializeToJsonObjectThatContainsTopicAndUri() {
    var expectedTopic = randomString();
    var expectedUri = randomUri();
    var eventReference = new EventReference(expectedTopic, expectedUri);
    String json = JsonConfig.writeValueAsString(eventReference);
    assertThat(json, containsString(expectedTopic));
    assertThat(json, containsString(expectedUri.toString()));
  }

  @Test
  void shouldDeSerializeFromJsonObjectWithoutInformationLoss() {
    var expectedTopic = randomString();
    var expectedUri = randomUri();
    var eventReference = new EventReference(expectedTopic, expectedUri);
    EventReference deserializedEventReference =
      EventReference.fromJson(JsonConfig.writeValueAsString(eventReference));
    assertThat(deserializedEventReference, is(equalTo(eventReference)));
  }

  @Test
  void shouldProvideBucketNameContainingTheEvent() {
    var s3uri = URI.create("s3://expected-bucket/path/to/file");
    var eventReference = new EventReference(randomString(), randomString(), s3uri, Instant.now());
    assertThat(eventReference.extractBucketName(), is(equalTo("expected-bucket")));
  }
}