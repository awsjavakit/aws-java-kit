package com.github.awsjavakit.identifiers;

import static com.github.awsjavakit.testingutils.RandomDataGenerator.randomString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SortableIdentifierTest {

  public static final int ELIMINATE_PROBABILITY_OF_ACCIDENTAL_SUCCESS = 5;

  @Test
  void shouldBeUniversallyUnique() {
    var s1 = SortableIdentifier.create();
    var uuid = s1.getUuid();

    assertThat(uuid.toString()).isSubstringOf(s1.toString());
  }

  @Test
  void shouldBeSortableBasedOnWhenItWasCreated() throws InterruptedException {
    for (int i = 0; i < ELIMINATE_PROBABILITY_OF_ACCIDENTAL_SUCCESS; i++) {
      var s1 = SortableIdentifier.create();
      Thread.sleep(ELIMINATE_PROBABILITY_OF_ACCIDENTAL_SUCCESS);
      var s2 = SortableIdentifier.create();
      assertThat(s1.toString()).isLessThan(s2.toString());
    }
  }

  @Test
  void shouldBeEqualWhenContainingSameValue() {
    var s1 = SortableIdentifier.create();
    var s2 = new SortableIdentifier(s1.toString());
    assertThat(s1).isEqualTo(s2);
    assertThat(s1.hashCode()).isEqualTo(s2.hashCode());
    assertThat(s1).isNotSameAs(s2);
  }

  @Test
  void shouldRejectNotValidIdentifiers() {
    assertThrows(RuntimeException.class, () -> new SortableIdentifier(randomString()));
  }

}