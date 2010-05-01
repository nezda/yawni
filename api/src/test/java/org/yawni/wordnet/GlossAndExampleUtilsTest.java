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

import com.google.common.base.Joiner;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.fest.assertions.Assertions.assertThat;
import static org.yawni.wordnet.GlossAndExampleUtils.*;

public class GlossAndExampleUtilsTest {
  private WordNetInterface dictionary;

  @Before
  public void init() {
    dictionary = WordNet.getInstance();
    // keep the test deterministic
  }

  @Test
  public void synsets() {
    System.err.println("synsets");
    for (final Synset synset : dictionary.synsets(POS.ALL)) {
      //System.err.println(s);
      final String definitions = getDefinitionsChunk(synset);
      assertTrue(definitions.length() > 0);
//      System.err.println("defs: "+definitions);
      final String examples = getExamplesChunk(synset);
//      System.err.println("exs: "+examples);
    }
    System.err.println("success!");
  }

  @Test
  public void glossChunkParsing() {
    System.err.println("glossChunkParsing");
    final WordSense release = dictionary.lookupWord("release", POS.NOUN).getSense(3);
    final String sentenceGloss = "a process that liberates or discharges something; \"there was a sudden release of oxygen\"; \"the release of iodine from the thyroid gland\"";
    assertThat(sentenceGloss).isEqualTo(release.getSynset().getGloss());

    final String definition1 = "a process that liberates or discharges something";
    final String example1 = "there was a sudden release of oxygen";
    final String example2 = "the release of iodine from the thyroid gland";
    // gloss = definition + example *
    // definition = [^;]+
    // example = ; " [^"]+ "
    assertThat(getDefinitionsChunk(release.getSynset())).contains(definition1);
    final String examples = getExamplesChunk(release.getSynset());
    assertThat(examples).contains(example1);
    assertThat(examples).contains(example2);
  }

//  @Test
//  public void synsets() {
//    System.err.println("synsets");
//    for (final Synset synset : dictionary.synsets(POS.ALL)) {
//      final List<String> definitions = getDefinitions(synset);
//      assertFalse(definitions.isEmpty());
//      assertFalse(definitions.get(0).isEmpty());
//      try {
//        final List<String> examples = getExamples(synset);
//      } catch (IllegalArgumentException iae) {
//        System.err.println("UH OH EXAMPLE: "+iae);
//      }
//      //System.err.println(s);
//    }
//  }

  @Test
  public void glossParsing1() {
    System.err.println("glossParsing");
    final WordSense release = dictionary.lookupWord("release", POS.NOUN).getSense(3);
    final String sentenceGloss = "a process that liberates or discharges something; \"there was a sudden release of oxygen\"; \"the release of iodine from the thyroid gland\"";
    assertThat(sentenceGloss).isEqualTo(release.getSynset().getGloss());

    final String definition1 = "a process that liberates or discharges something";
    final String example1 = "there was a sudden release of oxygen";
    final String example2 = "the release of iodine from the thyroid gland";
    // gloss = definition + example *
    // definition = [^;]+
    // example = ; " [^"]+ "
    final Pattern glossParts = Pattern.compile("([^;]+)(?:; \"([^\"]+)\")*");
    assertThat(sentenceGloss).matches(glossParts.pattern());
    assertThat(getDefinitions(release.getSynset())).contains(definition1);
    final List<String> examples = getExamples(release.getSynset());
    System.err.println("examples: "+Joiner.on('\n').join(examples));
    assertThat(examples).contains(example1, example2);
  }

  // invalid example
  // final WordSense refocus = dictionary.lookupWord("refocus", POS.NOUN).getSense(1);

//  @Test
//  public void glossParsing2() {
//    System.err.println("glossParsing");
//    final WordSense release = dictionary.lookupWord("release", POS.NOUN).getSense(3);
//    final String sentenceGloss = "a process that liberates or discharges something; \"there was a sudden release of oxygen\"; \"the release of iodine from the thyroid gland\"";
//    assertThat(sentenceGloss).isEqualTo(release.getSynset().getGloss());
//
//    final String definition1 = "a process that liberates or discharges something";
//    final String example1 = "there was a sudden release of oxygen";
//    final String example2 = "the release of iodine from the thyroid gland";
//    // gloss = [^;]+ (";" "\"" [^\"]+ "\"")*
//    final Pattern glossParts = Pattern.compile("([^;]+)(?:; \"([^\"]+)\")*");
//    assertThat(sentenceGloss).matches(glossParts.pattern());
//    assertThat(release.getSynset().getDefinitions()).contains(definition1);
//    System.err.println("examples: "+Joiner.on('\n').join(release.getSynset().getExamples()));
//    //a tangible and visible entity; an entity that can cast a shadow; "it was full of rackets, balls and other objects"
//  }
}