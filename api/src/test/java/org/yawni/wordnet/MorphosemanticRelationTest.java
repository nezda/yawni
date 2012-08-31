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

import com.google.common.collect.Iterables;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.yawni.util.CharSequences;
import static org.junit.Assert.*;
import static org.fest.assertions.Assertions.assertThat;
import org.yawni.wordnet.WordNetInterface.WordNetVersion;

public class MorphosemanticRelationTest {
  private WordNet wordNet;

  @Before
  public void init() {
    wordNet = WordNet.getInstance();
  }

//  @Test
//  public void findTypeNameCollisions() {
//    System.err.println("findTypeNameCollisions");
//    for (final MorphosemanticRelation rel : MorphosemanticRelation.values()) {
//      try {
//        final RelationType collision = RelationType.valueOf(rel.name());
//        fail("MorphosemanticRelation collision: "+collision);
//      } catch (IllegalArgumentException iae) {
//        // expected
//      }
//    }
//    // TeleologicalRelation.CAUSE collides with RelationType.CAUSE
//    for (final TeleologicalRelation rel : TeleologicalRelation.values()) {
//      try {
//        final RelationType collision = RelationType.valueOf(rel.name());
//        fail("TeleologicalRelation collision: "+collision);
//      } catch (IllegalArgumentException iae) {
//        // expected
//      }
//    }
//    System.err.println("done");
//  }

  @Test
  public void testEnum() {
//    System.err.println("enum values: "+MorphosemanticRelation.getStringToRelMap());
    System.err.println("enum values: "+MorphosemanticRelation.aliases());
    assertThat(MorphosemanticRelation.AGENT).isSameAs(MorphosemanticRelation.valueOf("AGENT"));
    assertThat(MorphosemanticRelation.fromValue("AGENT")).isSameAs(MorphosemanticRelation.valueOf("AGENT"));

    assertThat(MorphosemanticRelation.fromValue("BY_MEANS_OF")).isSameAs(MorphosemanticRelation.BY_MEANS_OF);
    assertThat(MorphosemanticRelation.fromValue("by-means-of")).isSameAs(MorphosemanticRelation.BY_MEANS_OF);
  }

//  @Ignore
  @Test
  public void testRawSearch() throws Exception {
		if (WordNetVersion.detect() != WordNetVersion.WN30) {
			return;
		}

    System.err.println("rawSearch");
    final String path = "/dict/" + WordNet.getMorphosemanticRelationsFilename();
    final BufferedReader lines = new BufferedReader(new InputStreamReader(getClass().getResource(path).openStream()));
    String line;
    while ((line = lines.readLine()) != null) {
      final String[] parts = line.split(" ");
      assert parts.length == 5;
      assert parts[0].length() == 9;
      Integer.parseInt(parts[0]); // crash test
      final CharSequence srcOffset = parts[0].substring(1);
      final POS srcPOS = POS.fromOrdinal((byte)Integer.parseInt(parts[0].substring(0, 1)));
      final CharSequence srcOffsetKey = parts[0];
      assert parts[3].length() == 9;
      Integer.parseInt(parts[3]); // crash test
      final CharSequence targetOffset = parts[3].substring(1);
      final POS targetPOS = POS.fromOrdinal((byte)Integer.parseInt(parts[3].substring(0, 1)));
      final CharSequence targetOffsetKey = parts[3];
//      String lexRelLine = wordNet.lookupMorphoSemanticRelationLine(offsetKey);
//      assert lexRelLine != null : "line: "+line;
//      // lines are not unique based on offset alone!
//      assert line.equals(lexRelLine) : "\nline:       "+line+"\nlexRelLine: "+lexRelLine;
      final ImmutableList<CharSequence> matches = ImmutableList.copyOf(wordNet.lookupMorphoSemanticRelationLines(srcOffsetKey));
      boolean found = false;
      for (final CharSequence lexRelLine : matches) {
        found |= CharSequences.equals(line, lexRelLine);
      }
      assert found : "could not find line: "+line+" found: \n"+Joiner.on("\n").join(matches);
      // TODO
      // - get the src synset
      // - lookup its relations
      final Iterable<Synset> src = wordNet.synsets("?POS="+srcPOS.name()+"&offset="+srcOffset);
      assertEquals(line, 1, Iterables.size(src));
      final Iterable<Synset> target = wordNet.synsets("?POS="+targetPOS.name()+"&offset="+targetOffset);
      assertEquals(line, 1, Iterables.size(target));

      final Synset srcSyn = Iterables.getOnlyElement(src);
      final WordSense srcSense = srcSyn.getWordSense(Integer.parseInt(parts[1]));
      final Synset targetSyn = Iterables.getOnlyElement(target);
      final WordSense targetSense = targetSyn.getWordSense(Integer.parseInt(parts[4]));
      //FIXME type is lame 2 step process
      final MorphosemanticRelation morphorel = MorphosemanticRelation.fromValue(parts[2]);
      final RelationType type = RelationType.valueOf(morphorel.name());
      assertNotNull(type);
      final List<RelationArgument> targets = srcSyn.getRelationTargets(type);
      if (targets.isEmpty()) {
        // 497 empty
        final List<RelationArgument> morphDerivs = srcSyn.getRelationTargets(RelationType.DERIVATIONALLY_RELATED);
        //System.err.println("empty "+type+" "+srcSense+" :: "+targetSense+"  "+morphDerivs.size());
        //System.err.println("  "+Joiner.on("\n  ").join(srcSyn.getRelations()));
        continue;
      }
      // this will never happen because these are LexicalRelations
      assert ! targets.contains(targetSyn);
//      if (! targets.contains(targetSyn)) {
//        System.err.println("yow "+srcSyn);
//      }
    }
    System.err.println("all good");
  }

  // translates 'morphosemantic-links.xls.tsv.offsets.bidi'
  @Ignore
  @Test
  public void translateSenseKeys() throws Exception {
    System.err.println("translateSenseKeys");
    final String path = "/dict/" + WordNet.getMorphosemanticRelationsFilename();
    final BufferedReader lines = new BufferedReader(new InputStreamReader(getClass().getResource(path).openStream()));
    String line;
    while ((line = lines.readLine()) != null) {
      // 100021265 nutrient%1:03:00:: uses 201204191 nutrify%2:34:00::
      final String[] parts = line.split(" ");
      assertThat(parts).hasSize(5);
      assert parts.length == 5;
      // have to shear off leading digit which is POS indicator
      assertThat(parts[0]).hasSize(9);
      assert parts[0].length() == 9;
      final POS srcPOS = POS.fromOrdinal(Byte.parseByte(parts[0].substring(0, 1)));
      final int  srcOffset = Integer.parseInt(parts[0].substring(1));
      // nutrient%1:03:00::
      final String srcSenseKey = parts[1];
      final Synset srcSynset = WordNet.getInstance().getSynsetAt(srcPOS, srcOffset);
      assertThat(srcSynset).isNotNull();

      assertThat(parts[3]).hasSize(9);
      assert parts[3].length() == 9;
      final POS targetPOS = POS.fromOrdinal(Byte.parseByte(parts[3].substring(0, 1)));
      final int targetOffset = Integer.parseInt(parts[3].substring(1));
      final String targetSenseKey = parts[4];
      final Synset targetSynset = WordNet.getInstance().getSynsetAt(targetPOS, targetOffset);
      assertThat(targetSynset).isNotNull();

      // TODO
      // + get WordSense for given synset offset and senseKey (really, could get this from senseKey alone)
      // + get Synset idx for given WordSense
      final WordSense srcWordSense = srcSynset.getWordSense(srcSenseKey);
      assert srcWordSense != null : "srcSenseKey: "+srcSenseKey+ " existing senseKeys: \n"+
        Joiner.on("\n").join(senseKeysOf(srcSynset));
      assertThat(srcWordSense).isNotNull();
      final int srcSynsetIdx = srcSynset.getSynsetIndex(srcWordSense);
      assertThat(srcSynsetIdx).isGreaterThanOrEqualTo(0);

      final WordSense targetWordSense = targetSynset.getWordSense(targetSenseKey);
      assert targetWordSense != null : "targetSenseKey: "+targetSenseKey+ " existing senseKeys: \n"+
        Joiner.on("\n").join(senseKeysOf(targetSynset));
      assertThat(targetWordSense).isNotNull();
      final int targetSynsetIdx = targetSynset.getSynsetIndex(targetWordSense);
      assertThat(targetSynsetIdx).isGreaterThanOrEqualTo(0);

      // print new line to stdout:
      // - synsetIndex AND senseKey
      //System.out.println(parts[0]+" "+srcSynsetIdx+" "+parts[1]+" "+parts[2]+" "+parts[3]+" "+targetSynsetIdx+" "+parts[4]);
      // - minimal
      //System.out.println(parts[0]+" "+srcSynsetIdx+" "+parts[2]+" "+parts[3]+" "+targetSynsetIdx);
    }
    System.err.println("done");
  }

  private static Iterable<CharSequence> senseKeysOf(final Synset synset) {
    final List<CharSequence> senseKeys = Lists.newArrayList();
    for (final WordSense wordSense : synset) {
      senseKeys.add(wordSense.getSenseKey());
    }
    return senseKeys;
  }

  @Test
  public void testValues() {
    System.err.println("values");
    for (final Synset synset : wordNet.synsets(POS.VERB)) {

    }
    System.err.println("done");
  }

//  @Ignore
//  @Test
//  public void testValues() {
//    System.err.println("values");
//    int numMorphosemanticRelationsFound = 0;
//    int numCandidates = 0;
//    int numEntryFound = 0;
//    int numNoEntryFound = 0;
//    for (final Synset synset : wordNet.synsets(POS.VERB)) {
//      final int offset = synset.getOffset();
//      //String s = synset.toString();
//      //System.err.println(s);
//      for (final Relation morphDeriv : synset.getRelations(RelationType.DERIVATIONALLY_RELATED)) {
//        final LexicalRelation lexRel = (LexicalRelation) morphDeriv;
//        if (morphDeriv.getTarget().getPOS() != POS.NOUN) {
//          continue;
//        }
//        if (morphDeriv.getSource().getPOS() != POS.VERB) {
//          continue;
//        }
//        numCandidates++;
//        final List<Relation> reverse = morphDeriv.getTarget().getRelations(RelationType.DERIVATIONALLY_RELATED);
//        if (reverse.isEmpty()) {
//          // only 2 cases:
//          // [from [WordSense 1793177@[POS verb]:"offend"#4] to [WordSense 1224031@[POS noun]:"offence"#4]]
//          // [from [WordSense 2566528@[POS verb]:"offend"#2] to [WordSense 766234@[POS noun]:"offence"#5]]
//          System.err.println("reverse missing: "+lexRel);
//        }
//        final String offsetKey = String.format("2%08d", offset);
//        String lexRelLine = wordNet.lookupMorphoSemanticRelationLine(offsetKey);
//        if (lexRelLine != null) {
//          numEntryFound++;
//          // TODO parse the line
//          // morphosemantic-links.xls.tsv.offsets.bidi
//          // 200692329 abstract%2:31:00:: event 105780104 abstraction%1:09:02::
//          final String[] parts = lexRelLine.split(" ");
//          assert parts.length == 5;
//          // have to shear off leading digit which is POS indicator
//          assert parts[0].length() == 9;
//          assert parts[3].length() == 9;
//          final int sourceOffset = Integer.parseInt(parts[0].substring(1));
//          assert synset.equals(lexRel.getSource().getSynset());
//          assert sourceOffset == lexRel.getSource().getSynset().getOffset();
////          if (lexRel.getSource().getSynset().getOffset() != sourceOffset) {
////            System.err.format("source mismatch! instance offset: %d parsed offset: %d %s\n",
////              lexRel.getSource().getSynset().getOffset(),
////              sourceOffset,
////              lexRel);
////          }
//          final int targetOffset = Integer.parseInt(parts[3].substring(1));
//          final String targetSensekey = parts[4];
//          if (lexRel.getTargetOffset() != targetOffset) {
//            System.err.format("target mismatch! instance offset: %d parsed offset: %d senseKey %s\n  %s\n  %s\n",
//              lexRel.getTargetOffset(),
//              targetOffset,
//              targetSensekey,
//              lexRelLine,
//              lexRel);
//          } else {
//            System.err.println("hit! "+lexRelLine);
//            numMorphosemanticRelationsFound++;
//            // if the target offset is right,
//            // - is the source sense key correct ?
//            // - is the target sense key correct ?
//          }
//          //System.err.println("MorphoSemanticRelation line: "+lexRelLine);
//        } else {
//          numNoEntryFound++;
//          // 1331 cases ; manual error ? need to examine
//          //System.err.println("eek! "+this+" target NOUN offset: "+lexRel.getTargetOffset());
////          System.err.println("eek! "+lexRel);
//        }
//      }
//    }
//    // numCandidates: 21,556
//    // numMorphosemanticRelationsFound: 9,548 (expecting closer to 16,995)
//    // numEntryFound: 20,225
//    // numNoEntryFound: 1,331
//    System.err.format("numCandidates: %,d numMorphosemanticRelationsFound: %,d numEntryFound: %,d numNoEntryFound: %,d\n",
//      numCandidates, numMorphosemanticRelationsFound, numEntryFound, numNoEntryFound);
//  }
}