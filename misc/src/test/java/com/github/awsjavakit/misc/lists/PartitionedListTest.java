package com.github.awsjavakit.misc.lists;

import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomInteger;
import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PartitionedListTest {

  public static final int SAMPLE_SIZE = 100;

  @Test
  void shouldAcceptAListAndThePartitionSize() {
    var sample = sampleList(SAMPLE_SIZE);
    int partitionSize = randomInteger(SAMPLE_SIZE);
    assertDoesNotThrow(() -> new PartitionedList<>(sample, partitionSize));
  }

  @ParameterizedTest
  @CsvSource({
    "100,5,4,3",
    "100,1,25,0", // extreme case with single item partitions
    "100,99,0,0",
    "100,99,0,98",
    "100,99,1,0",
  })
  void shouldReturnTheJthElementOfTheIthPartition(int sampleSize,
    int partitionSize,
    int partitionIndex,
    int elementIndex) {
    var sample = sampleList(sampleSize);

    var expectedItem = sample.get(
      calculateSampleIndex(partitionIndex, partitionSize, elementIndex));
    var partitioned = new PartitionedList<>(sample, partitionSize);
    assertThat(partitioned.get(partitionIndex).get(elementIndex)).isEqualTo(expectedItem);
  }

  @Test
  void shouldThrowIndexOutOfBoundsOfExceptionWhenRequestingPartitionIndexGreaterThanPossible() {
    var sample = sampleList(SAMPLE_SIZE);
    var partitionSizeGuaranteeingTwoPartitionsOnly = SAMPLE_SIZE - 1;
    var partitioned = new PartitionedList<>(sample, partitionSizeGuaranteeingTwoPartitionsOnly);
    var illegalIndex = 3;
    var exception =
      assertThrows(IndexOutOfBoundsException.class, () -> partitioned.get(illegalIndex));
    assertThat(exception.getMessage()).contains(Integer.toString(illegalIndex));
  }

  @Test
  void shouldReturnTheNumberOfPartitionsAsSize() {
    var sample = sampleList(randomInteger(1000));
    var partitionSize = randomInteger(sample.size());
    var partitioned = new PartitionedList<>(sample, partitionSize);
    var expectedSize = (int) Math.ceil((double) sample.size() / (double) partitionSize);
    assertThat(partitioned.size()).isEqualTo(expectedSize);
  }

  @Test
  void everyPartitionShouldHaveAtMostTheIndicatedSize() {
    var sample = sampleList(randomInteger(1000));
    int partitionSize = randomInteger(sample.size());
    var partitioned = new PartitionedList<>(sample, partitionSize);
    for (var partition : partitioned) {
      assertThat(partition).hasSizeLessThanOrEqualTo(partitionSize);
    }
  }

  @Test
  void shouldReturnIteratorOfPartitions() {
    var sample = sampleList(randomInteger(1000));
    int partitionSize = randomInteger(sample.size());
    var partitioned = new PartitionedList<>(sample, partitionSize);
    var reconstructed = new ArrayList<String>();
    //on purpose explicit use of iterator
    for(var iterator = partitioned.iterator(); iterator.hasNext();){
      reconstructed.addAll(iterator.next());
    }

    assertThat(reconstructed).containsAll(sample);
  }

  @Test
  void shouldReturnPartitionsNotLargerThanSpecifiedSize() {
    var sample = sampleList(randomInteger(1000));
    int partitionSize = randomInteger(sample.size());
    var partitioned = new PartitionedList<>(sample, partitionSize);
    for(var partition:partitioned){
      assertThat(partition).hasSizeLessThanOrEqualTo(partitionSize);
    }
  }

  @Test
  void shouldReturnStreamOfPartitions() {
    var sample = sampleList(randomInteger(1000));
    var partitionSize = randomInteger(sample.size());
    var partitioned = new PartitionedList<>(sample, partitionSize);
    var listOfLists = verifyAtCompileTimeThatIsListOfLists(partitioned);
    var reconstructed = listOfLists.stream()
      .flatMap(Collection::stream)
      .toList();
    assertThat(reconstructed).isEqualTo(sample);
  }

  @Test
  void shouldReturnThatIsEmptyOnlyWhenEmpty() {
    var emptySample = new PartitionedList<>(Collections.emptyList(), randomInteger());
    assertThat(emptySample.isEmpty()).isEqualTo(true);

    var nonEmpySample = new PartitionedList<>(sampleList(10), randomInteger(10));
    assertThat(nonEmpySample.isEmpty()).isEqualTo(false);
  }

  @Test
  void shouldReturnAnArrayOfLists() {
    var sample = sampleList(randomInteger(1000));
    var partitionSize = randomInteger(sample.size());
    var partitioned = new PartitionedList<>(sample, partitionSize);

    var array = partitioned.toArray();
    var reconstructed = Arrays.stream(array)
      .map(List.class::cast)
      .flatMap(Collection::stream)
      .toList();
    assertThat(reconstructed).isEqualTo(sample);
  }

  @Test
  void shouldPopulateGivenArray() {
    var sample = sampleList(randomInteger(1000));
    var partitionSize = randomInteger(sample.size());
    var partitioned = new PartitionedList<>(sample, partitionSize);

    var array = partitioned.toArray(List[]::new);
    var reconstructed = Arrays.stream(array)
      .map(List.class::cast)
      .flatMap(Collection::stream)
      .toList();
    assertThat(reconstructed).isEqualTo(sample);
  }

  @Disabled
  @Test
  void shouldBeComparableToGuavasImplementation() {
    var sample = sampleIntList(100_000_001);
    var partitions = 20;

    long guavaStart = System.currentTimeMillis();
    var guava = Lists.partition(sample, partitions);
    var totalSizeGuava = guava.stream().map(List::size).reduce(Integer::sum).orElseThrow();
    long guavaEnd = System.currentTimeMillis();
    var totalGuavaTime = guavaEnd - guavaStart;

    long partitionedStart = System.currentTimeMillis();
    var partitioned = Lists.partition(sample, partitions);
    var totalSizeParitioned = partitioned.stream().map(List::size).reduce(Integer::sum)
      .orElseThrow();
    long partitionedEnd = System.currentTimeMillis();
    var totalTimePartitioned = partitionedEnd - partitionedStart;

    assertThat(totalTimePartitioned).isLessThanOrEqualTo(totalGuavaTime);
    assertThat(totalSizeParitioned).isEqualTo(totalSizeGuava);

  }

  //TODO: one by one this methods will be implemented
  @Test
  void shouldThrowUnsupportedOperationExceptionForAnyUnimplementedOperation() {
    var sample = sampleList(100);
    var partitioned = new PartitionedList<>(sample, 5);

    assertThrows(UnsupportedOperationException.class,
      () -> partitioned.contains(randomList()));
    assertThrows(UnsupportedOperationException.class,
      () -> partitioned.add(List.of(randomString())));
    assertThrows(UnsupportedOperationException.class,
      () -> partitioned.remove(List.of(randomString())));
    assertThrows(UnsupportedOperationException.class,
      () -> partitioned.containsAll(listOfLists()));
    assertThrows(UnsupportedOperationException.class,
      () -> partitioned.addAll(listOfLists()));
    assertThrows(UnsupportedOperationException.class,
      () -> partitioned.addAll(0, listOfLists()));
    assertThrows(UnsupportedOperationException.class,
      () -> partitioned.removeAll(listOfLists()));
    assertThrows(UnsupportedOperationException.class,
      () -> partitioned.retainAll(listOfLists()));
    assertThrows(UnsupportedOperationException.class,
      partitioned::clear);
    assertThrows(UnsupportedOperationException.class,
      () -> partitioned.set(0, randomList()));
    assertThrows(UnsupportedOperationException.class,
      () -> partitioned.add(0, randomList()));
    assertThrows(UnsupportedOperationException.class,
      () -> partitioned.remove(0));
    assertThrows(UnsupportedOperationException.class,
      () -> partitioned.indexOf(randomList()));
    assertThrows(UnsupportedOperationException.class,
      () -> partitioned.lastIndexOf(randomList()));
    assertThrows(UnsupportedOperationException.class,
      partitioned::listIterator);
    assertThrows(UnsupportedOperationException.class,
      () -> partitioned.listIterator(0));
    assertThrows(UnsupportedOperationException.class,
      () -> partitioned.subList(0, 1));

  }

  private static List<Integer> sampleIntList(int size) {
    return IntStream.range(0, size).boxed().toList();
  }

  private static List<List<String>> verifyAtCompileTimeThatIsListOfLists(
    PartitionedList<String> partitioned) {
    return partitioned.stream().toList();
  }

  private static int calculateSampleIndex(int partitionIndex, int partitionSize, int elementIndex) {
    return partitionIndex * partitionSize + elementIndex;
  }

  private static List<String> sampleList(int sampleSize) {
    return IntStream.range(0, sampleSize)
      .mapToObj(ignored -> randomString())
      .toList();

  }

  private List<String> randomList() {
    return List.of(randomString());
  }

  private Collection<List<String>> listOfLists() {
    return List.of(List.of(randomString()));
  }

}