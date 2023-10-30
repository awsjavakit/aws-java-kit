package com.github.awsjavakit.misc.lists;

import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomInteger;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class PartitionedListTest {

  public static final int A_IN_ASCII = 97;
  public static final int Z_IN_ASCII = 122;

  @Test
  void shouldSplitTheListToPartitionsOfDefinedSize() {
    var input = allLetters();
    var size = 1 + randomInteger(26);
    var partitioned = new PartitionedList<>(input, size);
    for (var partition : partitioned) {
      assertThat(partition.size()).isLessThanOrEqualTo(size);
    }

  }

  private static List<String> allLetters() {
    return IntStream.range(A_IN_ASCII, Z_IN_ASCII + 1).boxed()
      .map(Character::toString).toList();
  }

}