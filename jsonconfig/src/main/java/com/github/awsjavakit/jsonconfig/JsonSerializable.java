package com.github.awsjavakit.jsonconfig;

public interface JsonSerializable {

  default String toJsonString() {
    return JsonConfig.writeValueAsString(this);

  }
}
