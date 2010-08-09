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
import static org.yawni.wordnet.GetIndex.baseNDigitX;

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
    assertEquals(2*2, alternate("x_y_z"));
    assertEquals(3*3, experimentalAlternate("x_y_z"));
  }
  @Test
  public void test4() {
    assertEquals(2*2*2, alternate("p_q_r_s"));
    assertEquals(3*3*3, experimentalAlternate("p_q_r_s"));
  }
  static int alternate(final String searchStr) {
    final GetIndex alternator = new GetIndex(searchStr, POS.NOUN, null);
    final char[] defaultAlternate = new char[] { '_', '-'};
    int i = -1;
    for (final CharSequence alt : alternator) {
//      System.err.println("alternation: " + alt);
      i++;
      final String altMsg = String.format("alternation[%d:%s]: %s",
        i, Integer.toString(i, defaultAlternate.length), alt);
      System.err.println(altMsg);
    }
//    for (final CharSequence alt : alternator) {
//      System.err.println("alternation': " + alt);
//    }
    return alternator.size();
  }

  static int experimentalAlternate(final String searchStr) {
    // note: get some weird behavior if we use backspace ('\b') as an alternate
    //experimentalAlternation[0:0]: x_y_z
    //experimentalAlternation[1:1]: x-y_z
    //experimentalAlternation[2:2]: y_z
    //experimentalAlternation[3:10]: x_y-z
    //experimentalAlternation[4:11]: x-y-z
    //experimentalAlternation[5:12]: y-z
    //experimentalAlternation[6:20]: x_z
    //experimentalAlternation[7:21]: x-z
    //experimentalAlternation[8:22]: z

    //experimentalAlternation[0:0]: x_y_z
    //experimentalAlternation[1:1]: x-y_z
    //experimentalAlternation[2:2]: x*y_z
    //experimentalAlternation[3:10]: x_y-z
    //experimentalAlternation[4:11]: x-y-z
    //experimentalAlternation[5:12]: x*y-z
    //experimentalAlternation[6:20]: x_y*z
    //experimentalAlternation[7:21]: x-y*z
    //experimentalAlternation[8:22]: x*y*z

    final char[] toAlternate = new char[] { '_', '-', '*' };
//    final char[] toAlternate = new char[] { '_', '-', '\b' };
    final GetIndex alternator = new GetIndex(searchStr, toAlternate);
    int i = -1;
    for (final CharSequence alt : alternator) {
//      System.err.println("experimentalAlternation: " + alt);//+ " fixed: "+alt.toString().replaceAll("\\b", ""));
      i++;
      final String altMsg = String.format("experimentalAlternation[%d:%s]: %s :: %s",
        i, Integer.toString(i, toAlternate.length), alt, alt.toString().replaceAll("\\*", ""));
      System.err.println(altMsg);
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

  @Test
  public void testBaseNDigitX() {
    assertEquals(3, baseNDigitX(123, 10, 0));
    assertEquals(2, baseNDigitX(123, 10, 1));
    assertEquals(1, baseNDigitX(123, 10, 2));

    assertEquals(1, baseNDigitX(5, 2, 0));
    assertEquals(0, baseNDigitX(5, 2, 1));
    assertEquals(1, baseNDigitX(5, 2, 2));

    assertEquals(0, baseNDigitX(5, 2, 3));

    assertEquals(8, baseNDigitX(0xf8, 16, 0));
    assertEquals(15, baseNDigitX(0xf8, 16, 1));
  }
}