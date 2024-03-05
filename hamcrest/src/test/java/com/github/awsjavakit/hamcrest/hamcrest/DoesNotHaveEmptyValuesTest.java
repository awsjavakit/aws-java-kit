package com.github.awsjavakit.hamcrest.hamcrest;

import static com.github.awsjavakit.hamcrest.hamcrest.DoesNotHaveEmptyValues.EMPTY_FIELD_ERROR;
import static com.github.awsjavakit.hamcrest.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static com.github.awsjavakit.hamcrest.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static com.github.awsjavakit.hamcrest.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFieldsAndClasses;
import static com.github.awsjavakit.hamcrest.hamcrest.PropertyValuePair.FIELD_PATH_DELIMITER;
import static com.github.awsjavakit.hamcrest.hamcrest.PropertyValuePair.LEFT_BRACE;
import static com.github.awsjavakit.hamcrest.hamcrest.PropertyValuePair.RIGHT_BRACE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class DoesNotHaveEmptyValuesTest {

  public static final String SAMPLE_STRING = "sampleString";
  public static final Map<String, String> SAMPLE_MAP = Map.of(SAMPLE_STRING, SAMPLE_STRING);
  public static final List<String> SAMPLE_LIST = Collections.singletonList(SAMPLE_STRING);
  public static final int SAMPLE_INT = 16;
  public static final String EMPTY_STRING = "";
  public static final String INT_FIELD = "intField";
  public static final String OBJECT_FIELD = "objectWithFields";
  public static final String STRING_FIELD = "stringField";
  public static final String LIST_FIELD = "list";
  public static final String MAP_FIELD = "map";
  public static final String JSON_FIELD = "jsonNode";

  public static final String LIST_WITH_INCOMPLETE_ENTRIES_FIELD = "listWithIncompleteEntries";

  public static final URI EXAMPLE_URI = URI.create("http://example.org");

  public static final String MISSING_VALUE_IN_ARRAY_ELEMENT = "listWithIncompleteEntries[0].stringField";
  public static final String GENERIC_PATH_TO_FIELD_IN_ARRAY_ELEMENT = "listWithIncompleteEntries.stringField";
  public static final Integer NULL_INTEGER = null;
  private static final JsonNode SAMPLE_JSON_NODE = nonEmptyJsonNode();
  private static final String NULL_STRING = null;
  private static final ObjectMapper JSON = new ObjectMapper();
  private DoesNotHaveEmptyValues<Object> matcher;

  @BeforeEach
  public void init() {
    matcher = new DoesNotHaveEmptyValues<>();
  }

  @Test
  public void matchesReturnsTrueWhenObjectWithSimpleFieldsHasNoNullValue() {
    WithBaseTypes nonEmptyObject = new WithBaseTypes(SAMPLE_STRING,
      SAMPLE_INT,
      SAMPLE_LIST,
      SAMPLE_MAP,
      SAMPLE_JSON_NODE);
    assertThat(matcher.matches(nonEmptyObject), is(true));
  }

  @Test
  public void matchesReturnsFalseWhenStringIsEmpty() {
    WithBaseTypes nonEmptyObject = new WithBaseTypes(EMPTY_STRING,
      SAMPLE_INT,
      SAMPLE_LIST,
      SAMPLE_MAP,
      SAMPLE_JSON_NODE);
    assertThat(matcher.matches(nonEmptyObject), is(false));
  }

  @Test
  public void matcherReturnsMessageContainingTheFieldNameWhenFieldIsEmpty() {
    WithBaseTypes withEmptyInt = new WithBaseTypes(SAMPLE_STRING,
      null,
      SAMPLE_LIST,
      SAMPLE_MAP,
      SAMPLE_JSON_NODE);

    AssertionError exception = assertThrows(AssertionError.class,
      () -> assertThat(withEmptyInt, doesNotHaveEmptyValues()));
    assertThat(exception.getMessage(), containsString(EMPTY_FIELD_ERROR));
    assertThat(exception.getMessage(), containsString(INT_FIELD));
  }

  @Test
  public void matcherReturnsFalseWhenObjectContainsChildWithEmptyField() {
    ClassWithChildrenWithMultipleFields testObject =
      new ClassWithChildrenWithMultipleFields(SAMPLE_STRING, objectMissingStringField(),
        SAMPLE_INT);
    assertThat(matcher.matches(testObject), is(false));
  }

  @Test
  public void matcherReturnsMessageWithMissingFieldsPath() {
    ClassWithChildrenWithMultipleFields testObject =
      new ClassWithChildrenWithMultipleFields(SAMPLE_STRING, objectMissingStringField(),
        SAMPLE_INT);
    AssertionError error = assertThrows(AssertionError.class,
      () -> assertThat(testObject, doesNotHaveEmptyValues()));
    assertThat(error.getMessage(),
      containsString(OBJECT_FIELD + FIELD_PATH_DELIMITER + STRING_FIELD));
  }

  @Test
  public void matchesReturnsFalseWhenBaseTypeIsNull() {
    WithBaseTypes baseTypesObject = new WithBaseTypes(null,
      SAMPLE_INT,
      SAMPLE_LIST,
      SAMPLE_MAP,
      SAMPLE_JSON_NODE);
    assertThat(matcher.matches(baseTypesObject), is(false));
  }

  @Test
  public void matchesReturnsFalseWhenListIsEmpty() {
    WithBaseTypes baseTypesObject = new WithBaseTypes(SAMPLE_STRING,
      SAMPLE_INT,
      Collections.emptyList(),
      SAMPLE_MAP,
      SAMPLE_JSON_NODE);
    assertThat(matcher.matches(baseTypesObject), is(false));
  }

  @Test
  public void matchesReturnsFalseWhenMapIsEmpty() {
    WithBaseTypes baseTypesObject = new WithBaseTypes(SAMPLE_STRING,
      SAMPLE_INT,
      SAMPLE_LIST,
      Collections.emptyMap(),
      SAMPLE_JSON_NODE);
    assertThat(matcher.matches(baseTypesObject), is(false));
  }

  @Test
  public void matchesReturnsFalseWhenJsonNodeIsEmpty() {
    WithBaseTypes baseTypesObject = new WithBaseTypes(SAMPLE_STRING,
      SAMPLE_INT,
      SAMPLE_LIST,
      SAMPLE_MAP,
      new ObjectMapper().createObjectNode());
    assertThat(matcher.matches(baseTypesObject), is(false));
  }

  @Test
  public void matchesDoesNotCheckUriFields() {
    ClassWithUri<URI> withUri = new ClassWithUri<>(EXAMPLE_URI, "aNonEmptyValue");
    assertThat(withUri, doesNotHaveEmptyValues());
    assertThat(withUri, doesNotHaveEmptyValuesIgnoringFields(Set.of("someOtherField")));
  }

  @Test
  public void matchesDoesNotCheckUrlFields() throws MalformedURLException {
    ClassWithUri<URL> withUri = new ClassWithUri<>(EXAMPLE_URI.toURL(), "aNonEmptyValue");
    assertThat(withUri, doesNotHaveEmptyValues());
    assertThat(withUri, doesNotHaveEmptyValuesIgnoringFields(Set.of("someOtherField")));
  }

  @Test
  void shouldStopRecursionForClock() {
    var entry = new ClassWithCustomObject<>(Clock.systemDefaultZone());
    assertThat(entry, doesNotHaveEmptyValues());

  }

  @Test
  public void matchesDoesNotCheckFieldInAdditionalCustomIgnoreClass() {
    WithBaseTypes ignoredObjectWithEmptyProperties =
      new WithBaseTypes(null, null, null, null, null);

    ClassWithChildrenWithMultipleFields testObject =
      new ClassWithChildrenWithMultipleFields(SAMPLE_STRING, ignoredObjectWithEmptyProperties,
        SAMPLE_INT);
    assertThat(testObject,
      DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringClasses(Set.of(WithBaseTypes.class)));
  }

  @Test
  public void matchesReturnsFalseWhenListElementHasEmptyValues() {
    WithBaseTypes objectWithSomeEmptyValue =
      new WithBaseTypes(null, SAMPLE_INT, SAMPLE_LIST, SAMPLE_MAP, SAMPLE_JSON_NODE);
    ClassWithList withList = new ClassWithList(List.of(objectWithSomeEmptyValue));
    AssertionError error = assertThrows(AssertionError.class,
      () -> assertThat(withList, doesNotHaveEmptyValues()));
    int expectedIndex = 0;
    assertThat(error.getMessage(), containsString(LIST_WITH_INCOMPLETE_ENTRIES_FIELD));
    assertThat(error.getMessage(), containsString(LEFT_BRACE + expectedIndex + RIGHT_BRACE));
  }

  @Test
  public void matchesReturnsTrueWhenIgnoredFieldIsNull() {
    WithBaseTypes testObject =
      new WithBaseTypes(null, SAMPLE_INT, SAMPLE_LIST, SAMPLE_MAP, SAMPLE_JSON_NODE);
    assertThat(testObject, doesNotHaveEmptyValuesIgnoringFields(Set.of(STRING_FIELD)));
  }

  @Test
  public void matchesReturnsTrueWhenIgnoredFieldPathPointsToFieldInObjectOfArray() {
    WithBaseTypes objectWithSomeEmptyValue =
      new WithBaseTypes(null, SAMPLE_INT, SAMPLE_LIST, SAMPLE_MAP, SAMPLE_JSON_NODE);
    ClassWithList withList = new ClassWithList(List.of(objectWithSomeEmptyValue));
    Executable checkWithoutExceptions = () -> assertThat(withList, doesNotHaveEmptyValues());
    AssertionError assertionError = assertThrows(AssertionError.class, checkWithoutExceptions);
    assertThat(assertionError.getMessage(), containsString(MISSING_VALUE_IN_ARRAY_ELEMENT));
    assertThat(withList,
      doesNotHaveEmptyValuesIgnoringFields(Set.of(GENERIC_PATH_TO_FIELD_IN_ARRAY_ELEMENT)));
  }

  @Test
  void shouldReturnTrueWhenIgnoredFieldIsEmptyAndFieldsInIgnoredClassAreEmpty() {
    var withBaseTypes = new WithBaseTypes(EMPTY_STRING,
      NULL_INTEGER,
      Collections.emptyList(),
      Collections.emptyMap(),
      JSON.createObjectNode());
    var objectWithBothEmptyValuesAndClassWithEmptyValues =
      new ClassWithChildrenWithMultipleFields(NULL_STRING, withBaseTypes, SAMPLE_INT);
    assertThatContainedObjectHasEmptyFields(withBaseTypes);
    assertThat(objectWithBothEmptyValuesAndClassWithEmptyValues,
      doesNotHaveEmptyValuesIgnoringFieldsAndClasses(
        Set.of(WithBaseTypes.class), Set.of("someStringField")));
  }



  private static JsonNode nonEmptyJsonNode() {
    ObjectNode node = new ObjectMapper().createObjectNode();
    node.put(SAMPLE_STRING, SAMPLE_STRING);
    return node;
  }

  private void assertThatContainedObjectHasEmptyFields(WithBaseTypes withBaseTypes) {
    var exception = assertThrows(AssertionError.class,
      () -> assertThat(withBaseTypes, doesNotHaveEmptyValues()));
    assertThat(exception.getMessage(), allOf(
      containsString(STRING_FIELD),
      containsString(INT_FIELD),
      containsString(LIST_FIELD),
      containsString(MAP_FIELD),
      containsString(JSON_FIELD)));
  }

  private WithBaseTypes objectMissingStringField() {
    return new WithBaseTypes(null, SAMPLE_INT, SAMPLE_LIST, SAMPLE_MAP, SAMPLE_JSON_NODE);
  }

  private static final class WithBaseTypes {

    private final String stringField;
    private final Integer intField;
    private final List<String> list;
    private final Map<String, String> map;
    private final JsonNode jsonNode;

    public WithBaseTypes(String stringField, Integer intField, List<String> list,
      Map<String, String> map, JsonNode jsonNode) {
      this.stringField = stringField;
      this.intField = intField;
      this.list = list;
      this.map = map;
      this.jsonNode = jsonNode;
    }

    public String getStringField() {
      return stringField;
    }

    public Integer getIntField() {
      return intField;
    }

    public List<String> getList() {
      return list;
    }

    public Map<String, String> getMap() {
      return map;
    }

    public JsonNode getJsonNode() {
      return jsonNode;
    }
  }

  private static final class ClassWithChildrenWithMultipleFields {

    private String someStringField;
    private WithBaseTypes objectWithFields;
    private Integer someIntField;

    public ClassWithChildrenWithMultipleFields(String someStringField,
      WithBaseTypes objectWithFields,
      Integer someIntField) {
      this.someStringField = someStringField;
      this.objectWithFields = objectWithFields;
      this.someIntField = someIntField;
    }

    public String getSomeStringField() {
      return someStringField;
    }

    public void setSomeStringField(String someStringField) {
      this.someStringField = someStringField;
    }

    public WithBaseTypes getObjectWithFields() {
      return objectWithFields;
    }

    public void setObjectWithFields(
      WithBaseTypes objectWithFields) {
      this.objectWithFields = objectWithFields;
    }

    public Integer getSomeIntField() {
      return someIntField;
    }

    public void setSomeIntField(Integer someIntField) {
      this.someIntField = someIntField;
    }
  }

  private static final class ClassWithUri<T> {

    private T uri;
    private List<T> uris;
    private String someOtherField;

    private ClassWithUri(T uri, String someOtherField) {
      this.uri = uri;
      this.uris = List.of(uri);
      this.someOtherField = someOtherField;
    }

    public List<T> getUris() {
      return uris;
    }

    public void setUris(List<T> uris) {
      this.uris = uris;
    }

    public String getSomeOtherField() {
      return someOtherField;
    }

    public void setSomeOtherField(String someOtherField) {
      this.someOtherField = someOtherField;
    }

    public T getUri() {
      return uri;
    }

    public void setUri(T uri) {
      this.uri = uri;
    }
  }

  private static final class ClassWithList {

    private List<WithBaseTypes> listWithIncompleteEntries;

    private ClassWithList(List<WithBaseTypes> listWithIncompleteEntries) {
      this.listWithIncompleteEntries = listWithIncompleteEntries;
    }

    public List<WithBaseTypes> getListWithIncompleteEntries() {
      return listWithIncompleteEntries;
    }

    public void setListWithIncompleteEntries(
      List<WithBaseTypes> listWithIncompleteEntries) {
      this.listWithIncompleteEntries = listWithIncompleteEntries;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj == null || obj.getClass() != this.getClass()) {
        return false;
      }
      var that = (ClassWithList) obj;
      return Objects.equals(this.listWithIncompleteEntries, that.listWithIncompleteEntries);
    }

    @Override
    public int hashCode() {
      return Objects.hash(listWithIncompleteEntries);
    }

    @Override
    public String toString() {
      return "ClassWithList[" +
        "listWithIncompleteEntries=" + listWithIncompleteEntries + ']';
    }

  }

  private static final class ClassWithCustomObject<T> {

    private T customObject;

    private ClassWithCustomObject(T customObject) {
      this.customObject = customObject;
    }

    public T getCustomObject() {
      return customObject;
    }

    public void setCustomObject(T customObject) {
      this.customObject = customObject;
    }

  }
}