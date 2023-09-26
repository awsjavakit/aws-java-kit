package com.github.awsjavakit.eventbridge.handlers;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

//Getters are used by Jackson
@SuppressWarnings("unused")
@JsonTypeInfo(use = Id.NAME, include = As.EXISTING_PROPERTY, property = "type")
@JsonTypeName(SampleEventDetail.TYPE)
public class SampleEventDetail {

  public static final String CUSTOM_HAMCREST_MATCHER_FIELD_SEPARATOR = ".";
  public static final String TYPE = "SampleHandlerInput";
  private String emptyString;
  private String message;
  private Integer identifier;
  private String name;
  private List<String> someEmptyList;

  public static Set<String> propertyNamesOfEmptyFields() {
    return Set.of("emptyString", "someEmptyList");
  }

  public static Set<String> propertyNamesOfEmptyFields(String parentField) {
    return propertyNamesOfEmptyFields()
      .stream()
      .map(field -> parentField + CUSTOM_HAMCREST_MATCHER_FIELD_SEPARATOR + field)
      .collect(Collectors.toSet());
  }

  public static SampleEventDetail eventWithEmptyFields() {
    SampleEventDetail sampleEventDetail = new SampleEventDetail();
    sampleEventDetail.setEmptyString("");
    sampleEventDetail.setSomeEmptyList(Collections.emptyList());
    return sampleEventDetail;
  }

  public List<String> getSomeEmptyList() {
    return someEmptyList;
  }

  public void setSomeEmptyList(List<String> someEmptyList) {
    this.someEmptyList = someEmptyList;
  }

  public String getEmptyString() {
    return emptyString;
  }

  public void setEmptyString(String emptyString) {
    this.emptyString = emptyString;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public Integer getIdentifier() {
    return identifier;
  }

  public void setIdentifier(Integer identifier) {
    this.identifier = identifier;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @JsonProperty(value = "type", access = Access.READ_ONLY)
  public String getType() {
    return TYPE;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getEmptyString(), getMessage(), getIdentifier());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SampleEventDetail that = (SampleEventDetail) o;
    return Objects.equals(getEmptyString(), that.getEmptyString())
      && Objects.equals(getMessage(), that.getMessage())
      && Objects.equals(getIdentifier(), that.getIdentifier());
  }
}
