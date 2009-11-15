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
import org.yawni.util.CharSequences;
import static org.junit.Assert.*;

public class BloomFilterTest {
  private static final Hasher<CharSequence> CHARSEQUENCE_HASHER = new Hasher<CharSequence>() {
    public int hashCode(Object o) {
      if (false == (o instanceof String) &&
          o instanceof CharSequence) {
       final CharSequence seq = (CharSequence)o;
       return CharSequences.hashCode(seq);
     } else {
       return o.hashCode();
     }
    }
  };
  @Test
  public void testRandomIntegers() {
    final int size = 100000;
    final double ONE_PERCENT = 0.01;
    final double TEN_PERCENT = 0.1;
    final double desiredFpFatio = ONE_PERCENT;
    final BloomFilter<Integer> filter = new BloomFilter<Integer>(size, desiredFpFatio);

    System.err.println("filter: "+filter);

    assertFalse("3 is in this empty filter? "+filter, filter.contains(3));

    final Set<Integer> hard = new HashSet<Integer>(size);

    final Random rand = new Random(0);
    while (hard.size() != size) {
      final int next = rand.nextInt();
      hard.add(next);
      filter.add(next);
//      System.err.println("next: "+next+" "+filter);
    }
    // extremely unlikey
    assertFalse("4 is not in this filter "+filter.toString(), filter.contains(4));

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
  
  @Test
  public void testRandomNumericStrings() {
    final int size = 100000;
    final double ONE_PERCENT = 0.01;
    final double TEN_PERCENT = 0.1;
    final double desiredFpFatio = ONE_PERCENT;
    final BloomFilter<CharSequence> filter = new BloomFilter<CharSequence>(size, desiredFpFatio, CHARSEQUENCE_HASHER);

    System.err.println("filter: "+filter);

    assertFalse("\"3\" is in this empty filter? "+filter, filter.contains(String.valueOf(3)));

    final Set<Integer> hard = new HashSet<Integer>(size);

    final Random rand = new Random(0);
    while (hard.size() != size) {
      final int next = rand.nextInt();
      hard.add(next);
      filter.add(String.valueOf(next));
//      System.err.println("next: "+next+" "+filter);
    }
    // extremely unlikey
    assertFalse("\"4\" is not in this filter "+filter.toString(), filter.contains(String.valueOf(4)));

    for (final Integer i : hard) {
      // test CharSequence's weird hashCode
      // i: "-1557994400" expected:<-469633325> but was:<468828627>
      assertEquals("i: "+i, String.valueOf(i).hashCode(),
        CharSequences.hashCode(new StringBuilder(String.valueOf(i))));
      assertTrue(filter.contains(String.valueOf(i)));
      assertTrue(filter.contains(new StringBuilder(String.valueOf(i))));
    }

    int falsePositives = 0;
    final int n = 10 * size;
    for (int i = 0; i < n; i++) {
      final int next = rand.nextInt();
//      System.err.println("next: "+next);
      final boolean inHard = hard.contains(next);
      final boolean inFilter = filter.contains(String.valueOf(next));
      if (! inHard && inFilter) {
        falsePositives++;
      }
    }
    final double fpRatio = falsePositives / (double)n;
    System.err.printf("n: %,d falsePositives: %,d n/falsePositives: %.4f\n",
      n, falsePositives, fpRatio);
    //assert fpRatio <
  }

  @Test
  public void testShortSequentialNumericalStrings() {
    final int size = 1000000;
    final double ONE_PERCENT = 0.01;
    final double TEN_PERCENT = 0.1;
    final double desiredFpFatio = ONE_PERCENT;
    final BloomFilter<CharSequence> filter = new BloomFilter<CharSequence>(size, desiredFpFatio, CHARSEQUENCE_HASHER);

    System.err.println("filter: "+filter);

    assertFalse("\"3\" is in this empty filter? "+filter, filter.contains(String.valueOf(3)));

    final Set<Integer> hard = new HashSet<Integer>(size);

    for (int i = 0; i < size; i++) {
      final int next = i;
      hard.add(next);
      filter.add(String.valueOf(next));
//      System.err.println("next: "+next+" "+filter);
    }

    for (final Integer i : hard) {
      // test CharSequence's weird hashCode
      // i: "-1557994400" expected:<-469633325> but was:<468828627>
      assertEquals("i: "+i, String.valueOf(i).hashCode(),
        CharSequences.hashCode(new StringBuilder(String.valueOf(i))));
      assertTrue(filter.contains(String.valueOf(i)));
      assertTrue(filter.contains(new StringBuilder(String.valueOf(i))));
    }

    int falsePositives = 0;
    final int n = 10 * size;
    for (int i = 0; i < n; i++) {
      final int next = i;
//      System.err.println("next: "+next);
      final boolean inHard = hard.contains(next);
      final boolean inFilter = filter.contains(String.valueOf(next));
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