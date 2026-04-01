package com.github.awsjavakit.http;

import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.StringNode;

class Oauth2CredentialsTest {

  @Test
  void shouldKeepTypeWhenInsideACollection() {
    var entry = new Oauth2Credentials(randomUri(), randomString(), randomString(), randomString());
    var collection = List.of(entry);
    var jsonString = JsonConfig.toJson(collection);
    var jsonArray = (ArrayNode) JsonConfig.JSON.readTree(jsonString);
    var jsonObject = jsonArray.get(0);
    assertThat(jsonObject.get("type"), is(instanceOf(StringNode.class)));
    assertThat(jsonObject.get("type").stringValue(), is(equalTo(Oauth2Credentials.TYPE)));
  }

  @Test
  void shouldBeEqualToEquivalentCredentials() {
    var uri = randomUri();
    var clientId = randomString();
    var clientSecret = randomString();
    var tag = randomString();
    var left = new Oauth2Credentials(uri, clientId, clientSecret, tag);
    var right = new Oauth2Credentials(uri, clientId, clientSecret, tag);

    assertThat(left, is(equalTo(right)));
    assertThat(left.hashCode(), is(equalTo(right.hashCode())));
  }

}