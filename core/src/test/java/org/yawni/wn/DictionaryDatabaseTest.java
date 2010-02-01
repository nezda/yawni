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

import org.yawni.util.Utils;
import static org.yawni.util.Utils.*;
import static org.fest.assertions.Assertions.assertThat;
import java.util.*;
import org.junit.BeforeClass;
import org.junit.Test;
import org.yawni.util.Joiner;

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
    assertThat(parents).hasSize(1); // though most Synsets have a single parent, not all do
    final RelationTarget parent = parents.get(0);
    //final Synset parentSyn = parent.getSynset();
    final List<RelationTarget> coordinateTerms = parent.getRelationTargets(RelationType.HYPONYM);

    // TODO
    // this is a confusing example :)
    // "antonym", "hyponym", "hypernym" will all be among the coordinate terms of "synonym"

    // comprehensive loops version
    // for Word from string
    //   for WordSense / Synset : word
    //     for parent : wordSense.getRelationTargets(RelationType.HYPERNYM)
    //       coordinateTerms/neighbors = parent.getRelationTargets(RelationType.HYPONYM)
    //
    // TODO:
    // represent each coordinateTerms as a RelationType; represent each coordinateTerm as a Relation
    // * multiParent sources add ambiguity, but maybe this doesn't matter ? define as "have <em>a</em> common parent
  }

  @Test
  public void synsetsCommands() {
    System.err.println("synsetsCommands");
    String query;
    Iterable<Synset> result;
    int offset;
    boolean caughtExpectedException;

    query = "?POS=ALL";
//    System.err.println("query: "+query);
    result = dictionary.synsets(query);
    assertThat(isEmpty(result)).isFalse();

    // basic 8-digit offset query
    // NOTE: leading zero integer literal is octal! (base 8), e.g., 04073208 != 4073208
    // in fact, 04073208 is not a valid octal number because 8 is not a valid octimal (?) digit
    assertThat(010).isEqualTo(8);
    offset = 4073208;
    //query = "?POS=n&offset=04073208";
    query = String.format("?POS=n&offset=%08d", offset);
//    System.err.println("query: "+query);
    result = dictionary.synsets(query);
    assertThat(size(result)).isEqualTo(1);
    assertThat(first(result).getOffset()).isEqualTo(offset);

    // simple happens-to-be-less-than-8-digit offset query with (mandatory) POS
    query = "?POS=n&offset=" + offset;
//    System.err.println("query: "+query);
    result = dictionary.synsets(query);
    assertThat(size(result)).isEqualTo(1);
    assertThat(first(result).getOffset()).isEqualTo(offset);

    // simple hapens-to-be-less-than-8-digit offset query, forgot POS
    query = "?offset=" + offset;
//    System.err.println("query: "+query);
    caughtExpectedException = false;
    try {
      result = dictionary.synsets(query);
    } catch (IllegalArgumentException e) {
      caughtExpectedException = true;
    }
    assertThat(caughtExpectedException).isTrue();
    
    // command repetition not supported
    query = "?POS=n&offset=04073208&offset=05847753";
//    System.err.println("query: "+query);
    caughtExpectedException = false;
    try {
      result = dictionary.synsets(query);
    } catch (IllegalArgumentException e) {
      caughtExpectedException = true;
    }
    assertThat(caughtExpectedException).isTrue();

    // command repetition not supported
    query = "?POS=n&POS=v";
//    System.err.println("query: "+query);
    caughtExpectedException = false;
    try {
      result = dictionary.synsets(query);
    } catch (IllegalArgumentException e) {
      caughtExpectedException = true;
    }
    assertThat(caughtExpectedException).isTrue();

    // basic ordinal POS query
    query = "?POS=1";
//    System.err.println("query: "+query);
    result = dictionary.synsets(query);
    assertThat(isEmpty(result)).isFalse();

    // 9-digit POS + offset query
    query = "?offset=104073208";
//    System.err.println("query: "+query);
    result = dictionary.synsets(query);
    assertThat(isEmpty(result)).isFalse();

    // invalid (random) synset offsets cause various exceptions in Synset parsing ctor
    query = "?offset=100001000";
//    System.err.println("query: "+query);
    caughtExpectedException = false;
    try {
      result = dictionary.synsets(query);
    } catch (IllegalArgumentException e) {
      caughtExpectedException = true;
    }
    assertThat(caughtExpectedException).isTrue();

    // lexname query
    query = "?lexname=verb.contact";
//    System.err.println("query: "+query);
    result = dictionary.synsets(query);
    assertThat(isEmpty(result)).isFalse();

    // lexname query
    query = "?lexname=contact";
//    System.err.println("query: "+query);
    result = dictionary.synsets(query);
    assertThat(isEmpty(result)).isFalse();

    // lexname query
    final String query1 = "?lexname=verb.contact";
    final String query2 = "?lexname=contact";
//    System.err.println("query1: "+query1);
//    System.err.println("query2: "+query2);
    final Iterable<Synset> result1 = dictionary.synsets(query1);
    final Iterable<Synset> result2 = dictionary.synsets(query2);
    assertThat(isEmpty(result1)).isFalse();
    assertThat(isEmpty(result2)).isFalse();
    assertThat(Utils.equals(result1, result2)).isTrue();

    // word query
    query = "?word=yawn";
//    System.err.println("query: "+query);
    result = dictionary.synsets(query);
    assertThat(size(result)).isEqualTo(3);

    // word query
    query = "?word=yawn&pos=NOUN";
//    System.err.println("query: "+query);
    result = dictionary.synsets(query);
    assertThat(size(result)).isEqualTo(1);

    // word query with stemming
    query = "?word=wounds";
//    System.err.println("query: "+query);
    result = dictionary.synsets(query);
//    System.err.println("query: "+query+" \n  "+Joiner.on("\n  ").join(result));
    assertThat(size(result)).isEqualTo(6);
  }

  @Test
  public void wordSensesCommands() {
    System.err.println("wordSensesCommands");
    String query;
    Iterable<WordSense> result;
    int offset;
    boolean caughtExpectedException;

    query = "?POS=ALL";
//    System.err.println("query: "+query);
    result = dictionary.wordSenses(query);
    assertThat(isEmpty(result)).isFalse();

    query = "?POS=1";
//    System.err.println("query: "+query);
    result = dictionary.wordSenses(query);
    assertThat(isEmpty(result)).isFalse();

    // adjposition query
    query = "?adj_position=PREDICATIVE";
//    System.err.println("query: "+query);
    result = dictionary.wordSenses(query);
    assertThat(isEmpty(result)).isFalse();

    // adjposition query
    query = "?adj_position=predicative";
//    System.err.println("query: "+query);
    result = dictionary.wordSenses(query);
    assertThat(isEmpty(result)).isFalse();

    // word query
    query = "?word=yawn";
//    System.err.println("query: "+query);
    result = dictionary.wordSenses(query);
    assertThat(size(result)).isEqualTo(3);

    // word query with POS
    query = "?word=yawn&pos=NOUN";
//    System.err.println("query: "+query);
    result = dictionary.wordSenses(query);
    assertThat(size(result)).isEqualTo(1);

    // word query with stemming
    query = "?word=wounds";
//    System.err.println("query: "+query);
    result = dictionary.wordSenses(query);
//    System.err.println("query: "+query+" \n  "+Joiner.on("\n  ").join(result));
    assertThat(size(result)).isEqualTo(6);

    offset = 4073208;
    query = String.format("?POS=n&offset=%08d", offset);
//    System.err.println("query: "+query);
    result = dictionary.wordSenses(query);
    assertThat(isEmpty(result)).isFalse();
    for (final WordSense wordSense : result) {
      assertThat(wordSense.getSynset().getOffset()).isEqualTo(offset);
    }
  }

  private static <T> boolean isUnique(final Collection<T> items) {
    return items.size() == new HashSet<T>(items).size();
  }
}