package com.github.awsjavakit.misc.exceptions;

import static com.github.awsjavakit.misc.exceptions.ExceptionUtils.stackTraceInSingleLine;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class ExceptionUtilsTest {

  public static final String NEW_LINE = System.lineSeparator();

  @Test
  public void stackTraceInSingleLineReturnsExceptionMessageInOneLine() {
    Executable action = this::throwsException;
    ArithmeticException exception = assertThrows(ArithmeticException.class, action);
    verifyOriginalMessageContainsNewLine(exception);

    String exceptionMessage = stackTraceInSingleLine(exception);
    assertThat(exceptionMessage, is(not(Matchers.nullValue())));
    assertThat(exceptionMessage, not(Matchers.containsString(NEW_LINE)));
  }

  @Test
  public void stackTraceInSingleLineThrowsNoExceptionWhenThereIsNoMessage() {
    Exception exception = new Exception((String) null);
    Executable action = () -> stackTraceInSingleLine(exception);
    assertDoesNotThrow(action);
  }

  private void verifyOriginalMessageContainsNewLine(ArithmeticException exception) {
    assertThat(originalMessage(exception), is(not(Matchers.nullValue())));
    assertThat(originalMessage(exception), Matchers.containsString(NEW_LINE));
  }

  private int throwsException() {
    return 1 / 0;
  }

  private String originalMessage(Exception e) {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    PrintWriter pw = new PrintWriter(outputStream);
    e.printStackTrace(pw);
    pw.close();
    return outputStream.toString(StandardCharsets.UTF_8);
  }

}