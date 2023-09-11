package com.github.awsjavakit.apigateway;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.stream.Collectors;

/**
 * A small utility class for reading from resources and files.
 */
public final class IoUtils {

  private IoUtils() {
    // NO-OP
  }

  /**
   * Read a file from the resources as String.
   *
   * @param resourcePath the path of the resource file relative to the resources folder.
   * @return a String with the file contents.
   */
  public static String stringFromResources(Path resourcePath) {
    var inputStream = ClassLoader.getSystemClassLoader()
      .getResourceAsStream(resourcePath.toString());
    return inputStreamToString(inputStream);
  }

  /**
   * Converts an {@link InputStream} to {@link String} assuming UTF-8 encoding
   *
   * @param input an {@link InputStream}
   * @return a String
   */
  public static String inputStreamToString(InputStream input) {
    return new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))
      .lines()
      .collect(Collectors.joining(System.lineSeparator()));
  }
}
