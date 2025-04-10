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
package org.yawni.wordnet;

import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.fest.assertions.Assertions.assertThat;

public class WordTest {
  private static WordNetInterface wordNet;
  @BeforeClass
  public static void init() {
    wordNet = WordNet.getInstance();
  }

  @Test
  public void test0() {
    final Word result = wordNet.lookupWord("running-away", POS.VERB);
    final List<WordSense> result2 = wordNet.lookupWordSenses("running-away", POS.VERB);
    final List<WordSense> result3 = wordNet.lookupWordSenses("running-away", POS.ALL);
    //System.err.println("test0 result : " + result);
    //System.err.println("test0 result2: " + result2);
    //System.err.println("test0 result3: " + result3);
    // running-away is not a VERB lemma
    assertThat(result == null).isTrue();
    assertThat(result2).isNotEmpty();
  }

  @Test
  public void countCollocations() {
    System.err.println("countCollocations");
    final int[] lengths = new int[30];
    final int[] dashes = new int[30];
    final int[] periods = new int[30];
    for (final Word word : wordNet.words(POS.ALL)) {
      final int currLen = Morphy.countWords(word.getLowercasedLemma(), '-');
      lengths[currLen]++;
      dashes[count(word.getLowercasedLemma(), '-')]++;
//      if (dashes[5] == 1) {
//        //kiss-me-over-the-garden-gate
//        System.err.println("word: "+word.getLowercasedLemma());
//        break;
//      }
      periods[count(word.getLowercasedLemma(), '.')]++;
//      if (periods[4] == 1) {
//        //d.p.r.k.
//        System.err.println("word: "+word.getLowercasedLemma());
//        break;
//      }
    }
    assertTrue("no token-length 1 Words?", lengths[1] > 0);
    assertTrue("no token-length 2 Words?", lengths[2] > 0);
    assertTrue("no token-length 3 Words?", lengths[3] > 0);

    assertTrue("no 1-dash Words?", dashes[1] > 0);
    assertTrue("no 2-dash Words?", dashes[2] > 0);
    assertTrue("no 3-dash Words?", dashes[3] > 0);

    for (int i = 0; i < lengths.length; i++) {
      if (lengths[i] != 0) {
        System.err.printf("length: %3s count: %5s\n", i, lengths[i]);
      }
    }
    for (int i = 1; i < dashes.length; i++) {
      if (dashes[i] != 0) {
        System.err.printf("dashes: %3s count: %5s\n", i, dashes[i]);
      }
    }
    for (int i = 1; i < periods.length; i++) {
      if (periods[i] != 0) {
        System.err.printf("periods: %3s count: %5s\n", i, periods[i]);
      }
    }
  }

  private static int count(CharSequence seq, char c) {
    int count = 0;
    for (int i = 0, n = seq.length(); i < n; i++) {
      if (c == seq.charAt(i)) {
        count++;
      }
    }
    return count;
  }
}