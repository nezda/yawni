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
package org.yawni.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static com.google.common.collect.Iterables.isEmpty;
import static com.google.common.collect.Iterables.elementsEqual;
import com.google.common.collect.Iterators;
import static org.junit.Assert.*;

public class UtilsTest {
  @Test
  public void testMismatch() {
    final List<Integer> empty = Collections.emptyList();
    assertEquals(0, Utils.mismatch(empty, 0, 0, empty, 0));
    final List<Integer> ints1 = Arrays.asList(1, 2, 3);
    final List<Integer> ints2 = Arrays.asList(1, 2, 4);
    assertEquals(ints1.size(), Utils.mismatch(ints1, 0, ints1.size(), ints1, 0));
    assertEquals(2, Utils.mismatch(ints1, 0, ints1.size(), ints2, 0));
    assertEquals(2, Utils.mismatch(ints1, 1, ints1.size(), ints2, 1));
    final List<Integer> ints3 = Arrays.asList(1, 1, 2, 3);
    assertEquals(1, Utils.mismatch(ints3, 0, ints3.size(), ints1, 0));
    assertEquals(ints3.size(), Utils.mismatch(ints3, 1, ints3.size(), ints1, 0));
    assertEquals(ints1.size(), Utils.mismatch(ints1, 0, ints1.size(), ints3, 1));
    assertEquals(0, Utils.mismatch(ints1, 0, ints1.size(), empty, 0));
  }

  @Test
  public void testIsSorted() {
    final List<Integer> empty = Collections.emptyList();
    assertTrue(Utils.isSorted(empty));
    final List<Integer> ints1 = Arrays.asList(1, 2, 3);
    assertTrue(Utils.isSorted(ints1));
    assertTrue(Utils.isSorted(ints1.iterator()));
    final List<Integer> ints2 = Arrays.asList(3, 2, 1);
    assertFalse(Utils.isSorted(ints2));
    assertFalse(Utils.isSorted(ints2.iterator()));
  }

  @Test
  public void testStartsWith() {
    assertTrue(CharSequences.startsWith("'hood", "'ho"));
    assertFalse(CharSequences.startsWith("'ho", "'hood"));
  }

  @Test
  public void testUniq() {
    final List<Integer> empty = Collections.emptyList();
    assertTrue(Utils.isUnique(Utils.uniq(empty)));
    assertTrue(isEmpty(Utils.uniq(empty)));
    final List<Integer> ints1 = Arrays.asList(1, 2, 3);
    final List<Integer> ints1Dups = Arrays.asList(1, 2, 2, 3);
    assertTrue(Utils.isSorted(ints1));
    assertTrue(Utils.isSorted(ints1Dups));
    assertTrue(Utils.isSorted(ints1.iterator()));
    assertFalse(Utils.isUnique(ints1Dups));
    assertFalse(isEmpty(ints1Dups));
    assertTrue(Utils.isUnique(Utils.uniq(ints1Dups)));
    final Iterable<Integer> uniqd = Utils.uniq(ints1Dups);
    assertFalse(isEmpty(Utils.uniq(ints1Dups)));
  }

  @Test
  public void testIteratorEquals() {
    final Iterable<Integer> EMPTY = Collections.emptyList();
    final Iterable<Integer> ONE_TWO_THREE = Arrays.asList(1, 2, 3);
    final Iterable<Integer> ONE_TWO_THREE_THREE = Arrays.asList(1, 2, 3, 3);

    assertTrue(elementsEqual(ONE_TWO_THREE, ONE_TWO_THREE));
    assertTrue(Iterators.elementsEqual(ONE_TWO_THREE.iterator(), ONE_TWO_THREE.iterator()));
    assertTrue(Iterators.elementsEqual(EMPTY.iterator(), EMPTY.iterator()));
    assertTrue(elementsEqual(EMPTY, EMPTY));
    assertTrue(elementsEqual(new ArrayList<String>(), new ArrayList<String>()));
    assertFalse(Iterators.elementsEqual(ONE_TWO_THREE.iterator(), EMPTY.iterator()));
    assertFalse(elementsEqual(ONE_TWO_THREE, ONE_TWO_THREE_THREE));
    assertFalse(Iterators.elementsEqual(ONE_TWO_THREE.iterator(), ONE_TWO_THREE_THREE.iterator()));
  }
}