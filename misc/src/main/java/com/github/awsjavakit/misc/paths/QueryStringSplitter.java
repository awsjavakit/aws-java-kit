package com.github.awsjavakit.misc.paths;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

class QueryStringSplitter {

  public static final String CATCH_AMPERSAND_WHEN_NOT_ESCAPED = "(?<!\\\\)&";
  private final URI uri;

  public QueryStringSplitter(URI uri) {
    this.uri = uri;
  }

  public Map<String, String> toMap() {
    var queryString = uri.getQuery();
    return
      Arrays.stream(queryString.split(CATCH_AMPERSAND_WHEN_NOT_ESCAPED))
        .map(str -> str.split("="))
        .map(array -> Map.entry(array[0], array[1]))
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

  }
}
