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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.yawni.util.LightImmutableList;

/**
 * Utility methods for heuristically parsing WordNet glosses to separate definitions and examples.
 * @yawni.experimental
 */
public class GlossAndExampleUtils {

  // for WordNet 3.0, this is never empty
  public static String getDefinitionsChunk(final Synset synset) {
    final String sentenceGloss = synset.getGloss();
    final int didx = getDefinitionsExamplesDivision(synset);
    return sentenceGloss.substring(0, didx);
  }

  // for WordNet 3.0, this is empty for 84,749 of 117,659 Synsets
  public static String getExamplesChunk(final Synset synset) {
    final String sentenceGloss = synset.getGloss();
    final int didx = getDefinitionsExamplesDivision(synset);
    if (didx != sentenceGloss.length()) {
      // first 2 chars are [:;][ ]?
      final char c0 = sentenceGloss.charAt(didx);
      assert c0 == ';' || c0 == ':';
      final char c1 = sentenceGloss.charAt(didx + 1);
      //assert c1 == ' ' : synset + " gloss: "+sentenceGloss;
      //{detonate, explode, blow up}] gloss: burst and release energy as through a violent chemical or physical reaction;"the bomb detonated at noon"; "The Molotov cocktail exploded"
      int delimPrefix = 1;
      if (c1 == ' ') {
        delimPrefix++;
      }
      //final char firstContentChar = sentenceGloss.charAt(didx + delimPrefix + 1);
      //assert firstContentChar != ' ' : synset + " gloss: "+sentenceGloss;
      //{drop}] gloss: omit (a letter or syllable) in speaking or writing; " New Englanders drop their post-vocalic r's"
      return sentenceGloss.substring(didx + delimPrefix);
    } else {
      return "";
    }
  }
  
  public static int getDefinitionsExamplesDivision(final Synset synset) {
    final String sentenceGloss = synset.getGloss();
    final int endIdx = sentenceGloss.length();
    int didx = -1;
    int qidx = -1;
    // find first dquote after first semicolon or colon
    for (int i = 0; i < endIdx; i++) {
      final char c = sentenceGloss.charAt(i);
      if (c == '"') {
        //assert didx != -1 : synset;
        //{stride} -- (significant progress (especially in the phrase "make strides"); "they made big strides in productivity")
        if (didx != -1) {
          qidx = i;
          break;
        }
      }
      if (c == ';' || c == ':') {
        assert qidx == -1 : synset;
        didx = i;
      }
    }
    //assert didx != -1 : synset;
    if (didx == -1 || qidx == -1) {
      // never found (semi)colon and/or double quote, therefore entire gloss is definition
      return endIdx;
    } else {
      return didx;
    }
  }

  private static final String GLOSS_DEFINITION_1_CAPTURING_PATTERN = "^([^;]+)";
  private static final String GLOSS_DEFINITION_REST_CAPTURING_PATTERN = "(?:; ?([^;]+))?";
  private static final String GLOSS_DEFINITIONS_NON_CAPTURING_PATTERN = "^[^;]+(?:; ?[^;\"]+)*";

  private static final String AUTHOR = "(?:[ ]*[-]*[^;\"]+)?";
  private static final String GLOSS_EXAMPLE_CAPTURING_PATTERN = "(?: ?[;:,]? ?(?:\"|(?<=\"; ))([^\"]+)(?:\"|$)"+AUTHOR+")?";
  private static final String GLOSS_EXAMPLES_NON_CAPTURING_PATTERN = "(?:; (?:\"|(?<=\"; ))[^\"]+(?:\"|$)"+AUTHOR+")*";
  private static final String CAPTURING_PARENTHESIZED_ASIDE = "[ ]*(\\([^()]+\\))?";
  private static final String NON_CAPTURING_PARENTHESIZED_ASIDE = "[ ]*(?:\\([^()]+\\))?";

  private static Pattern GLOSS_DEFINITIONS_PATTERN = Pattern.compile(
    GLOSS_DEFINITION_1_CAPTURING_PATTERN +
    Joiner.on("").join(Collections.nCopies(16, GLOSS_DEFINITION_REST_CAPTURING_PATTERN)) +
    GLOSS_EXAMPLES_NON_CAPTURING_PATTERN +
    ";?"+NON_CAPTURING_PARENTHESIZED_ASIDE+"$"
    );

  private static Pattern GLOSS_EXAMPLES_PATTERN = Pattern.compile(
    GLOSS_DEFINITIONS_NON_CAPTURING_PATTERN +
    Joiner.on("").join(Collections.nCopies(15, GLOSS_EXAMPLE_CAPTURING_PATTERN)) +
    ";?"+CAPTURING_PARENTHESIZED_ASIDE+"$"
    );

  /**
   * Some glosses contain multiple definitions.
   * @yawni.experimental
   * @see Synset#getGloss()
   */
  public static List<String> getDefinitions(final Synset synset) {
    final String sentenceGloss = synset.getGloss();
    final Matcher match = GLOSS_DEFINITIONS_PATTERN.matcher(sentenceGloss);
    if (! match.matches()) {
      throw new IllegalArgumentException("gloss definitions pattern failed for sentence gloss:\n" +
        sentenceGloss + " " + synset);
    }
    final int groupCount = match.groupCount();
    final List<String> definitions = new ArrayList<String>(groupCount);
    for (int i = 0; i < groupCount; i++) {
      final String groupText = match.group(i + 1);
      if (groupText != null) {
        definitions.add(groupText);
      }
    }
    return LightImmutableList.copyOf(definitions);
  }

  /**
   * Many glosses contain 1 or more examples; the formatting of a small fraction of the examples is
   * inconsistent.
   * @see Synset#getGloss()
   * @yawni.experimental
   */
  public static List<String> getExamples(final Synset synset) {
    final String sentenceGloss = synset.getGloss();
    final Matcher match = GLOSS_EXAMPLES_PATTERN.matcher(sentenceGloss);
    if (! match.matches()) {
      throw new IllegalArgumentException("gloss examples pattern failed for sentence gloss:\n" +
        sentenceGloss + " " + synset);
    }
    final int groupCount = match.groupCount();
    if (groupCount == 0) {
      return LightImmutableList.of();
    }
    final List<String> examples = new ArrayList<String>(groupCount);
    for (int i = 0; i < groupCount; i++) {
      final String groupText = match.group(i + 1);
      if (groupText != null) {
        examples.add(groupText);
      }
    }
    return LightImmutableList.copyOf(examples);
  }
}