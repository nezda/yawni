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
}
