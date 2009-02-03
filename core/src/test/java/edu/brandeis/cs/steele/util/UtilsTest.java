/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package edu.brandeis.cs.steele.util;

import java.nio.CharBuffer;
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

  @Test
  public void testIsSorted() {
    final List<Integer> empty = Collections.emptyList();
    assertEquals(true, Utils.isSorted(empty));
    final List<Integer> ints1 = Arrays.asList(1,2,3);
    assertEquals(true, Utils.isSorted(ints1));
    assertEquals(true, Utils.isSorted(ints1.iterator()));
    final List<Integer> ints2 = Arrays.asList(3,2,1);
    assertEquals(false, Utils.isSorted(ints2));
    assertEquals(false, Utils.isSorted(ints2.iterator()));
  }
  
  @Test
  public void testStartsWith() {
    assertTrue(Utils.startsWith("'hood", "'ho"));
    assertFalse(Utils.startsWith("'ho", "'hood"));
  }

  @Test
  public void testUniq() {
    final List<Integer> empty = Collections.emptyList();
    assertTrue(Utils.isUnique(Utils.uniq(empty)));
    assertTrue(Utils.isEmpty(Utils.uniq(empty)));
    final List<Integer> ints1 = Arrays.asList(1,2,3);
    final List<Integer> ints1Dups = Arrays.asList(1,2,2,3);
    assertEquals(true, Utils.isSorted(ints1));
    assertEquals(true, Utils.isSorted(ints1Dups));
    assertEquals(true, Utils.isSorted(ints1.iterator()));
    assertFalse(Utils.isUnique(ints1Dups));
    assertFalse(Utils.isEmpty(ints1Dups));
    assertTrue(Utils.isUnique(Utils.uniq(ints1Dups)));
    //System.err.println("pre uniq'd: "+asList(ints1Dups));
    final Iterable<Integer> uniqd = Utils.uniq(ints1Dups);
    //System.err.println("uniq'd: "+asList(uniqd));
    //System.err.println("uniq'd: "+asList(uniqd));
    assertFalse(Utils.isEmpty(Utils.uniq(ints1Dups)));
  }

  @Test
  public void testCharSequenceTokenizer() {
    final String s = "someString";
    final CharBuffer cb = CharBuffer.wrap(s);
    // String, StringBuilder/StringBuffer, and CharBuffer are all incompatible with
    // respect to equals() AND hashCode()
    assertFalse(s.equals(cb));
    assertTrue(s.hashCode() != cb.hashCode());
  }

  private static <T> List<T> asList(final Iterable<T> iterable) {
    final List<T> list = new ArrayList<T>();
    for(final T t : iterable) {
      list.add(t);
    }
    return list;
  }
}
