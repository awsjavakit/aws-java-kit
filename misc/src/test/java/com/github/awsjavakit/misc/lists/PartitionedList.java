package com.github.awsjavakit.misc.lists;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Spliterators;

public class PartitionedList<T> implements List<List<T>> {

  private final List<T> contents;
  private final int partitionSize;

  public PartitionedList(List<T> list, int partitionSize) {
    this.contents = list;
    this.partitionSize = partitionSize;
  }

  @Override
  public int size() {
    return 0;
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public boolean contains(Object o) {
    return false;
  }

  @Override
  public Iterator<List<T>> iterator() {
    return
  }

  @Override
  public Object[] toArray() {
    return new Object[0];
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return null;
  }

  @Override
  public boolean add(List<T> ts) {
    return false;
  }

  @Override
  public boolean remove(Object o) {
    return false;
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return false;
  }

  @Override
  public boolean addAll(Collection<? extends List<T>> c) {
    return false;
  }

  @Override
  public boolean addAll(int index, Collection<? extends List<T>> c) {
    return false;
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    return false;
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    return false;
  }

  @Override
  public void clear() {

  }

  @Override
  public List<T> get(int index) {
    return null;
  }

  @Override
  public List<T> set(int index, List<T> element) {
    return null;
  }

  @Override
  public void add(int index, List<T> element) {

  }

  @Override
  public List<T> remove(int index) {
    return null;
  }

  @Override
  public int indexOf(Object o) {
    return 0;
  }

  @Override
  public int lastIndexOf(Object o) {
    return 0;
  }

  @Override
  public ListIterator<List<T>> listIterator() {
    return null;
  }

  @Override
  public ListIterator<List<T>> listIterator(int index) {
    return null;
  }

  @Override
  public List<List<T>> subList(int fromIndex, int toIndex) {
    return null;
  }
}
