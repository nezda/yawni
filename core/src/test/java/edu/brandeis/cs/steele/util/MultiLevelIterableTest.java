package edu.brandeis.cs.steele.util;

import org.junit.*;
import static org.junit.Assert.*;
import java.util.*;

public class MultiLevelIterableTest {

  @Test
  public void test1() {
    final List<Integer> empty = Collections.emptyList();
    final List<List<Integer>> emptyEmpty = new ArrayList<List<Integer>>();
    emptyEmpty.add(Collections.<Integer>emptyList());
    assertEquals(empty, list(MultiLevelIterable.of(emptyEmpty)));
    final List<Integer> ints1 = Arrays.asList(1,2,3);
    final List<Integer> ints2 = Arrays.asList(1,2,4);
    final List<List<Integer>> ints1Ints2 = Arrays.asList(ints1, ints2);
    final List<Integer> merged = Arrays.asList(1,2,3,1,2,4);
    assertEquals(merged, list(MultiLevelIterable.of(ints1Ints2)));
    final List<List<Integer>> ints1EmptyEmptyInts2 = Arrays.asList(ints1, empty, empty, ints2);
    assertEquals(merged, list(MultiLevelIterable.of(ints1EmptyEmptyInts2)));
    final List<List<Integer>> emptyInts1EmptyEmptyInts2Empty = 
      Arrays.asList(empty, ints1, empty, empty, ints2, empty);
    assertEquals(merged, list(MultiLevelIterable.of(emptyInts1EmptyEmptyInts2Empty)));
  }

  static <T> ArrayList<T> list(final Iterable<T> sequence) {
    final ArrayList<T> list = new ArrayList<T>();
    for (final T t : sequence) {
      list.add(t);
    }
    return list;
  }
}
