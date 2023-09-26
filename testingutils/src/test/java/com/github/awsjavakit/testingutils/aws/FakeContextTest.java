package com.github.awsjavakit.testingutils.aws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FakeContextTest {

  @Test
  void shouldReturnSomeRequestId() {
    var context = new FakeContext();
    assertThat(context.getAwsRequestId()).isNotNull();
  }

  @Test
  void shouldIndicateThatTheOtherMethodsShouldBeOverriden() {
    var context = new FakeContext();
    assertThrows(UnsupportedOperationException.class, context::getLogGroupName);
    assertThrows(UnsupportedOperationException.class, context::getFunctionName);
    assertThrows(UnsupportedOperationException.class, context::getIdentity);
    assertThrows(UnsupportedOperationException.class, context::getFunctionVersion);
    assertThrows(UnsupportedOperationException.class, context::getInvokedFunctionArn);
    assertThrows(UnsupportedOperationException.class, context::getLogger);
    assertThrows(UnsupportedOperationException.class, context::getMemoryLimitInMB);
    assertThrows(UnsupportedOperationException.class, context::getRemainingTimeInMillis);
    assertThrows(UnsupportedOperationException.class, context::getLogStreamName);
    assertThrows(UnsupportedOperationException.class, context::getClientContext);
  }

}