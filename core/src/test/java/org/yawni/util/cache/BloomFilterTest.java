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
package org.yawni.util.cache;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

public class BloomFilterTest {
  @Test
  public void test1() {
    final int size = 100;
    final float ONE_PERCENT = 0.01f;
    final float TEN_PERCENT = 0.1f;
    final float desiredFpFatio = ONE_PERCENT;
    final BloomFilter<Integer> filter = new BloomFilter<Integer>(size, desiredFpFatio);

    System.err.println("filter: "+filter);

    assertFalse(filter.contains(3));

    final Set<Integer> hard = new HashSet<Integer>(size);

    final Random rand = new Random(0);
    while (hard.size() != size) {
      final int next = rand.nextInt();
      hard.add(next);
      filter.add(next);
      System.err.println("next: "+next+" "+filter);
    }
    // extremely unlikey
    assertFalse(filter.toString(), filter.contains(4));

    for (final Integer i : hard) {
      assertTrue(filter.contains(i));
    }

    int falsePositives = 0;
    final int n = 10 * size;
    for (int i = 0; i < n; i++) {
      final int next = rand.nextInt();
//      System.err.println("next: "+next);
      final boolean inHard = hard.contains(next);
      final boolean inFilter = filter.contains(next);
      if (! inHard && inFilter) {
        falsePositives++;
      }
    }
    final double fpRatio = falsePositives / (double)n;
    System.err.printf("n: %,d falsePositives: %,d n/falsePositives: %.4f\n", 
      n, falsePositives, fpRatio);
    //assert fpRatio <
  }
}