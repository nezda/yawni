package edu.brandeis.cs.steele.util;

import junit.framework.JUnit4TestAdapter;
import org.junit.*;
import static org.junit.Assert.*;
import java.util.*;

public class MergedIterableTest {

  @Test
  public void test1() {
    final List<Integer> empty = Collections.emptyList();
    assertEquals(empty, list(MergedIterable.merge(empty, empty)));
    final List<Integer> ints1 = Arrays.asList(1,2,3);
    final List<Integer> ints2 = Arrays.asList(1,2,4);
    final List<Integer> merged = Arrays.asList(1,1,2,2,3,4);
    assertEquals(merged, list(MergedIterable.merge(ints1, ints2)));
    assertEquals(merged, list(MergedIterable.merge(true, ints1, ints2)));
  }

  @Test(expected=IllegalArgumentException.class)
  public void testValidateSort1() {
    final List<Integer> ints1Rev = Arrays.asList(3,2,1);
    final List<Integer> ints2 = Arrays.asList(1,2,4);
    MergedIterable.merge(true, ints1Rev, ints2);
  }
  @Test(expected=IllegalArgumentException.class)
  public void testValidateSort2() {
    final List<Integer> ints1Rev = Arrays.asList(3,2,1);
    final List<Integer> ints2 = Arrays.asList(1,2,4);
    MergedIterable.merge(true, ints2, ints1Rev);
  }

  static <T> ArrayList<T> list(final Iterable<T> sequence) {
    final ArrayList<T> list = new ArrayList<T>();
    for (final T t : sequence) {
      list.add(t);
    }
    return list;
  }

  public static junit.framework.Test suite() {
    return new JUnit4TestAdapter(MergedIterableTest.class);
  }
}
