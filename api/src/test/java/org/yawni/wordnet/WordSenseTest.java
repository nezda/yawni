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

import com.google.common.base.Joiner;
import java.util.EnumSet;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.fest.assertions.Assertions.assertThat;
import org.yawni.wordnet.WordNetInterface.WordNetVersion;

public class WordSenseTest {
  private static WordNetInterface WN;
  private static WordNetVersion VERSION;

  @BeforeClass
  public static void init() {
    WN = WordNet.getInstance();
    VERSION = WordNetVersion.detect();
    System.err.println("WordNetVersion: "+VERSION);
  }

  @Test
  public void testSpecificLexicalRelations() {
    System.err.println("testSpecificLexicalRelations");
    final EnumSet<WordNetVersion> testForVersions = EnumSet.of(WordNetVersion.WN21, WordNetVersion.WN30);
    if (!testForVersions.contains(VERSION)) {
      return;
    }
    final WordSense abstraction = WN.lookupWord("abstraction", POS.NOUN).getSense(6);
    final LexicalRelation abstractionUndergoer = abstraction.getLexicalRelations(RelationType.UNDERGOER).get(0);
    final SemanticRelation abstractionHyponym = abstraction.getSynset().getSemanticRelations(RelationType.HYPONYM).get(0);
    assertThat(abstractionUndergoer.compareTo(abstractionHyponym)).isLessThan(0);

    final WordSense viral = WN.lookupWord("viral", POS.ADJ).getSense(1);
    final WordSense virus = WN.lookupWord("virus", POS.NOUN).getSense(1);
    assertThat(viral.getRelationTargets(RelationType.DERIVATIONALLY_RELATED)).contains(virus);
    assertThat(virus.getRelationTargets(RelationType.DERIVATIONALLY_RELATED)).contains(viral);

    final WordSense hypocrite = WN.lookupWord("hypocrite", POS.NOUN).getSense(1);
    final WordSense hypocritical = WN.lookupWord("hypocritical", POS.ADJ).getSense(1);

    // relation missing from WordNet 3.0! TODO in 3.1?
    // assertThat(hypocrite.getRelationTargets(RelationType.DERIVATIONALLY_RELATED)).contains(hypocritical);
    // relation missing from WordNet 3.0! TODO in 3.1?
    // assertThat(hypocritical.getRelationTargets(RelationType.DERIVATIONALLY_RELATED)).contains(hypocrite);

    final WordSense hypocrisy = WN.lookupWord("hypocrisy", POS.NOUN).getSense(1);
    assertThat(hypocritical.getRelationTargets(RelationType.DERIVATIONALLY_RELATED)).contains(hypocrisy);
    assertThat(hypocrisy.getRelationTargets(RelationType.DERIVATIONALLY_RELATED)).contains(hypocritical);

    final WordSense palatine = WN.lookupWord("palatine", POS.NOUN).getSense(1);
    // roman2 is a hypernym of palatine
    final WordSense roman2 = WN.lookupWord("roman", POS.NOUN).getSense(2);
    // this will fail because the WordSense palatine has NO HYPERNYMs ? this is VERY confusing
//    assertThat(palatine.getRelationTargets(RelationType.HYPERNYM)).contains(roman2);
    assertThat(palatine.getRelationTargets(RelationType.HYPERNYM)).contains(roman2.getSynset());
    // this will fail because the WordSense palatine's Synset's HYPERNYM targets are Synsets, NOT WordSenses
//    assertThat(palatine.getSynset().getRelationTargets(RelationType.HYPERNYM)).contains(roman2);
    assertThat(palatine.getSynset().getRelationTargets(RelationType.HYPERNYM)).contains(roman2.getSynset());

    // inter-connections among invent derivs depends on where you start
    final WordSense invent = WN.lookupWord("invent", POS.VERB).getSense(1);
    final WordSense inventor = WN.lookupWord("inventor", POS.NOUN).getSense(1);
    assertThat(invent.getRelationTargets(RelationType.DERIVATIONALLY_RELATED)).contains(inventor);
    assertThat(inventor.getRelationTargets(RelationType.DERIVATIONALLY_RELATED)).contains(invent);

    final WordSense invention = WN.lookupWord("invention", POS.NOUN).getSense(1);
    System.err.println("invent derivs:\n" + Joiner.on("\n").join(invent.getRelationTargets(RelationType.DERIVATIONALLY_RELATED)));
    System.err.println("invention derivs:\n" + Joiner.on("\n").join(invention.getRelationTargets(RelationType.DERIVATIONALLY_RELATED)));
    System.err.println("inventor derivs:\n" + Joiner.on("\n").join(inventor.getRelationTargets(RelationType.DERIVATIONALLY_RELATED)));
  }

  @Test
  public void testLexicalRelations() {
    System.err.println("testRelations");
    for (final WordSense sense : WN.wordSenses(POS.ALL)) {
      for (final Relation relation : sense.getRelations()) {
        assertThat(relation.isLexical() ^ ! relation.hasSource(sense)).isTrue();
        //assertTrue("! type.isLexical(): "+relation, relation.getType().isLexical());
        if (! relation.getType().isLexical()) {
          //System.err.println("CONFUSED "+relation);
        }
      }
    }
  }

  /**
   * verifies optimized {@link WordSense#getSenseKey()} is equal to simple, slow form
   */
  @Test
  public void testSenseKey() {
    System.err.println("testSenseKey");
    for (final WordSense sense : WN.wordSenses(POS.ALL)) {
      // NOTE: String != StringBuilder ! (use .toString() or contentEquals())
      final String currSenseKey = sense.getSenseKey().toString();
      final String altSenseKey = getSenseKey(sense).toString();
      assertThat(currSenseKey).isEqualTo(altSenseKey);
    }
  }

  private String getSenseKey(final WordSense sense) {
    if (sense.getSynset().isAdjectiveCluster()) {
      return oldAdjClusterSenseKey(sense);
    } else {
      return oldNonAdjClusterSenseKey(sense);
    }
  }

  private String oldAdjClusterSenseKey(final WordSense sense) {
    final List<RelationArgument> adjsses = sense.getSynset().getRelationTargets(RelationType.SIMILAR_TO);
    assert adjsses.size() == 1 || VERSION == WordNetVersion.WN20;
    // with WN20 sense: [WordSense 2093443@[POS adjective]:"acerate"#1] adjsses: [[Synset 2092764@[POS adjective]<adj.all>{simple, unsubdivided}], [Synset 1749884@[POS adjective]<adj.all>{pointed}]]
    final Synset adjss = (Synset) adjsses.get(0);
    // if satellite, key lemma in cntlist.rev
    // is adjss's first word (no case) and
    // adjss's lexid (aka lexfilenum) otherwise
    final String searchWord = adjss.getWordSenses().get(0).getLemma();
    final int headSense = adjss.getWordSenses().get(0).getLexid();
    return String.format("%s%%%d:%02d:%02d:%s:%02d",
        sense.getLemma().toLowerCase(),
        POS.SAT_ADJ.getWordNetCode(),
        sense.getSynset().lexfilenum(),
        sense.getLexid(),
        searchWord.toLowerCase(),
        headSense);
  }

  private String oldNonAdjClusterSenseKey(final WordSense sense) {
    return String.format("%s%%%d:%02d:%02d::",
        sense.getLemma().toLowerCase(),
        sense.getPOS().getWordNetCode(),
        sense.getSynset().lexfilenum(),
        sense.getLexid()
        );
  }

  // torture test for "soft" caches - SoftReferences don't seem to have time to clear under such aggressive load
  @Test
  public void testLargeNumberOfSearches() {
    for (int i = 0; i <= 1000000; i++) {
      final String query = String.valueOf(i);
      for (WordSense wordSense : WN.lookupWordSenses(query, POS.ALL)) {
//        System.err.println(wordSense);
        assertThat(wordSense.toString()).isNotNull();
      }
    }
  }
}