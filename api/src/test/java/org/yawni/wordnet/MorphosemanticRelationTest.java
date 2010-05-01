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

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.fest.assertions.Assertions.assertThat;

public class MorphosemanticRelationTest {
  private WordNet wordNet;

  @Before
  public void init() {
    wordNet = WordNet.getInstance();
  }

  @Test
  public void testEnum() {
    System.err.println("enum values: "+MorphosemanticRelation.getStringToRelMap());
    assertThat(MorphosemanticRelation.AGENT).isSameAs(MorphosemanticRelation.valueOf("AGENT"));
    assertThat(MorphosemanticRelation.fromValue("AGENT")).isSameAs(MorphosemanticRelation.valueOf("AGENT"));
    
    assertThat(MorphosemanticRelation.fromValue("BY_MEANS_OF")).isSameAs(MorphosemanticRelation.BY_MEANS_OF);
    assertThat(MorphosemanticRelation.fromValue("by-means-of")).isSameAs(MorphosemanticRelation.BY_MEANS_OF);
  }

  @Test
  public void testValues() {
    System.err.println("values");
    for (final Synset synset : wordNet.synsets(POS.VERB)) {
      final int offset = synset.getOffset();
      //String s = synset.toString();
      //System.err.println(s);
      for (final Relation morphDeriv : synset.getRelations(RelationType.DERIVATIONALLY_RELATED)) {
        final LexicalRelation lexRel = (LexicalRelation) morphDeriv;
        if (morphDeriv.getTarget().getPOS() != POS.NOUN) {
          continue;
        }
        final List<Relation> reverse = morphDeriv.getTarget().getRelations(RelationType.DERIVATIONALLY_RELATED);
        if (reverse.isEmpty()) {
          System.err.println("reverse missing: "+lexRel);
        }
        final String offsetKey = String.format("2%08d", offset);
        String lexRelLine = wordNet.lookupMorphoSemanticRelationLine(offsetKey);
        if (lexRelLine != null) {
          // TODO parse the line
          System.err.println("MorphoSemanticRelation line: "+lexRelLine);
        } else {
          //System.err.println("eek! "+this+" target NOUN offset: "+lexRel.getTargetOffset());
          System.err.println("eek! "+lexRel);
        }
      }
    }
  }
}