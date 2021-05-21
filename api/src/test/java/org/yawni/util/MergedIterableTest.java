/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.yawni.util;

import org.junit.*;
import static org.junit.Assert.*;

import java.util.*;

import static com.google.common.collect.Lists.newArrayList;

public class MergedIterableTest {
  @Test
  public void test1() {
    final List<Integer> empty = Collections.emptyList();
    assertEquals(empty, newArrayList(MergedIterable.merge(empty, empty)));
    final List<Integer> ints1 = Arrays.asList(1, 2, 3);
    final List<Integer> ints2 = Arrays.asList(1, 2, 4);
    final List<Integer> merged = Arrays.asList(1, 1, 2, 2, 3, 4);
    assertEquals(merged, newArrayList(MergedIterable.merge(ints1, ints2)));
    assertEquals(merged, newArrayList(MergedIterable.merge(empty, ints1, empty, ints2, empty)));
    assertEquals(merged, newArrayList(MergedIterable.merge(true, ints1, ints2)));
  }

  @Test
  public void test2() {
    final List<Integer> empty = Collections.emptyList();
    assertEquals(empty, newArrayList(MergedIterable.merge(empty, empty)));
    final List<Integer> ints1 = Arrays.asList(1, 2, 3);
    final List<Integer> ints2 = Arrays.asList(1, 2, 3);
    final List<Integer> ints3 = Arrays.asList(1, 2, 3);
    final List<Integer> ints4 = Arrays.asList(1, 2, 3);
    final List<Integer> merged = Arrays.asList(1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3);
    assertEquals(merged, newArrayList(MergedIterable.merge(ints1, ints2, ints3, ints4)));
    assertEquals(merged, newArrayList(MergedIterable.merge(true, ints1, ints2, ints3, ints4)));
  }

  @Test(expected=IllegalArgumentException.class)
  public void testValidateSort1() {
    final List<Integer> ints1Rev = Arrays.asList(3, 2, 1);
    final List<Integer> ints2 = Arrays.asList(1, 2, 4);
    MergedIterable.merge(true, ints1Rev, ints2);
  }

  @Test(expected=IllegalArgumentException.class)
  public void testValidateSort2() {
    final List<Integer> ints1Rev = Arrays.asList(3, 2, 1);
    final List<Integer> ints2 = Arrays.asList(1, 2, 4);
    MergedIterable.merge(true, ints2, ints1Rev);
  }
}