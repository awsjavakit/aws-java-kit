package com.github.awsjavakit.http.token;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.Instant;

@JsonTypeInfo(use = Id.NAME, include = As.EXISTING_PROPERTY, property = "type")
@JsonTypeName(OAuthTokenEntry.TYPE)
public record OAuthTokenEntry(
  @JsonProperty("value") String value,
  @JsonProperty("timestamp") Instant timestamp
) {

  public static final String TYPE = "OAuthToken";

  @JsonProperty(value = "type", access = Access.READ_ONLY)
  public String type() {
    return TYPE;
  }

}
