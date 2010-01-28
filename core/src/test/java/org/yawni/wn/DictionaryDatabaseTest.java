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
package org.yawni.wn;

import static org.yawni.util.Utils.*;
import static org.fest.assertions.Assertions.assertThat;
import java.util.*;
import org.junit.BeforeClass;
import org.junit.Test;

public class DictionaryDatabaseTest {
  private static DictionaryDatabase dictionary;
  @BeforeClass
  public static void init() {
    dictionary = FileBackedDictionary.getInstance();
  }

  /** 
   * test POS.ALL support
   * + List<String> lookupBaseForms(POS pos, String someString)
   * - List<Synset> lookupSynsets(POS pos, String someString)
   * - Iterator<Relation> relations(POS pos)
   * - Iterator<Relation> relations(POS pos, RelationType relationType)
   * - Iterator<Word> words(POS pos)
   * - Iterator<Synset> synsets(POS pos)
   * - Iterator<WordSense> wordSenses(POS pos)
   */
  //@Ignore
  @Test
  public void test() {
    final List<String> tanks = dictionary.lookupBaseForms("tank", POS.ALL);
    System.err.println("tanks: "+tanks);
    final List<String> geese = dictionary.lookupBaseForms("geese", POS.ALL);
    System.err.println("geese: "+geese);
  }

  /**
   * Expects exception because POS.ALL does not make sense for method 
   * lookupWord(POS, lemma) which returns at most 1 result.
   * TODO subclass IllegalArgumentException (a RuntimeException) 
   * indicating this.
   */
  @Test(expected=IllegalArgumentException.class)
  public void test_lookupWord() {
    dictionary.lookupWord("tank", POS.ALL);
  }

  /**
   * Look for warning issues with lookupSynsets()
   */
  @Test
  public void lookupSynsetsTest() {
    //String str = "allow for";
    //String str = "allowing for";
    String str = "allows for";
    POS pos = POS.VERB;

    List<Synset> syns = dictionary.lookupSynsets(str, pos);
    if (syns.isEmpty()) {
      System.err.println("XXX PROBLEM: "+str+" no syns found (loopback failure)");
    }
    //System.err.println(str+": "+Arrays.toString(syns));
    syns = dictionary.lookupSynsets("compromise", POS.ALL);
    assertThat(isUnique(syns)).isTrue();
    assertThat(syns.size()).isEqualTo(5);
  }

  @Test
  public void coordinateTerms() {
    final Word synonyms = dictionary.lookupWord("synonyms", POS.NOUN);
    assertThat(synonyms).isNull();
    final Word synonym = dictionary.lookupWord("synonym", POS.NOUN);
    final List<WordSense> senses = synonym.getWordSenses();
    assertThat(senses).hasSize(1);
    final WordSense sense = senses.get(0);
    final List<RelationTarget> parents = sense.getRelationTargets(RelationType.HYPERNYM);
    assertThat(parents).hasSize(1);
    final RelationTarget parent = parents.get(0);

    // TODO
    // this is a confusing example :)
    // "antonym", "hyponym", "hypernym" will all be among the coordinate terms of "synonym"
  }

  @Test
  public void synsets() {
    String query;
    Iterable<Synset> result;

    query = "?POS=ALL";
    System.err.println("query: "+query);
    result = dictionary.synsets(query);
    System.err.println("isEmpty? "+isEmpty(result));

    query = "?POS=n&offset=04073208";
    System.err.println("query: "+query);
    result = dictionary.synsets(query);
    System.err.println("isEmpty? "+isEmpty(result));

    // command repetition not supported
//    query = "?POS=n&offset=04073208&offset=05847753";
//    System.err.println("query: "+query);
//    result = dictionary.synsets(query);
//    System.err.println("isEmpty? "+isEmpty(result));

    query = "?POS=1";
    System.err.println("query: "+query);
    result = dictionary.synsets(query);
    System.err.println("isEmpty? "+isEmpty(result));

    query = "?offset=104073208";
    System.err.println("query: "+query);
    result = dictionary.synsets(query);
    System.err.println("isEmpty? "+isEmpty(result));

    // invalid (e.g., random) synset offsets cause various exceptions
    // in Synset parsing ctor
//    query = "?offset=100001000";
//    System.err.println("query: "+query);
//    result = dictionary.synsets(query);
//    System.err.println("isEmpty? "+isEmpty(result));
  }

  private static <T> boolean isUnique(final Collection<T> items) {
    return items.size() == new HashSet<T>(items).size();
  }
}