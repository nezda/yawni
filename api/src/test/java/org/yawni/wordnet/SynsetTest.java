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

import org.junit.BeforeClass;
import org.junit.Test;
import static com.google.common.collect.Iterables.contains;
import static org.fest.assertions.Assertions.assertThat;

public class SynsetTest {
  private static WordNetInterface wordNet;
  @BeforeClass
  public static void init() {
    wordNet = WordNet.getInstance();
  }

  @Test
  public void testSomeGlosses() {
    System.err.println("testSomeGlosses");
    final WordSense sentence = wordNet.lookupWord("sentence", POS.NOUN).getSense(1);
    final String sentenceGloss = "a string of words satisfying the grammatical rules of a language; \"he always spoke in grammatical sentences\"";
    assertThat(sentenceGloss).isEqualTo(sentence.getSynset().getGloss());

    final WordSense lexeme = wordNet.lookupWord("lexeme", POS.NOUN).getSense(1);
    final String lexemeGloss = "a minimal unit (as a word or stem) in the lexicon of a language; `go' and `went' and `gone' and `going' are all members of the English lexeme `go'";
    assertThat(lexemeGloss).isEqualTo(lexeme.getSynset().getGloss());
  }

  @Test
  public void testGlossSearch() {
    System.err.println("testGlossSearch");
    final WordSense sentence = wordNet.lookupWord("sentence", POS.NOUN).getSense(1);
    //System.err.println("hits: "+Joiner.on("\n  ").join(dictionary.searchGlossBySubstring("\\bgrammatical\\b", POS.ALL)));
    assertThat(contains(wordNet.searchGlossBySubstring("\\bgrammatical\\b", POS.ALL), sentence.getSynset())).isTrue();
  }

  @Test
  public void testDescriptions() {
    System.err.println("testDescriptions");
    int count = 0;
    final int expectedCount = 117659;
    for (final Synset synset : wordNet.synsets(POS.ALL)) {
      count++;
      //if(++count > 10) break;
      // exercise toString() and getGloss()
      final String msg = count+" "+synset+"\n  "+synset.getGloss();
      //System.err.println(msg);
      // exercise normal and long description with and without verbose
      //TODO assert something here, don't just exercise
      final String msg2 = count+" "+synset+"\n  "+synset.getDescription();
    }
    assertThat(count).isEqualTo(expectedCount);
    System.err.printf("tested %,d descriptions.\n", count);
  }
}