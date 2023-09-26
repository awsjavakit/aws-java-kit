package com.github.awsjavakit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;

import com.github.awsjavakit.logutils.LogUtils;
import com.github.awsjavakit.logutils.TestAppender;
import org.junit.jupiter.api.Test;

class LogUtilsTest {

    @Test
    public void shouldReturnAnAppenderWithTheLoggedMessages() {
        TestAppender appender = LogUtils.getTestingAppender(DummyClassForLogTesting.class);

        assertThatAppenderCapturesLogMessagesFromCustomClass(appender);
    }

    @Test
    public void shouldReturnTheNameOfTheClass() {
        String loggerName = LogUtils.toLoggerName(SamplePojo.class);
        assertThat(loggerName, is(equalTo(SamplePojo.class.getCanonicalName())));
    }

    @Test
    public void shouldIncludeLogsOfClassOfInterestAsWellWhenListingLogsOfRootLogger() {
        TestAppender appender = LogUtils.getTestingAppenderForRootLogger();
        assertThatAppenderCapturesLogMessagesFromCustomClass(appender);
    }

    private void assertThatAppenderCapturesLogMessagesFromCustomClass(TestAppender appender) {
        DummyClassForLogTesting loggingObject = new DummyClassForLogTesting();
        String someMessage = "Some message";

        loggingObject.logMessage(someMessage);
        String actual = appender.getMessages();
        assertThat(actual, containsString(someMessage));
    }
}