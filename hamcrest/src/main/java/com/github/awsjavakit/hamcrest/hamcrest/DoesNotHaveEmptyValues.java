package com.github.awsjavakit.hamcrest.hamcrest;

import static java.util.Objects.isNull;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.URL;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

@SuppressWarnings("PMD.LooseCoupling")
public class DoesNotHaveEmptyValues<T> extends BaseMatcher<T> {

  public static final String EMPTY_FIELD_ERROR = "Empty field found: ";
  public static final String FIELD_DELIMITER = ",";
  public static final String TEST_DESCRIPTION = "All fields of all included objects need to be non empty";

  private final List<PropertyValuePair> emptyFields;
  private Set<Class<?>> stopRecursionClasses;
  private Set<String> ignoreFields;

  public DoesNotHaveEmptyValues() {
    super();
    stopRecursionClasses = classesWithNoPojoStructure();
    ignoreFields = Collections.emptySet();

    this.emptyFields = new ArrayList<>();
  }

  public static <R> DoesNotHaveEmptyValues<R> doesNotHaveEmptyValues() {
    return new DoesNotHaveEmptyValues<>();
  }

  /**
   * Stop the nested check for the classes in the ignore list. The fields of the specified types
   * will be checked whether they are null or not, but their fields will not be checked.
   *
   * @param ignoreList List of classes where the nested field check will stop.
   * @param <R>        the type of the object in test.
   * @return a matcher.
   */
  public static <R> DoesNotHaveEmptyValues<R> doesNotHaveEmptyValuesIgnoringClasses(
    Set<Class<?>> ignoreList) {
    DoesNotHaveEmptyValues<R> matcher = new DoesNotHaveEmptyValues<>();
    initializeClassesWhereRecursiveFieldCheckingWillStop(ignoreList, matcher);
    return matcher;
  }

  public static <R> DoesNotHaveEmptyValues<R> doesNotHaveEmptyValuesIgnoringFields(
    Set<String> ignoreList) {
    DoesNotHaveEmptyValues<R> matcher = new DoesNotHaveEmptyValues<>();
    initializeFieldNamesWhichWillBeIgnoredDuringChecking(ignoreList, matcher);
    return matcher;
  }

  public static <R> DoesNotHaveEmptyValues<R> doesNotHaveEmptyValuesIgnoringFieldsAndClasses(
    Set<Class<?>> ignoreClassesList,
    Set<String> ignoreFieldsList) {
    DoesNotHaveEmptyValues<R> matcher = new DoesNotHaveEmptyValues<>();
    initializeClassesWhereRecursiveFieldCheckingWillStop(ignoreClassesList, matcher);
    initializeFieldNamesWhichWillBeIgnoredDuringChecking(ignoreFieldsList, matcher);
    return matcher;
  }

  @Override
  public boolean matches(Object actual) {
    return objectDoesNotHaveFieldsWithEmptyValues(PropertyValuePair.rootObject(actual));
  }

  public boolean objectDoesNotHaveFieldsWithEmptyValues(PropertyValuePair fieldValue) {
    List<PropertyValuePair> fieldsToBeChecked = createListWithFieldsToBeChecked(fieldValue);
    emptyFields.addAll(collectEmptyFields(fieldsToBeChecked));
    return emptyFields.isEmpty();
  }

  @Override
  public void describeTo(Description description) {
    description.appendText(TEST_DESCRIPTION);
  }

  @Override
  public void describeMismatch(Object item, Description description) {
    String emptyFieldNames = emptyFields.stream()
      .map(PropertyValuePair::getFieldPath)
      .collect(Collectors.joining(
        FIELD_DELIMITER));

    description.appendText(EMPTY_FIELD_ERROR)
      .appendText(emptyFieldNames);
  }

  private static <R> void initializeFieldNamesWhichWillBeIgnoredDuringChecking(
    Set<String> ignoreList,
    DoesNotHaveEmptyValues<R> matcher) {
    matcher.ignoreFields = addFieldPathDelimiterToRootField(ignoreList);
  }

  private static <R> void initializeClassesWhereRecursiveFieldCheckingWillStop(
    Set<Class<?>> ignoreList,
    DoesNotHaveEmptyValues<R> matcher) {
    Set<Class<?>> newStopRecursionClasses = new HashSet<>();
    newStopRecursionClasses.addAll(matcher.stopRecursionClasses);
    newStopRecursionClasses.addAll(ignoreList);
    matcher.stopRecursionClasses = newStopRecursionClasses;
  }

  private static Set<String> addFieldPathDelimiterToRootField(Set<String> ignoreList) {
    return ignoreList.stream()
      .map(DoesNotHaveEmptyValues::addPathDelimiterToTopLevelFields)
      .collect(Collectors.toSet());
  }

  private static String addPathDelimiterToTopLevelFields(String f) {
    if (f.startsWith(PropertyValuePair.FIELD_PATH_DELIMITER)) {
      return f;
    } else {
      return PropertyValuePair.FIELD_PATH_DELIMITER + f;
    }
  }

  private static boolean isEmptyContainer(JsonNode node) {
    return node.isContainerNode() && node.isEmpty();
  }

  @SuppressWarnings("PMD.LooseCoupling")
  private List<PropertyValuePair> createListWithFieldsToBeChecked(PropertyValuePair rootObject) {
    List<PropertyValuePair> fieldsToBeChecked = new ArrayList<>();
    var fieldsToBeVisited = initializeFieldsToBeVisited(rootObject);
    while (!fieldsToBeVisited.isEmpty()) {
      PropertyValuePair currentField = fieldsToBeVisited.pop();
      if (currentField.shouldBeChecked(stopRecursionClasses, ignoreFields)) {
        addNestedFieldsToFieldsToBeVisited(fieldsToBeVisited, currentField);
        fieldsToBeChecked.add(currentField);
      }
    }
    return fieldsToBeChecked;
  }

  @SuppressWarnings("PMD.LooseCoupling")
  private Stack<PropertyValuePair> initializeFieldsToBeVisited(PropertyValuePair rootObject) {
    var fieldsToBeVisited = new Stack<PropertyValuePair>();
    fieldsToBeVisited.add(rootObject);
    return fieldsToBeVisited;
  }

  @SuppressWarnings("PMD.LooseCoupling")
  private void addNestedFieldsToFieldsToBeVisited(Stack<PropertyValuePair> fieldsToBeVisited,
                                                  PropertyValuePair currentField) {
    if (currentField.isComplexObject()) {
      fieldsToBeVisited.addAll(currentField.children());
    } else if (currentField.isCollection()) {
      addEachArrayElementAsFieldToBeVisited(fieldsToBeVisited, currentField);
    }
  }

  @SuppressWarnings("PMD.LooseCoupling")
  private void addEachArrayElementAsFieldToBeVisited(Stack<PropertyValuePair> fieldsToBeVisited,
                                                     PropertyValuePair currentField) {
    var collectionElements = currentField.createPropertyValuePairsForEachCollectionItem();
    fieldsToBeVisited.addAll(collectionElements);
  }

  /*Classes that their fields do not have getters*/
  private Set<Class<?>> classesWithNoPojoStructure() {
    return Set.of(
      URI.class,
      URL.class,
      Clock.class
    );
  }

  private List<PropertyValuePair> collectEmptyFields(List<PropertyValuePair> propertyValuePairs) {
    return propertyValuePairs.stream()
      .filter(propertyValue -> isEmpty(propertyValue.getValue()))
      .collect(Collectors.toList());
  }

  @SuppressWarnings("PMD.SimplifyBooleanReturns")
  private boolean isEmpty(Object value) {
    if (isNull(value)) {
      return true;
    }
    return
      isBlankString(value)
        || isEmptyCollection(value)
        || isEmptyMap(value)
        || isEmptyJsonNode(value);
  }

  @SuppressWarnings("PMD.SimplifyBooleanReturns")
  private boolean isEmptyMap(Object value) {
    return value instanceof Map && ((Map<?, ?>) value).isEmpty();
  }

  @SuppressWarnings("PMD.SimplifyBooleanReturns")
  private boolean isEmptyJsonNode(Object value) {
    if (value instanceof JsonNode) {
      return isEmptyContainer((JsonNode) value) || isEmptyValueNode((JsonNode) value);
    }
    return false;
  }

  @SuppressWarnings("PMD.SimplifyBooleanReturns")
  private boolean isEmptyValueNode(JsonNode value) {
    if (value.isTextual()) {
      return isNull(value.textValue()) || value.textValue().isBlank();
    }
    return value.isMissingNode() || value.isNull();

  }

  @SuppressWarnings("PMD.SimplifyBooleanReturns")
  private boolean isEmptyCollection(Object value) {
    return value instanceof Collection && ((Collection<?>) value).isEmpty();
  }

  @SuppressWarnings("PMD.SimplifyBooleanReturns")
  private boolean isBlankString(Object value) {
    return value instanceof String && ((String) value).isBlank();
  }
}
