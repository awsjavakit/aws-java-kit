package com.github.awsjavakit;

import org.slf4j.LoggerFactory;

public class DummyClassForLogTesting {

  public void logMessage(String message) {
    var slf4jLogger = LoggerFactory.getLogger(DummyClassForLogTesting.class);
    slf4jLogger.info(message);
  }
}
