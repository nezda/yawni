package edu.brandeis.cs.steele.util;

import junit.framework.JUnit4TestAdapter;
import org.junit.*;
import static org.junit.Assert.*;
import java.util.*;

public class UtilsTest {

  @Test
  public void testEquals() {
    Integer i1 = null;
    Integer i2 = null;
    assertTrue(Utils.equals(i1, i2));
    i1 = 1;
    assertFalse(Utils.equals(i1, i2));
    i1 = null;
    i2 = 2;
    assertFalse(Utils.equals(i1, i2));
    i1 = 1;
    i2 = new Integer(1);
    assertTrue(Utils.equals(i1, i2));
  }

  @Test
  public void testMismatch() {
    final List<Integer> empty = Collections.emptyList();
    assertEquals(0, Utils.mismatch(empty, 0, 0, empty, 0));
    final List<Integer> ints1 = Arrays.asList(1,2,3);
    final List<Integer> ints2 = Arrays.asList(1,2,4);
    assertEquals(ints1.size(), Utils.mismatch(ints1, 0, ints1.size(), ints1, 0));
    assertEquals(2, Utils.mismatch(ints1, 0, ints1.size(), ints2, 0));
    assertEquals(2, Utils.mismatch(ints1, 1, ints1.size(), ints2, 1));
    final List<Integer> ints3 = Arrays.asList(1,1,2,3);
    assertEquals(1, Utils.mismatch(ints3, 0, ints3.size(), ints1, 0));
    assertEquals(ints3.size(), Utils.mismatch(ints3, 1, ints3.size(), ints1, 0));
    assertEquals(ints1.size(), Utils.mismatch(ints1, 0, ints1.size(), ints3, 1));
    assertEquals(0, Utils.mismatch(ints1, 0, ints1.size(), empty, 0));
  }

  public static junit.framework.Test suite() {
    return new JUnit4TestAdapter(UtilsTest.class);
  }
}
