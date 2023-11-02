package com.github.awsjavakit.misc.lists;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class PartitionedList<T> implements List<List<T>> {

  private final List<T> contents;
  private final int partitionSize;
  private final int numberOfPartitions;

  public PartitionedList(List<T> contents, int partitionSize) {
    this.contents = contents;
    this.partitionSize = partitionSize;
    this.numberOfPartitions = calculateNumberOfPartitions(contents, partitionSize);
  }

  @Override
  public int size() {
    return this.numberOfPartitions;
  }

  @Override
  public boolean isEmpty() {
    return contents.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Iterator<List<T>> iterator() {
    return new PartitionedListIterator<>(this);
  }

  @Override
  public Object[] toArray() {
    var lists = this.stream().toList();
    return lists.toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    var lists = this.stream().toList();
    return lists.toArray(a);
  }

  @Override
  public boolean add(List<T> ts) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean remove(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean addAll(Collection<? extends List<T>> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean addAll(int index, Collection<? extends List<T>> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<T> get(int index) {
    var fromIndex = calculatePartitionStart(index);
    var untilIndex = calculatePartitionEndExclusive(index);
    return contents.subList(fromIndex, untilIndex);
  }

  @Override
  public List<T> set(int index, List<T> element) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void add(int index, List<T> element) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<T> remove(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int indexOf(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int lastIndexOf(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ListIterator<List<T>> listIterator() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ListIterator<List<T>> listIterator(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<List<T>> subList(int fromIndex, int toIndex) {
    throw new UnsupportedOperationException();
  }

  private int calculateNumberOfPartitions(List<T> contents, int partitionSize) {
    return (int) Math.ceil((double) contents.size() / (double) partitionSize);
  }

  private int calculatePartitionEndExclusive(int index) {
    var candidate = partitionSize * (index + 1);
    return Math.min(candidate, contents.size());

  }

  private int calculatePartitionStart(int partitionIndex) {
    if (partitionIndex >= this.size()) {
      throw indexOutOfBounds(partitionIndex);
    }
    return partitionIndex * partitionSize;
  }

  private IndexOutOfBoundsException indexOutOfBounds(int partitionIndex) {
    return new IndexOutOfBoundsException(formatMessage(partitionIndex));
  }

  private String formatMessage(int partitionIndex) {
    return String.format("Index %d is out of bounds for length %d", partitionIndex, size());
  }

}
