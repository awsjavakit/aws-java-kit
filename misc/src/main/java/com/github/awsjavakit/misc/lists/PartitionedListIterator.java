package com.github.awsjavakit.misc.lists;

import java.util.Iterator;
import java.util.List;

public class PartitionedListIterator<T> implements Iterator<List<T>> {

  private final PartitionedList<T> contents;
  private int currentIndex;

  PartitionedListIterator(PartitionedList<T> contents) {
    this.contents = contents;
  }

  @Override
  public boolean hasNext() {
    return currentIndex < contents.size();
  }

  @Override
  public List<T> next() {
    var output = contents.get(currentIndex);
    currentIndex++;
    return output;
  }
}