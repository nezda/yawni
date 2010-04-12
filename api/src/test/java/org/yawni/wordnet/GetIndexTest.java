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
package org.yawni.wordnet;

import org.junit.*;
import static org.junit.Assert.*;

// main things to test for:
// - produces all combinations
// - produces no duplicates

public class GetIndexTest {
  private static WordNetInterface wordNet;
  private static Morphy morphy;
  @BeforeClass
  public static void init() {
    wordNet = WordNet.getInstance();
    morphy = ((WordNet)wordNet).morphy;
  }
  @Test
  public void test2() {
    System.err.println("test2() "+morphy);
    assertEquals(2, alternate("a_b"));
  }
  @Test
  public void test3() {
    System.err.println("test3() "+morphy);
    assertEquals(4, alternate("x_y_z"));
  }
  @Test
  public void test4() {
    assertEquals(8, alternate("p_q_r_s"));
  }
  static int alternate(final String searchStr) {
    final GetIndex alternator = new GetIndex(searchStr, POS.NOUN, null);
    for (final CharSequence alt : alternator) {
      System.err.println("alternation: " + alt);
    }
//    for (final CharSequence alt : alternator) {
//      System.err.println("alternation': " + alt);
//    }
    return alternator.size();
  }
//  @Test
//  public void test5() {
//    assertEquals(16, GetIndex.alternate("d_e_f_g_h"));
//  }
//  @Test
//  public void testExplore() {
//    //XXX NOTE THE UNDERSCORES
//    System.err.println("testExplore() "+morphy);
//    new GetIndex("internal-combustion_engine", POS.NOUN, morphy);
////    GetIndex.alternate("internal-combustion_engine");
////    GetIndex.alternate("I_ran");
////    GetIndex.alternate("be_an");
//
////    GetIndex.alternate("m_n_o_p");
//  }
}