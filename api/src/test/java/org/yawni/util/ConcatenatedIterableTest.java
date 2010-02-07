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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;
import org.junit.Test;

import static org.yawni.util.Utils.asList;

public class ConcatenatedIterableTest {
  @Test
  public void test1() {
    final List<Integer> empty = Collections.emptyList();
    assertEquals(empty, asList(ConcatenatedIterable.concat(empty, empty)));
    final List<Integer> ints1 = Arrays.asList(1, 2, 3);
    final List<Integer> ints2 = Arrays.asList(1, 2, 4);
    final List<Integer> merged = Arrays.asList(1, 2, 3, 1, 2, 4);
    assertEquals(merged, asList(ConcatenatedIterable.concat(ints1, ints2)));
    assertEquals(merged, asList(ConcatenatedIterable.concat(empty, ints1, empty, ints2, empty)));
  }

  @Test
  public void test2() {
    final List<Integer> empty = Collections.emptyList();
    assertEquals(empty, asList(ConcatenatedIterable.concat(empty, empty)));
    final List<Integer> ints1 = Arrays.asList(1, 2, 3);
    final List<Integer> ints2 = Arrays.asList(1, 2, 4);
    final List<Integer> ints3 = Arrays.asList(1, 2, 5);
    final List<Integer> ints4 = Arrays.asList(1, 2, 6);
    final List<Integer> merged = Arrays.asList(1, 2, 3, 1, 2, 4, 1, 2, 5, 1, 2, 6);
    assertEquals(merged, asList(ConcatenatedIterable.concat(ints1, ints2, ints3, ints4)));
  }
}