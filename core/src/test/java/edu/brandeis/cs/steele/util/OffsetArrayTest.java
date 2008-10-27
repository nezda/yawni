package edu.brandeis.cs.steele.util;

import junit.framework.JUnit4TestAdapter;
import org.junit.*;
import static org.junit.Assert.*;
import java.util.*;

public class OffsetArrayTest {
  @Test
  public void basics() {
    OffsetArray a1 = new OffsetArray(4);
    a1.set(0, 0);
    a1.set(1, 1);
    a1.set(2, 2);
    a1.set(3, 3);
    //System.err.println(a1);
    assertEquals(a1.get(0), 0);
    assertEquals(a1.get(1), 1);
    assertEquals(a1.get(2), 2);
    assertEquals(a1.get(3), 3);

    OffsetArray a2 = new OffsetArray(4);
    a2.set(0, 0);
    a2.set(1, 1);
    a2.set(2, 2);
    a2.set(3, 3);

    assertEquals(a1, a2);
  }

  @Test
  public void linearMarch() {
    final int len = 2048;
    OffsetArray a1 = new OffsetArray(len);
    for(int i=0; i<len; i++) {
      a1.set(i, i);
    }
    for(int i=0; i<len; i++) {
      assertEquals(a1.get(i), i);
    }
  }

  @Test
  public void empty() {
    OffsetArray a1 = new OffsetArray(0);
    //System.err.println(a1);
    assertEquals(Arrays.toString(new int[0]), a1.toString());
    assertTrue(a1.isEmpty());
    assertEquals(a1.size(), 0);
  }

  @Test (expected=ArrayIndexOutOfBoundsException.class)
  public void outOfBounds() {
    OffsetArray a1 = new OffsetArray(0);
    a1.set(1, 1);
  }
  
  @Test (expected=IllegalArgumentException.class)
  public void overflow() {
    OffsetArray a1 = new OffsetArray(1);
    a1.set(0, Integer.MAX_VALUE); 
  }

  @Test (expected=IllegalArgumentException.class)
  public void negative() {
    OffsetArray a1 = new OffsetArray(1);
    a1.set(0, -1);
  }

  @Test
  public void toStringDoesntThrow() {
    OffsetArray a1 = new OffsetArray(4);
    a1.set(0, 0);
    a1.set(1, 1);
    a1.set(2, 2);
    a1.set(3, 3);
    String toString = a1.toString();
  }

  /**
   * Demonstrates uses of asIntegerList()
   */
  @Test
  public void asIntegerList() {
    OffsetArray descending = new OffsetArray(4);
    descending.set(0, 3);
    descending.set(1, 2);
    descending.set(2, 1);
    descending.set(3, 0);
    //System.err.println(descending);
    List<Integer> twoOne = descending.asIntegerList().subList(1, 3);
    //System.err.println(twoOne);
    // sort the sublist
    Collections.sort(twoOne);
    //System.err.println(twoOne);
    //System.err.println(descending);
    assertEquals(descending.get(0), 3);
    assertEquals(descending.get(1), 1);
    assertEquals(descending.get(2), 2);
    assertEquals(descending.get(3), 0);

    Collections.sort(descending.asIntegerList());
    assertEquals(descending.get(0), 0);
    assertEquals(descending.get(1), 1);
    assertEquals(descending.get(2), 2);
    assertEquals(descending.get(3), 3);
  }

  public static junit.framework.Test suite() {
    return new JUnit4TestAdapter(OffsetArrayTest.class);
  }
}
