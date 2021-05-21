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

import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import org.junit.BeforeClass;
import org.junit.Test;
import static com.google.common.collect.Iterables.contains;
import static org.fest.assertions.Assertions.assertThat;
import org.yawni.wordnet.WordNetInterface.WordNetVersion;

public class SynsetTest {
  private static WordNetInterface WN;
  @BeforeClass
  public static void init() {
    WN = WordNet.getInstance();
  }

  @Test
  public void testSomeGlosses() {
    System.err.println("testSomeGlosses");
    final WordSense sentence = WN.lookupWord("sentence", POS.NOUN).getSense(1);
    final String sentenceGloss = "a string of words satisfying the grammatical rules of a language; \"he always spoke in grammatical sentences\"";
    assertThat(sentenceGloss).isEqualTo(sentence.getSynset().getGloss());

    final WordSense lexeme = WN.lookupWord("lexeme", POS.NOUN).getSense(1);
    final String lexemeGloss = "a minimal unit (as a word or stem) in the lexicon of a language; `go' and `went' and `gone' and `going' are all members of the English lexeme `go'";
    assertThat(lexemeGloss).isEqualTo(lexeme.getSynset().getGloss());
  }

  @Test
  public void testGlossSearch() {
    System.err.println("testGlossSearch");
    final WordSense sentence = WN.lookupWord("sentence", POS.NOUN).getSense(1);
    //System.err.println("hits: "+Joiner.on("\n  ").join(dictionary.searchGlossBySubstring("\\bgrammatical\\b", POS.ALL)));
    assertThat(contains(WN.searchGlossBySubstring("\\bgrammatical\\b", POS.ALL), sentence.getSynset())).isTrue();
  }

  @Test
  public void testDescriptions() {
    System.err.println("testDescriptions");
    int count = 0;
    final ImmutableMap<WordNetVersion, Integer> numSynsets = ImmutableMap.of(WordNetVersion.WN30, 117659, WordNetVersion.WN21, 117597);
    final Integer expectedCount = numSynsets.get(WordNetVersion.detect());
    for (final Synset synset : WN.synsets(POS.ALL)) {
      count++;
      //if(++count > 10) break;
      // exercise toString() and getGloss()
      final String msg = count+" "+synset+"\n  "+synset.getGloss();
      //System.err.println(msg);
      // exercise normal and long description with and without verbose
      //TODO assert something here, don't just exercise
      final String msg2 = count+" "+synset+"\n  "+synset.getDescription();
    }
    if (expectedCount != null) {
      assertThat(count).isEqualTo(expectedCount);
    }
    System.err.printf("tested %,d descriptions.\n", count);
  }

  @Test
  public void getSynsetAt() {
    final Optional<Synset> findable = WN.getSynsetAt(POS.NOUN, 7846);
    assertThat(findable.map(Synset::getOffset)).isEqualTo(Optional.of(7846));
    final Optional<Synset> notFindable = WN.getSynsetAt(POS.NOUN, 7841);
    assertThat(notFindable).isEqualTo(Optional.empty());
  }
}