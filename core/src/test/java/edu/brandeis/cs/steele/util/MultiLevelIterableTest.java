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

import static edu.brandeis.cs.steele.util.Utils.asList;

public class MultiLevelIterableTest {

  @Test
  public void test1() {
    final List<Integer> empty = Collections.emptyList();
    final List<List<Integer>> emptyEmpty = new ArrayList<List<Integer>>();
    emptyEmpty.add(Collections.<Integer>emptyList());
    assertEquals(empty, asList(MultiLevelIterable.of(emptyEmpty)));
    final List<Integer> ints1 = Arrays.asList(1, 2, 3);
    final List<Integer> ints2 = Arrays.asList(1, 2, 4);
    @SuppressWarnings("unchecked")
    final List<List<Integer>> ints1Ints2 = Arrays.asList(ints1, ints2);
    final List<Integer> merged = Arrays.asList(1, 2, 3, 1, 2, 4);
    assertEquals(merged, asList(MultiLevelIterable.of(ints1Ints2)));
    @SuppressWarnings("unchecked")
    final List<List<Integer>> ints1EmptyEmptyInts2 = Arrays.asList(ints1, empty, empty, ints2);
    assertEquals(merged, asList(MultiLevelIterable.of(ints1EmptyEmptyInts2)));
    @SuppressWarnings("unchecked")
    final List<List<Integer>> emptyInts1EmptyEmptyInts2Empty = 
      Arrays.asList(empty, ints1, empty, empty, ints2, empty);
    assertEquals(merged, asList(MultiLevelIterable.of(emptyInts1EmptyEmptyInts2Empty)));
  }
}
