package com.github.awsjavakit.testingutils;

import static com.gtihub.awsjavakit.attempt.Try.attempt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.awsjavakit.misc.JacocoGenerated;
import java.net.URI;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Random;
import net.datafaker.Faker;
import org.apache.commons.lang3.RandomStringUtils;

/**
 * Utility class for generating random values.
 */
@JacocoGenerated
public final class RandomDataGenerator {

  private static final int MIN_RANDOM_STRING_LENGTH = 10;
  private static final int MAX_RANDOM_STRING_LENGTH = 20;
  private static final Random RANDOM = new Random();
  private static final Faker FAKER = new Faker();
  private static final Instant BEGINNING_OF_TIME =
    LocalDateTime.of(1971, Month.JANUARY, 2, 0, 0).toInstant(ZoneOffset.UTC);
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final String ILLEGAL_CEILING_VALUE = "Cannot have a negative ceiling";
  private static final Instant END_OF_TIME = Instant.now();
  private static final int ARBITRARY_FIELDS_NUMBER = 5;
  private static final ZoneId ZONE_ID = ZoneId.systemDefault();
  private static final ZoneOffset CURRENT_ZONE_OFFSET = ZONE_ID.getRules().getOffset(Instant.now());

  private RandomDataGenerator() {

  }

  /**
   * Generate a random alphanumeric string.
   *
   * @return a random alphanumeric string.
   */
  public static String randomString() {
    return RandomStringUtils.randomAlphanumeric(MIN_RANDOM_STRING_LENGTH, MAX_RANDOM_STRING_LENGTH);
  }

  /**
   * Generate a random integer from 0 ot {@link Integer#MAX_VALUE}.
   *
   * @return a random integer from 0 ot {@link Integer#MAX_VALUE}.
   */
  public static Integer randomInteger() {
    return randomInteger(Integer.MAX_VALUE);
  }

  /**
   * @param bound the (exclusive) upper bound for random values.
   * @return a random integer in the [0,bound) interval.
   */
  public static Integer randomInteger(int bound) {
    return RANDOM.nextInt(bound);
  }

  /**
   * A URI of the form "https://www.example.com/&lt;some_random_alphanumeric&gt;"
   *
   * @return a random URI.
   */
  public static URI randomUri() {
    return URI.create("https://www.example.com/" + randomString());
  }

  /**
   * Generates a random DOI.
   *
   * @return a DOI as URI
   */
  public static URI randomDoi() {
    return URI.create("https://doi.org/10.1000/" + randomInteger(10_000));
  }

  /**
   * Generates a random ISBN-10.
   *
   * @return a random ISBN-10.
   */
  public static String randomIsbn10() {
    return FAKER.code().isbn10();
  }

  /**
   * Generates a random ISBN-13.
   *
   * @return a random ISBN-13.
   */
  public static String randomIsbn13() {
    return FAKER.code().isbn13();
  }

  /**
   * Return a random boolean.
   *
   * @return true or false
   */
  public static boolean randomBoolean() {
    return randomElement(true, false);
  }

  /**
   * Returns a random element out of the specified elements.
   * @param elements the elements that we are going to pick from.
   * @return one of the specified elements.
   * @param <T> the elements' type.
   */
  public static <T> T randomElement(T... elements) {
    return elements[RANDOM.nextInt(elements.length)];
  }

  /**
   * Returns a random element out of the specified elements.
   * @param elements the elements that we are going to pick from.
   * @return one of the specified elements.
   * @param <T> the elements' type.
   */
  @SuppressWarnings({"unchecked"})
  public static <T> T randomElement(Collection<T> elements) {
    return (T) randomElement(elements.toArray());
  }

  /**
   * Generates a random {@link LocalDateTime} between 1970-02-01 and now
   *
   * @return a {@link LocalDateTime} between [1970-02-01,now)
   */
  public static LocalDateTime randomLocalDateTime() {
    return LocalDateTime.ofInstant(randomInstant(), ZONE_ID);
  }

  /**
   * Random LocalDateTime.
   *
   * @param after a {@link  LocalDateTime} lower bound
   * @return a random {@link LocalDateTime} between (after, now)
   */
  public static LocalDateTime randomLocalDateTime(LocalDateTime after) {
    var afterAsInstant = Instant.from(after.toInstant(CURRENT_ZONE_OFFSET));
    return LocalDateTime.ofInstant(randomInstant(afterAsInstant), ZONE_ID);
  }

  /**
   * Random LocalDateTime.
   *
   * @return a {@link LocalDateTime} between (1971-01-02:00:00, now)
   */
  public static LocalDate randomLocalDate() {
    return LocalDate.from(randomLocalDateTime());
  }

  /**
   * Random LocalDate.
   *
   * @param after a {@link  LocalDate} lower bound
   * @return a random {@link LocalDate} between (after, now)
   */
  public static LocalDate randomLocalDate(LocalDate after) {
    var afterAsInstant = Instant.from(after.atStartOfDay().toInstant(CURRENT_ZONE_OFFSET));
    return LocalDate.ofInstant(randomInstant(afterAsInstant), ZONE_ID);
  }

  /**
   * Random LocalDateTime
   *
   * @return a random {@link Instant} between (1971-02-01:00:00, now)
   */
  public static Instant randomInstant() {
    return FAKER.date().between(Date.from(BEGINNING_OF_TIME), Date.from(END_OF_TIME)).toInstant();
  }

  /**
   * Random LocalDate.
   *
   * @param after a {@link  Instant} lower bound
   * @return a random {@link Instant} between (after, now)
   */
  public static Instant randomInstant(Instant after) {
    return FAKER.date().between(Date.from(after), Date.from(END_OF_TIME)).toInstant();
  }

  /**
   * Generates a random JSON string with random keys and random values;
   *
   * @return a JSON string
   */
  public static String randomJson() {
    var root = JSON.createObjectNode();
    for (int i = 0; i < ARBITRARY_FIELDS_NUMBER; i++) {
      root.set(randomString(), randomFlatJson());
    }
    return attempt(() -> JSON.writeValueAsString(root)).orElseThrow();
  }

  /**
   * Generates a random ISSN;
   *
   * @return a String containing a valid ISSN;
   */
  public static String randomIssn() {
    return IssnGenerator.randomIssn();
  }

  /**
   * Generates an invalid ISSN;
   *
   * @return an ISSN-like string.
   */
  public static String randomInvalidIssn() {
    return IssnGenerator.randomInvalidIssn();
  }

  /**
   * Generates a bounded random double between.
   *
   * @param ceiling the (exclusive upper bound)
   * @return a double number between [0,ceiling).
   */
  public static double randomDouble(double ceiling) {
    if (ceilingIsValidCeilingValue(ceiling)) {
      return ceiling * randomDoubleBetweenZeroAndOne();
    }
    throw new IllegalArgumentException(ILLEGAL_CEILING_VALUE);
  }

  // negative ceiling would mean that we are expected to return a value between (-Inf, ceiling).
  private static boolean ceilingIsValidCeilingValue(double ceiling) {
    return ceiling >= 0;
  }

  private static double randomDoubleBetweenZeroAndOne() {
    return RANDOM.nextDouble();
  }

  private static ObjectNode randomFlatJson() {
    var root = JSON.createObjectNode();
    for (int i = 0; i < ARBITRARY_FIELDS_NUMBER; i++) {
      root.put(randomString(), randomString());
    }
    return root;
  }
}
