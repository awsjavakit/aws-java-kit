package com.github.awsjavakit.misc.lists;

import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomInteger;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class PartitionedListTest {


  public static final int SAMPLE_SIZE = 100;

  @Test
  void shouldAcceptAListAndThePartitionSize() {
    var sample = sampleList(SAMPLE_SIZE);
    int partitionSize = randomInteger(SAMPLE_SIZE);
    assertDoesNotThrow(() -> new PartitionedList<>(sample, partitionSize));
  }

  @Test
  void shouldReturnTheJthElementOfTheIthPartition() {
    var sample = sampleList(100);
    var partitionSize = 5;
    var partitionIndex = 4;
    var elementIndex = 6;
    var expectedItem = sample.get(
      calculateSampleIndex(partitionIndex, partitionSize, elementIndex));
    var partitioned = new PartitionedList<>(sample, partitionSize);
    assertThat(partitioned.get(partitionIndex).get(elementIndex)).isEqualTo(expectedItem);
  }

  //TODO: one by one this methods will be implemented
  @Test
  void shouldThrowUnsupportedOperationExceptionForAnyUnimplementedOperation() {
    var sample = sampleList(100);
    var partitioned = new PartitionedList<>(sample, 5);

    assertThrows(UnsupportedOperationException.class, partitioned::size);
    assertThrows(UnsupportedOperationException.class, partitioned::isEmpty);
    assertThrows(UnsupportedOperationException.class,
      () -> partitioned.contains(randomList()));
    assertThrows(UnsupportedOperationException.class, partitioned::iterator);
    assertThrows(UnsupportedOperationException.class, partitioned::toArray);
    assertThrows(UnsupportedOperationException.class, () -> partitioned.toArray(new List[0]));
    assertThrows(UnsupportedOperationException.class,
      () -> partitioned.add(List.of(randomString())));
    assertThrows(UnsupportedOperationException.class,
      () -> partitioned.remove(List.of(randomString())));
    assertThrows(UnsupportedOperationException.class,
      () -> partitioned.containsAll(listOfLists()));
    assertThrows(UnsupportedOperationException.class,
      () -> partitioned.addAll(listOfLists()));
    assertThrows(UnsupportedOperationException.class,
      () -> partitioned.addAll(0,listOfLists()));
    assertThrows(UnsupportedOperationException.class,
      () -> partitioned.removeAll(listOfLists()));
    assertThrows(UnsupportedOperationException.class,
      () -> partitioned.retainAll(listOfLists()));
    assertThrows(UnsupportedOperationException.class,
      partitioned::clear);
    assertThrows(UnsupportedOperationException.class,
      () -> partitioned.set(0,randomList()));
    assertThrows(UnsupportedOperationException.class,
      () -> partitioned.add(0,randomList()));
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
      () -> partitioned.subList(0,1));

  }

  private List<String> randomList() {
    return List.of(randomString());
  }

  private static int calculateSampleIndex(int partitionIndex, int partitionSize, int elementIndex) {
    return partitionIndex * partitionSize + elementIndex;
  }

  private Collection<List<String>> listOfLists() {
    return List.of(List.of(randomString()));
  }

  private List<String> sampleList(int sampleSize) {
    return IntStream.range(0, sampleSize)
      .mapToObj(ignored -> randomString())
      .toList();

  }

}