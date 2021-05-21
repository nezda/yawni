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

import org.junit.Before;
import org.junit.Test;
import org.yawni.wordnet.WordNetInterface;
import org.yawni.wordnet.WordNet;
import org.yawni.wordnet.POS;
import org.yawni.wordnet.Word;
import static org.fest.assertions.Assertions.assertThat;

public class WordCaseUtilsTest {
  private WordNetInterface dictionary;
  @Before
  public void init() {
    dictionary = WordNet.getInstance();
  }

  @Test
  public void test() {
    String lemma;
    Word word;
    lemma = "CD";
    word = getWord(lemma, POS.NOUN);
    System.err.println("case variants: "+WordCaseUtils.getUniqueLemmaCaseVariants(word));
    System.err.println("dominant case: "+WordCaseUtils.getDominantCasedLemma(word));
    assertThat(WordCaseUtils.getUniqueLemmaCaseVariants(word)).containsExactly("Cd", "cd", "CD");
    assertThat(WordCaseUtils.getDominantCasedLemma(word)).isEqualTo("cd");

    lemma = "roma";
    word = getWord(lemma, POS.NOUN);
    System.err.println("case variants: "+WordCaseUtils.getUniqueLemmaCaseVariants(word));
    System.err.println("dominant case: "+WordCaseUtils.getDominantCasedLemma(word));
    assertThat(WordCaseUtils.getUniqueLemmaCaseVariants(word)).containsExactly("Roma");
    assertThat(WordCaseUtils.getDominantCasedLemma(word)).isEqualTo("Roma");

    lemma = "rom";
    word = getWord(lemma, POS.NOUN);
    System.err.println("case variants: "+WordCaseUtils.getUniqueLemmaCaseVariants(word));
    System.err.println("dominant case: "+WordCaseUtils.getDominantCasedLemma(word));
    assertThat(WordCaseUtils.getUniqueLemmaCaseVariants(word)).containsExactly("ROM");
    assertThat(WordCaseUtils.getDominantCasedLemma(word)).isEqualTo("ROM");
  }

  private Word getWord(final String lemma, final POS pos) {
    return dictionary.lookupWord(lemma, pos);
  }
}