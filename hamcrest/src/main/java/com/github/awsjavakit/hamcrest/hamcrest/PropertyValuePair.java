package com.github.awsjavakit.hamcrest.hamcrest;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import com.fasterxml.jackson.databind.JsonNode;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PropertyValuePair {

  public static final String ERROR_INVOKING_GETTER = "Could not get value for field: ";
  public static final String FIELD_PATH_DELIMITER = ".";
  public static final String ROOT_OBJECT_PATH = "";
  public static final String LEFT_BRACE = "[";
  public static final String RIGHT_BRACE = "]";
  public static final String ARRAY_INDEX_INDICATOR = "\\[\\d*\\]";
  public static final String EMPTY_STRING = "";
  private final String propertyName;
  private final Object value;
  private final String fieldPath;
  private final String parentPath;

  public PropertyValuePair(String propertyName, Object value, String parentPath) {
    this.propertyName = propertyName;
    this.value = value;
    this.parentPath = parentPath;
    this.fieldPath = formatFieldPathInfo(propertyName, parentPath);
  }

  public static PropertyValuePair rootObject(Object object) {
    return new PropertyValuePair(null, object, ROOT_OBJECT_PATH);
  }

  public static PropertyValuePair collectionElement(String propertyName, Object value,
                                                    String parentPath, int index) {
    return new PropertyValuePair(propertyName + formatArrayIndex(index), value, parentPath);
  }

  public String getPropertyName() {
    return propertyName;
  }

  public Object getValue() {
    return value;
  }

  public String getFieldPath() {
    return fieldPath;
  }

  public Set<PropertyValuePair> children() {
    return Stream.of(collectClassPropertyDescriptors(), collectRecordPropertyDescriptors())
      .flatMap(Collection::stream)
      .collect(Collectors.toSet());
  }

  private Set<PropertyValuePair> collectClassPropertyDescriptors() {
    var properties = collectPropertyDescriptors();
    return properties.stream()
      .map(this::extractFieldValue)
      .collect(Collectors.toSet());
  }

  public boolean isBaseType() {
    return isNull(value)
      || value.getClass().isPrimitive()
      || value instanceof Class
      || value instanceof String
      || value instanceof Integer
      || value instanceof Double
      || value instanceof Float
      || value instanceof Boolean
      || value instanceof Character
      || value instanceof Byte
      || value instanceof Short
      || value instanceof Long
      || value instanceof Map
      || value instanceof Collection
      || value instanceof JsonNode
      ;
  }

  public boolean isComplexObject() {
    return !isBaseType();
  }

  public boolean isCollection() {
    return nonNull(value) && value instanceof Collection;
  }

  public List<PropertyValuePair> createPropertyValuePairsForEachCollectionItem() {
    Collection<?> values = (Collection<?>) getValue();
    List<PropertyValuePair> collectionElements = new ArrayList<>();
    int index = 0;
    Iterator<?> iterator = values.iterator();
    while (iterator.hasNext()) {
      Object currentValue = iterator.next();
      PropertyValuePair newElement = collectionElement(getPropertyName(), currentValue, parentPath,
        index);
      collectionElements.add(newElement);
      index++;
    }
    return collectionElements;
  }

  public boolean shouldBeChecked(Set<Class<?>> stopRecursionClasses, Set<String> ignoredFields) {
    return classShouldBeChecked(stopRecursionClasses) && fieldShouldBeChecked(ignoredFields);
  }

  private static String formatArrayIndex(int index) {
    return LEFT_BRACE + index + RIGHT_BRACE;
  }

  private List<PropertyValuePair> collectRecordPropertyDescriptors() {
    return
      Optional.ofNullable(value.getClass().getRecordComponents()).stream()
        .flatMap(Arrays::stream)
        .map(this::getDeclaredField)
        .map(this::extractFieldValue)
        .toList();
  }


  @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
  private Object getValue(Field field) {
    try {
      field.setAccessible(true);
      return field.get(value);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  private Field getDeclaredField(RecordComponent comp) {
    try {
      return value.getClass().getDeclaredField(comp.getName());
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(e);
    }
  }

  private boolean classShouldBeChecked(Set<Class<?>> stopRecursionClasses) {
    return stopRecursionClasses
      .stream()
      .noneMatch(stopRecursionClass -> stopRecursionClass.isInstance(value));
  }

  private boolean fieldShouldBeChecked(Set<String> ignoreFields) {
    return !ignoreFields.contains(createGenericFieldPath());
  }

  private String createGenericFieldPath() {
    return getFieldPath().replaceAll(ARRAY_INDEX_INDICATOR, EMPTY_STRING);
  }

  private String formatFieldPathInfo(String propertyName, String parentPath) {
    if (isRootObject()) {
      return ROOT_OBJECT_PATH;
    } else {
      return parentPath + FIELD_PATH_DELIMITER + propertyName;
    }
  }

  private boolean isRootObject() {
    return isNull(propertyName);
  }

  private List<PropertyDescriptor> collectPropertyDescriptors() {
    BeanInfo beanInfo = getBeanInfo(value);
    return Arrays.stream(beanInfo.getPropertyDescriptors())
      .collect(Collectors.toList());
  }

  private BeanInfo getBeanInfo(Object actual) {
    try {
      return Introspector.getBeanInfo(actual.getClass());
    } catch (IntrospectionException e) {
      throw new RuntimeException(e);
    }
  }

  private PropertyValuePair extractFieldValue(Field field) {
    return new PropertyValuePair(field.getName(), getValue(field), this.fieldPath);
  }


  private PropertyValuePair extractFieldValue(PropertyDescriptor propertyDescriptor) {
    try {
      return new PropertyValuePair(
        propertyDescriptor.getName(),
        propertyDescriptor.getReadMethod().invoke(value),
        this.fieldPath
      );
    } catch (IllegalAccessException | InvocationTargetException e) {
      String fieldName = this.fieldPath + FIELD_PATH_DELIMITER + propertyDescriptor.getName();
      throw new RuntimeException(ERROR_INVOKING_GETTER + fieldName, e);
    }
  }
}