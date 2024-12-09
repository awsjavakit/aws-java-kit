package com.github.awsjavakit.hamcrest.hamcrest;

import java.time.Duration;
import java.time.Instant;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

public class InstantIsCloseTo extends BaseMatcher<Instant> {

  private final Instant expected;
  private final Duration error;

  public InstantIsCloseTo(Instant expected, Duration duration) {
    super();
    this.expected = expected;
    this.error = duration;
  }

  public static InstantIsCloseTo closeTo(Instant expected, Duration duration) {
    return new InstantIsCloseTo(expected, duration);
  }

  @Override
  public boolean matches(Object o) {
    var actual = (Instant) o;
    var minExpected = expected.minus(error);
    var maxExpected = expected.plus(error);

    return !actual.isBefore(minExpected) && !actual.isAfter(maxExpected);
  }

  @Override
  public void describeTo(Description description) {
    description.appendText(expected.toString());
  }
}
