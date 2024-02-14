package com.github.awsjavakit.stepfunctions.lambda;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeInfo(use = Id.NAME, include = As.EXISTING_PROPERTY, property = "type")
@JsonTypeName(SubTypeB.TYPE)
public class SubTypeB implements BaseType {

  public static final String TYPE = "SubTypeB";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public int hashCode() {
    return TYPE.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof SubTypeB;
  }

}
