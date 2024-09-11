package com.github.awsjavakit.testingutils.hamcrest;

import static com.github.awsjavakit.testingutils.hamcrest.InstantIsCloseTo.closeTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class InstantIsCloseToTest {

  @Test
  void shouldSucceedWhenActualIsEqualToExpected() {
    var expected = Instant.parse("1980-01-01T00:00:00z");
    var actual = Instant.parse("1980-01-01T00:00:00z");
    assertThat(actual, is(closeTo(expected, Duration.ofSeconds(1))));
  }

  @Test
  void shouldFailWhenActualIsNotEqualToExpected() {
    var expected = Instant.parse("1980-01-01T00:00:00z");
    var actual = Instant.now();
    Executable action = () -> assertThat(actual, is(closeTo(expected, Duration.ofSeconds(1))));
    var error= assertThrows(AssertionError.class, action);
    assertThat(error.getMessage(),containsString(expected.toString()));
  }

  @Test
  void shouldSucceedIfActualIsWithinAcceptableRange() {
    var expected = Instant.parse("1980-01-01T00:00:00z");
    var actual = Instant.parse("1980-01-01T00:00:05z");
    assertThat(actual, is(closeTo(expected, Duration.ofSeconds(10))));
  }

  @Test
  void shouldAcceptZeroDuration(){
    var expected = Instant.parse("1980-01-01T00:00:00z");
    var actual = Instant.parse("1980-01-01T00:00:00z");
    assertThat(actual, is(closeTo(expected, Duration.ZERO)));
  }

}