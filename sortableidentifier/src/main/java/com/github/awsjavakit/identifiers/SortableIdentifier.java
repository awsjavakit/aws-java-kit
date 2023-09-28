package com.github.awsjavakit.identifiers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.Instant;
import java.util.UUID;

public class SortableIdentifier {

  private static final String CONSTANT_TIMESTAMP_LENGTH = "%012x-%s";
  private static final int UUID_START_INDEX = 13;
  private final String value;

  @JsonCreator
  public SortableIdentifier(String value) {
    validate(value);
    this.value = value;

  }

  public static SortableIdentifier create() {
    var now = Instant.now().toEpochMilli();
    var stringValue = String.format(CONSTANT_TIMESTAMP_LENGTH, now, UUID.randomUUID());
    return new SortableIdentifier(stringValue);
  }

  public UUID getUuid() {
    return UUID.fromString(value.substring(UUID_START_INDEX));
  }

  @JsonValue
  @Override
  public String toString() {
    return value;
  }

  private void validate(String value) {
    var uuidSubstring = value.substring(UUID_START_INDEX);
    try {
      UUID.fromString(uuidSubstring);
    } catch (RuntimeException e) {
      throw new RuntimeException("SortableIdentifier should contain a UUID",e);
    }
  }
}
