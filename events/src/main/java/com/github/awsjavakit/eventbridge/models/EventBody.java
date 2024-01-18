package com.github.awsjavakit.eventbridge.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface EventBody {

  String TOPIC = "topic";

  @JsonProperty(TOPIC)
  String getTopic();
}
