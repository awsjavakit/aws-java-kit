package com.github.awsjavakit.misc.exceptions;

import com.github.awsjavakit.misc.SingletonCollector;
import com.github.awsjavakit.misc.StringUtils;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.stream.Stream;

public final class ExceptionUtils {

  private ExceptionUtils() {
  }

  /**
   * Returns the stacktrace in one line. It replaces all whitespaces with space and removes multiple
   * whitespaces.
   *
   * @param e the Exception
   * @return the Stacktrace String.
   */
  public static String stackTraceInSingleLine(Exception e) {
    StringWriter sw = new StringWriter();
    e.printStackTrace(new PrintWriter(sw));
    String exceptionString = sw.toString();
    return Stream.of(exceptionString)
      .map(StringUtils::removeMultipleWhiteSpaces)
      .map(StringUtils::replaceWhiteSpacesWithSpace)
      .collect(SingletonCollector.collect());
  }
}

