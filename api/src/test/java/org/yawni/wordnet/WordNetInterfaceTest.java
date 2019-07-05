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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import static com.google.common.collect.Iterables.isEmpty;
import static com.google.common.collect.Iterables.size;
import static org.fest.assertions.Assertions.assertThat;
import java.util.*;
import org.junit.BeforeClass;
import org.junit.Test;
import org.yawni.util.Utils;
import org.yawni.wordnet.WordNetInterface.WordNetVersion;

public class WordNetInterfaceTest {
  private static WordNetInterface WN;
	private static WordNetVersion VERSION;

  @BeforeClass
  public static void init() {
    WN = WordNet.getInstance();
		VERSION = WordNetVersion.detect();
  }

	@Test
	public void testWordNetVersion() {
		final WordNetVersion version = WordNetVersion.detect();
		System.err.println("WordNetVersion: "+version);
	}

  /**
   * test POS.ALL support
   * + {@code List<String> lookupBaseForms(POS pos, String someString)}
   * - {@code List<Synset> lookupSynsets(POS pos, String someString)}
   * - {@code Iterator<Relation> relations(POS pos)}
   * - {@code Iterator<Relation> relations(POS pos, RelationType relationType)}
   * - {@code Iterator<Word> words(POS pos)}
   * - {@code Iterator<Synset> synsets(POS pos)}
   * - {@code Iterator<WordSense> wordSenses(POS pos)}
   */
  //@Ignore
  @Test
  public void test() {
    String query;
    List<String> results;
    query = "tank";
    results = WN.lookupBaseForms(query, POS.ALL);
		assertThat(results).containsExactly("tank");
//    System.err.println("query: "+query+" results: "+results);
    query = "geese";
    results = WN.lookupBaseForms(query, POS.ALL);
//    System.err.println("query: "+query+" results: "+results);
		assertThat(results).containsExactly("goose", "geese");
    query = "mouse";
    results = WN.lookupBaseForms(query, POS.ALL);
//    System.err.println("query: "+query+" results: "+results);
		assertThat(results).containsExactly("mouse");
    // queries with more than 1 baseform
    query = "mice";
    results = WN.lookupBaseForms(query, POS.ALL);
//    System.err.println("query: "+query+" results: "+results);
		assertThat(results).containsExactly("mouse", "mice");
    query = "wings";
    results = WN.lookupBaseForms(query, POS.ALL);
//    System.err.println("query: "+query+" results: "+results);
		assertThat(results).containsExactly("wing", "wings");
    query = "years";
    results = WN.lookupBaseForms(query, POS.ALL);
//    System.err.println("query: "+query+" results: "+results);
		assertThat(results).containsExactly("year", "years");
    query = "businessmen";
    results = WN.lookupBaseForms(query, POS.ALL);
//    System.err.println("query: "+query+" results: "+results);
		assertThat(results).containsExactly("businessman", "businessmen");
    query = "men";
    results = WN.lookupBaseForms(query, POS.ALL);
//    System.err.println("query: "+query+" results: "+results);
		assertThat(results).containsExactly("man", "men");
    query = "was";
    results = WN.lookupBaseForms(query, POS.ALL);
//    System.err.println("query: "+query+" results: "+results);
		assertThat(results).containsExactly("WA", "be", "was");
  }

  /**
   * Expects exception because POS.ALL does not make sense for method
   * lookupWord(POS, lemma) which returns at most 1 result.
   * TODO subclass IllegalArgumentException (a RuntimeException)
   * indicating this.
   */
  @Test(expected=IllegalArgumentException.class)
  public void test_lookupWord() {
    WN.lookupWord("tank", POS.ALL);
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

    List<Synset> syns = WN.lookupSynsets(str, pos);
    if (syns.isEmpty()) {
      System.err.println("XXX PROBLEM: "+str+" no syns found (loopback failure)");
    }
    //System.err.println(str+": "+Arrays.toString(syns));
    syns = WN.lookupSynsets("compromise", POS.ALL);
    assertThat(isUnique(syns)).isTrue();
    assertThat(syns.size()).isEqualTo(5);
  }

  @Test
  public void coordinateTerms() {
    final Word synonyms = WN.lookupWord("synonyms", POS.NOUN);
//    assertThat(synonyms).isNull();
    assertThat(synonyms == null).isTrue();
    final Word synonym = WN.lookupWord("synonym", POS.NOUN);
    final List<WordSense> senses = synonym.getWordSenses();
    assertThat(senses).hasSize(1);
    final WordSense sense = senses.get(0);
    final List<RelationArgument> parents = sense.getRelationTargets(RelationType.HYPERNYM);
    assertThat(parents).hasSize(1); // though most Synsets have a single parent, not all do
    final RelationArgument parent = parents.get(0);
    //final Synset parentSyn = parent.getSynset();
    final List<RelationArgument> coordinateTerms = parent.getRelationTargets(RelationType.HYPONYM);

    // TODO
    // this is a confusing example :)
    // "antonym", "hyponym", "hypernym" will all be among the coordinate terms of "synonym"

    // comprehensive loops VERSION
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
		if (VERSION != WordNetVersion.WN30) {
			return;
		}
    String query;
    Iterable<Synset> result;
    int offset;
    boolean caughtExpectedException;

    query = "?POS=ALL";
//    System.err.println("query: "+query);
    result = WN.synsets(query);
    assertThat(isEmpty(result)).isFalse();

    // basic 8-digit offset query
    // NOTE: leading zero integer literal is octal! (base 8), e.g., 04073208 != 4073208
    // in fact, 04073208 is not a valid octal number because 8 is not a valid octimal (?) digit
    assertThat(010).isEqualTo(8);

    offset = 4073208;
    //query = "?POS=n&offset=04073208";
    query = String.format("?POS=n&offset=%08d", offset);
//    System.err.println("query: "+query);
    result = WN.synsets(query);
    assertThat(size(result)).isEqualTo(1);
    assertThat(Utils.first(result).getOffset()).isEqualTo(offset);

    // simple happens-to-be-less-than-8-digit offset query with (mandatory) POS
    query = "?POS=n&offset=" + offset;
//    System.err.println("query: "+query);
    result = WN.synsets(query);
    assertThat(size(result)).isEqualTo(1);
    assertThat(Utils.first(result).getOffset()).isEqualTo(offset);

    // simple hapens-to-be-less-than-8-digit offset query, forgot POS
    query = "?offset=" + offset;
//    System.err.println("query: "+query);
    caughtExpectedException = false;
    try {
      WN.synsets(query);
    } catch (IllegalArgumentException e) {
      caughtExpectedException = true;
    }
    assertThat(caughtExpectedException).isTrue();

    // command repetition not supported
    query = "?POS=n&offset=04073208&offset=05847753";
//    System.err.println("query: "+query);
    caughtExpectedException = false;
    try {
      WN.synsets(query);
    } catch (IllegalArgumentException e) {
      caughtExpectedException = true;
    }
    assertThat(caughtExpectedException).isTrue();

    // command repetition not supported
    query = "?POS=n&POS=v";
//    System.err.println("query: "+query);
    caughtExpectedException = false;
    try {
      WN.synsets(query);
    } catch (IllegalArgumentException e) {
      caughtExpectedException = true;
    }
    assertThat(caughtExpectedException).isTrue();

    // basic ordinal POS query
    query = "?POS=1";
//    System.err.println("query: "+query);
    result = WN.synsets(query);
    assertThat(isEmpty(result)).isFalse();

    // 9-digit POS + offset query
    query = "?offset=104073208";
//    System.err.println("query: "+query);
    result = WN.synsets(query);
    assertThat(isEmpty(result)).isFalse();

    // invalid (random) synset offsets cause various exceptions in Synset parsing ctor
    query = "?offset=100001000";
//    System.err.println("query: "+query);
    caughtExpectedException = false;
    try {
      WN.synsets(query);
    } catch (IllegalArgumentException e) {
      caughtExpectedException = true;
    }
    assertThat(caughtExpectedException).isTrue();

    // lexname query
    query = "?lexname=verb.contact";
//    System.err.println("query: "+query);
    result = WN.synsets(query);
    assertThat(isEmpty(result)).isFalse();

    // lexname query
    query = "?lexname=contact";
//    System.err.println("query: "+query);
    result = WN.synsets(query);
    assertThat(isEmpty(result)).isFalse();

    // lexname query
    final String query1 = "?lexname=verb.contact";
    final String query2 = "?lexname=contact";
//    System.err.println("query1: "+query1);
//    System.err.println("query2: "+query2);
    final Iterable<Synset> result1 = WN.synsets(query1);
    final Iterable<Synset> result2 = WN.synsets(query2);
    assertThat(isEmpty(result1)).isFalse();
    assertThat(isEmpty(result2)).isFalse();
    assertThat(Iterables.elementsEqual(result1, result2)).isTrue();

    // word query
    query = "?word=yawn";
//    System.err.println("query: "+query);
    result = WN.synsets(query);
    assertThat(size(result)).isEqualTo(3);

    // word query
    query = "?word=yawn&pos=NOUN";
//    System.err.println("query: "+query);
    result = WN.synsets(query);
    assertThat(size(result)).isEqualTo(1);

    // word query with stemming
    query = "?word=wounds";
//    System.err.println("query: "+query);
    result = WN.synsets(query);
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
    result = WN.wordSenses(query);
    assertThat(isEmpty(result)).isFalse();

    query = "?POS=1";
//    System.err.println("query: "+query);
    result = WN.wordSenses(query);
    assertThat(isEmpty(result)).isFalse();

    // adjposition query
    query = "?adj_position=PREDICATIVE";
//    System.err.println("query: "+query);
    result = WN.wordSenses(query);
    assertThat(isEmpty(result)).isFalse();

    // adjposition query
    query = "?adj_position=predicative";
//    System.err.println("query: "+query);
    result = WN.wordSenses(query);
    assertThat(isEmpty(result)).isFalse();

    // word query
    query = "?word=yawn";
//    System.err.println("query: "+query);
    result = WN.wordSenses(query);
    assertThat(size(result)).isEqualTo(3);

    // word query with POS
    query = "?word=yawn&pos=NOUN";
//    System.err.println("query: "+query);
    result = WN.wordSenses(query);
    assertThat(size(result)).isEqualTo(1);

    // word query with stemming
    query = "?word=wounds";
//    System.err.println("query: "+query);
    result = WN.wordSenses(query);
//    System.err.println("query: "+query+" \n  "+Joiner.on("\n  ").join(result));
    assertThat(size(result)).isEqualTo(6);

		if (VERSION == WordNetVersion.WN30) {
			offset = 4073208;
			query = String.format("?POS=n&offset=%08d", offset);
//	    System.err.println("query: "+query);
			result = WN.wordSenses(query);
			assertThat(isEmpty(result)).isFalse();
			for (final WordSense wordSense : result) {
				assertThat(wordSense.getSynset().getOffset()).isEqualTo(offset);
			}
		}
  }

  @Test
  public void coreRankTest() {
		if (VERSION == WordNetVersion.WN30) {
			assertThat(WN.lookupWord("time", POS.NOUN).getSense(7).getCoreRank()).isEqualTo(1);
			assertThat(WN.lookupWord("time", POS.NOUN).getSense(1).getCoreRank()).isEqualTo(-1);
		}
  }

  private static <T> boolean isUnique(final Collection<T> items) {
    return items.size() == new HashSet<>(items).size();
  }
}