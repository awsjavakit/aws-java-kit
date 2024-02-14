package com.github.awsjavakit.stepfunctions.lambda;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonSubTypes(value = {
  @JsonSubTypes.Type(value = SubTypeA.class, name = SubTypeA.TYPE),
  @JsonSubTypes.Type(value = SubTypeB.class, name = SubTypeB.TYPE)
})
@JsonTypeInfo(use= Id.NAME,include = As.EXISTING_PROPERTY,property = "type")
public interface BaseType {


  @JsonProperty(value="type",access = Access.READ_ONLY)
  String getType();

}
