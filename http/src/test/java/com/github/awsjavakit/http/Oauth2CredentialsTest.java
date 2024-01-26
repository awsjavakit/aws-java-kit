package com.github.awsjavakit.http;

import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomUri;
import static com.spotify.hamcrest.jackson.JsonMatchers.jsonObject;
import static com.spotify.hamcrest.jackson.JsonMatchers.jsonText;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.List;
import org.junit.jupiter.api.Test;

class Oauth2CredentialsTest {

  @Test
  void shouldKeepTypeWhenInsideACollection() throws JsonProcessingException {
    var entry = new Oauth2Credentials(randomUri(), randomString(), randomString(), randomString());
    var collection = List.of(entry);
    var jsonString = JsonConfig.toJson(collection);
    var jsonArray = (ArrayNode) JsonConfig.JSON.readTree(jsonString);
    var jsonObject = jsonArray.get(0);
    assertThat(jsonObject, is(jsonObject().where("type", is(jsonText(Oauth2Credentials.TYPE)))));

  }

}