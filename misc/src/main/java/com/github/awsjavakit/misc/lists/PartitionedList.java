package com.github.awsjavakit.misc.lists;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class PartitionedList<T> implements List<List<T>> {

  private final List<T> contents;
  private final int partitionSize;

  public PartitionedList(List<T> contents, int partitionSize) {
    this.contents = contents;
    this.partitionSize = partitionSize;
  }

  @Override
  public int size() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isEmpty() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean contains(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Iterator<List<T>> iterator() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object[] toArray() {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    throw new UnsupportedOperationException();
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


  private int calculatePartitionEndExclusive(int index) {
    var candidate = partitionSize * (index + 1);
    return Math.max(candidate, contents.size());

  }

  private int calculatePartitionStart(int partitionIndex) {
    return partitionIndex * partitionSize;
  }
}
