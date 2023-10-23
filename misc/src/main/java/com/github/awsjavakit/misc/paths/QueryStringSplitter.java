package com.github.awsjavakit.misc.paths;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

class QueryStringSplitter {

  public static final String CATCH_AMPERSAND_WHEN_NOT_ESCAPED = "(?<!\\\\)&";
  public static final int PROPERTY_PART = 0;
  public static final int VALUE_PART = 1;
  public static final String ASSIGNMENT_OPERATOR = "=";
  private final URI uri;

  public QueryStringSplitter(URI uri) {
    this.uri = uri;
  }

  public Map<String, String> toMap() {
    var queryString = uri.getQuery();
    return
      Arrays.stream(queryString.split(CATCH_AMPERSAND_WHEN_NOT_ESCAPED))
        .map(str -> str.split(ASSIGNMENT_OPERATOR))
        .map(array -> Map.entry(array[PROPERTY_PART], array[VALUE_PART]))
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

  }
}
