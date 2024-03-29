package com.github.awsjavakit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DummyClassForLogTesting {

  private static Logger slf4jLogger = LoggerFactory.getLogger(DummyClassForLogTesting.class);

  public void logMessage(String message) {
    slf4jLogger = LoggerFactory.getLogger(DummyClassForLogTesting.class);
    slf4jLogger.info(message);
  }
}
