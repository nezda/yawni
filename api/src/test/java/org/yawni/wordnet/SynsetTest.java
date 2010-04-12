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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.Test;
import org.yawni.util.Joiner;
import static org.yawni.util.Utils.contains;
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
      final String msg3 = count+" "+synset+"\n  "+synset.getDescription(true);
      final String msg4 = count+" "+synset+"\n  "+synset.getLongDescription();
      final String msg5 = count+" "+synset+"\n  "+synset.getLongDescription(true);
    }
    assertThat(count).isEqualTo(expectedCount);
    System.err.printf("tested %,d descriptions.\n", count);
  }

  @Test
  public void testAntonym() {
    System.err.println("testAntonym");
    // adj up#1 has ANTONYM adj down#1
    final Word upWord = wordNet.lookupWord("up", POS.ADJ);
    assertThat(upWord.getRelationTypes()).contains(RelationType.ANTONYM);
    final WordSense up1 = upWord.getSense(1);
    final Word downWord = wordNet.lookupWord("down", POS.ADJ);
    final WordSense down1 = downWord.getSense(1);
    assertThat(up1.getRelationTargets(RelationType.ANTONYM)).contains(down1);
    assertThat(down1.getRelationTargets(RelationType.ANTONYM)).contains(up1);

    // adj beutifulu#1 has ANTONYM adj ugly#1
    // https://sourceforge.net/tracker/index.php?func=detail&aid=1226746&group_id=33824&atid=409470
    final Word beautifulWord = wordNet.lookupWord("beautiful", POS.ADJ);
    assertThat(beautifulWord.getRelationTypes()).contains(RelationType.ANTONYM);
    final WordSense beautiful1 = beautifulWord.getSense(1);
    final Word uglyWord = wordNet.lookupWord("ugly", POS.ADJ);
    final WordSense ugly1 = uglyWord.getSense(1);
    assertThat(beautiful1.getRelationTargets(RelationType.ANTONYM)).contains(ugly1);
    assertThat(ugly1.getRelationTargets(RelationType.ANTONYM)).contains(beautiful1);
  }

  // test SEE_ALSO
  // ADJ"happy"#1 → {"cheerful", "contented", "content", "glad", "elated", "euphoric", "felicitous", "joyful", "joyous"}

  @Test
  public void testPertainym() {
    System.err.println("testPertainym");
    // adj presidential#1 has PERTAINYM noun president#3
    final Word presidentialWord = wordNet.lookupWord("presidential", POS.ADJ);
    assertThat(presidentialWord.getRelationTypes()).contains(RelationType.PERTAINYM);
    final WordSense presidential1 = presidentialWord.getSense(1);
    final Word presidentWord = wordNet.lookupWord("president", POS.NOUN);
    final WordSense president3 = presidentWord.getSense(3);
    assertThat(presidential1.getRelationTargets(RelationType.PERTAINYM)).contains(president3);
    // https://sourceforge.net/tracker/index.php?func=detail&aid=1372493&group_id=33824&atid=409470
    assertThat(presidential1.getRelationTargets(RelationType.DERIVED)).isEmpty();
  }

  @Test
  public void testDomainTypes() {
    System.err.println("testDomainTypes");
    // adj up#7 member of noun TOPIC computer#1
    final Word word = wordNet.lookupWord("up", POS.ADJ);
    System.err.println("word: "+word+" relationTypes: "+word.getRelationTypes());
    System.err.println("  "+word.getSense(7).getLongDescription());
    final RelationType[] relationTypes = new RelationType[] {
      //RelationType.DOMAIN,
      //RelationType.MEMBER_OF_TOPIC_DOMAIN,
      RelationType.DOMAIN_OF_TOPIC,
      //RelationType.MEMBER_OF_THIS_DOMAIN_TOPIC,
    };
    //FIXME assert something here, don't just print
    for (final RelationType relationType : relationTypes) {
      for (final RelationTarget target : word.getSense(7).getSynset().getRelationTargets(relationType)) {
        System.err.println(relationType + " target: " + target);
      }
    }
  }

  @Test
  public void testAttributeType() {
    System.err.println("testAttributeType");
    // adj low-pitch#1 is attribute of "pitch"#1
    final Word word = wordNet.lookupWord("low-pitched", POS.ADJ);
    System.err.println("word: "+word+" relationTypes: "+word.getRelationTypes());
    System.err.println("  "+word.getSense(1).getLongDescription());
    final RelationType[] relationTypes = new RelationType[] {
      RelationType.ATTRIBUTE,
    };
    //FIXME assert something here, don't just print
    for (final RelationType relationType : relationTypes) {
      for (final RelationTarget target : word.getSense(1).getSynset().getRelationTargets(relationType)) {
        System.err.println("  "+relationType+" target: "+target);
      }
    }
  }

  @Test
  public void testVerbFrames() {
    System.err.println("testVerbFrames");
    // verb "complete"#1 _synset_ has 4 generic verb frames
    // 1. Somebody ----s
    // 2. Somebody ----s something
    // 3. Something ----s something
    // 4. Somebody ----s VERB-ing
    // and 1 specific verb frame
    // 1. They won't %s the story
    // https://sourceforge.net/tracker/index.php?func=detail&aid=1749797&group_id=33824&atid=409471
    final Word complete = wordNet.lookupWord("complete", POS.VERB);
    final WordSense complete1 = complete.getSense(1);
    //TODO compare with wnb and what its actually supposed to do
    assertThat(complete1.getVerbFrames()).hasSize(5);
    final Word finish = wordNet.lookupWord("finish", POS.VERB);
    final WordSense finish1 = finish.getSense(1);
  }

  @Test
  public void testInstances() {
    System.err.println("testInstances");
    // noun "George Bush"#1 has 
    final Word georgeBush = wordNet.lookupWord("George Bush", POS.NOUN);
    System.err.println("word: "+georgeBush+" relationTypes: "+georgeBush.getRelationTypes());
    System.err.println("  "+georgeBush.getSense(1).getLongDescription());
    final RelationType[] relationTypes = new RelationType[] {
      //FIXME make HYPONYM a superset of INSTANCE_HYPONYM ?
      RelationType.HYPONYM,
      RelationType.INSTANCE_HYPONYM,
      RelationType.HYPERNYM,
      RelationType.INSTANCE_HYPERNYM,
    };
    for (final RelationType relationType : relationTypes) {
      final List<RelationTarget> targets = georgeBush.getSense(1).getSynset().getRelationTargets(relationType);
      //assertTrue("type: "+relationType, targets.isEmpty() == false);
      // woah - WordSense targets are different than Synset targets ??
      // at a minimum this needs to be documented
      final List<RelationTarget> targetsAlt = georgeBush.getSense(1).getRelationTargets(relationType);
//      assertEquals("relationType: "+relationType, targets, targetsAlt);
      //assertTrue(targets == targetsAlt);
      for (final RelationTarget target : targets) {
        System.err.println("  " + relationType + " target: " + target);
      }
    }
  }

  @Test
  public void testVerbGroup() {
    System.err.println("testVerbGroup");
    // verb turn#1 groups with turn#4 and turn#19
    final Word word = wordNet.lookupWord("turn", POS.VERB);

    for (final WordSense sense : word) {
      final List<RelationTarget> g = new ArrayList<RelationTarget>();
      gather(sense.getSynset(), RelationType.VERB_GROUP, g);
      if (g.isEmpty()) {
        continue;
      }
      try {
        Collections.sort(g, new FocalWordSynsetComparator(word));
      } catch (IllegalArgumentException iae) {
        System.err.println("uh oh: "+iae);
      }
      System.err.println(sense+" VERB_GROUP:\n  "+Joiner.on("\n  ").join(g));
    }

    System.err.println(word);
    final WordSense s1 = word.getSense(1);
    System.err.println("  "+s1);
    RelationTarget syn1 = s1.getSynset();
    System.err.println("  "+syn1);
    // VERB_GROUP targets form a chain/tree: syn1 → {syn2}, syn2 → {syn3, syn4}, ...
    // To reveal the whole extent of the relation, the transitive closure implied by the explicit relations
    // must be expanded.  The result is fully connected groups (i.e., cliques) of related synsets.  
    // This is not easily represented in our API without additional containers (e.g., List<List<Synset>>).
    
    // - gather these recursively
    final List<RelationTarget> g1 = new ArrayList<RelationTarget>();
    gather(syn1, RelationType.VERB_GROUP, g1);
//    System.err.println("g1: "+g1);
    System.err.println("g1:\n  "+Joiner.on("\n  ").join(g1));
    // most efficient way of enumerating verb groups:
    // - start with full set of Synset for given Word
    // - take 1st Synset,
    //   - follow VERB_GROUP pointers (? assert all targets in full set ?)
    //     - create Map<Synset, Set<Synset>> where value sets are shared

    // Oops! Only want results which stay within the Synsets *this* Word is in,
    // here we got derailed on VERB_GROUP path from work#26 → work#25
//  [WordSense 458471@[POS verb]:"turn"#25] VERB_GROUP:
//    [Synset 458754@[POS verb]<verb.change>{ferment#3, work#25}]
//    [Synset 458471@[POS verb]<verb.change>{sour#1, turn#25, ferment#4, work#26}]
  }

  private static class FocalWordSynsetComparator implements Comparator<RelationTarget> {
    private final Word focalWord;
    FocalWordSynsetComparator(final Word focalWord) {
      this.focalWord = focalWord;
    }
    public int compare(RelationTarget s1, RelationTarget s2) {
      final WordSense ws1 = focalSense(s1.getSynset());
      final WordSense ws2 = focalSense(s2.getSynset());
      return Integer.signum(ws1.getSenseNumber() - ws2.getSenseNumber());
    }
    private WordSense focalSense(final Synset s) {
      final WordSense focalWordSense = s.getWordSense(focalWord);
      if (focalWordSense == null) {
        throw new IllegalArgumentException("Given "+s+" has no sense of focal Word "+focalWord);
      }
      return focalWordSense;
    }
  } // end class FocalWordSynsetComparator

  private static void gather(final RelationTarget source, final RelationType type, final List<RelationTarget> accum) {
//    for (final RelationTarget target : source.getRelationTargets(type)) {
    for (final Relation rel : source.getRelations(type)) {
      final RelationTarget target = rel.getTarget();
//      System.err.println("rel: "+rel);
      if (accum.contains(target)) {
        continue;
      }
      accum.add(target);
      gather(target, type, accum);
    }
  }

  @Test
  public void testLexicalRelations() {
    System.err.println("testLexicalRelations");
    final Set<RelationType> expectedLexicalRelations = EnumSet.noneOf(RelationType.class);
    for (final RelationType relType : RelationType.values()) {
      if (relType.isLexical()) {
        expectedLexicalRelations.add(relType);
      }
    }
    final Set<RelationType> foundLexicalRelations = EnumSet.noneOf(RelationType.class);
    for (final Synset synset : wordNet.synsets(POS.ALL)) {
      for (final Relation relation : synset.getRelations()) {
        if (relation.isLexical()) {
          foundLexicalRelations.add(relation.getType());
          if (relation.getType().isSemantic()) {
            //System.err.println("CONFUSED "+relation);
          }
        }
        // check if isLexical, relation source and target are WordSenses,
        // else neither the source nor the target are WordSenses
        if (relation.isLexical()) {
          assertThat(relation.getSource()).isInstanceOf(WordSense.class);
          assertThat(relation.getTarget()).isInstanceOf(WordSense.class);
        } else {
          assertThat(relation.getSource()).isInstanceOf(Synset.class);
          assertThat(relation.getTarget()).isInstanceOf(Synset.class);
        }
      }
    }
    //System.err.printf("foundLexicalRelations.size(): %d expectedLexicalRelations.size(): %d\n",
    //  foundLexicalRelations.size(), expectedLexicalRelations.size());
    //System.err.printf("foundLexicalRelations: %s \nexpectedLexicalRelations: %s\n",
    //  foundLexicalRelations, expectedLexicalRelations);
    // compute the difference in these 2 sets:
    Set<RelationType> missing = EnumSet.copyOf(foundLexicalRelations);
    missing.removeAll(expectedLexicalRelations);
    //assertTrue(String.format("missing: %s\n", missing), missing.isEmpty());
    System.err.println("oddball semi-lexical RelationTypes: "+missing);
  }

  @Test
  public void testSemanticRelations() {
    System.err.println("testSemanticRelations");
    for (final Synset synset : wordNet.synsets(POS.ALL)) {
      for (final SemanticRelation relation : synset.getSemanticRelations(null)) {
        assertThat(relation.isSemantic()).isTrue();
        assertThat(relation.getType().isSemantic()).isTrue(); // msg: "! isSemantic(): "+relation
      }
    }
  }
}