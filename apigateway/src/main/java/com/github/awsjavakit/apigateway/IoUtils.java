package com.github.awsjavakit.apigateway;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public final class IoUtils {

  private IoUtils() {

  }

  public static String inputStreamToString(InputStream input) {
    return new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))
      .lines()
      .collect(Collectors.joining(System.lineSeparator()));
  }

}
