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

import java.util.List;

/**
 * A class that implements this interface is a broker or factory
 * for objects that model WordNet lexical and semantic entities.
 *
 * @see FileBackedDictionary
 */
public interface DictionaryDatabase {
  /**
   * Look up a {@code Word} in the database by its <b>lemma</b>.  The search is
   * case-independent and phrases are separated by spaces (e.g., "look up", not
   * "look_up"), but otherwise {@code lemma}  must match the form in the
   * database exactly.  Similar to C function {@code index_lookup}.
   * Note that {@link POS#ALL} doesn't make sense here because the result
   * would no longer be unique (i.e., a scalar, single {@code Word}).
   * @param lemma The orthographic representation of the word.
   * @param pos The part-of-speech.
   * @return An {@code Word} representing the word, or
   * {@code null} if no such entry exists.
   */
  public Word lookupWord(final CharSequence lemma, final POS pos);

  ///**
  // * Return the base form of an exceptional derivation, if an entry for it
  // * exists in the database. e.g., returns "goose" from derivation query term
  // * "geese" as {@code POS.NOUN}.
  // * @param pos The part-of-speech.
  // * @param derivationLemma A (possibly <i>inflected</i>) form of the word.
  // * @return The <i>uninflected</i> word, or {@code null} if no exception entry exists.
  // */
  //public String lookupBaseForm(final POS pos, final String derivationLemma);

  /**
   * Return all <strong>properly cased</strong> (aka "true cased") base forms (aka "lemmas", "stems") of {@code someString} in {@code pos}.
   * Utilizes an implementation of the {@code morphstr()} and {@code getindex()} algorithms.
   * See {@link WordSense#getLemma()} for a description of "true cased" base forms.
   * @param someString someString does <em>not</em> need to be a base form
   * @param pos The part-of-speech.
   * @return an immutable list of the baseform(s) of {@code someString}
   * @see <a href="http://wordnet.princeton.edu/man/morphy.7WN.html">http://wordnet.princeton.edu/man/morphy.7WN.html</a>
   */
  public List<String> lookupBaseForms(final String someString, final POS pos);

  /**
   * Convenient combination of basic API methods {@link #lookupBaseForms()}, {@link #lookupWord()}
   * and {@link Word#getSynsets()}.
   * @param someString
   * @param pos The part-of-speech.
   * @return an immutable list of the {@code Synset}(s) of {@code someString} in {@code pos}
   */
  public List<Synset> lookupSynsets(final String someString, final POS pos);

  /**
   * Return an iterator of <b>all</b> the {@code Word}s in the database.
   * @param pos The part-of-speech.
   * @return An iterable of {@code Word}s.
   */
  public Iterable<Word> words(final POS pos);

  /**
   * Return an iterator of all the {@code Word}s whose <em>lemmas</em> contain {@code substring}
   * as a <b>substring</b>.
   * @param substring The substring to search for.
   * @param pos The part-of-speech.
   * @return An iterable of {@code Word}s.
   * @see <a href="http://wordnet.princeton.edu/wordnet/man/wn.1WN.html"><code>wn -grep (<i>n</i>|<i>v</i>|<i>a</i>|<i>r</i>)</code></a>
   */
  public Iterable<Word> searchBySubstring(final CharSequence substring, final POS pos);

  /**
   * Return an iterator of all the {@code Word}s whose <em>lemmas</em> <b>begin with</b> {@code prefix}.
   * @param prefix The prefix to search for.
   * @param pos The part-of-speech.
   * @return An iterable of {@code Word}s.
   */
  public Iterable<Word> searchByPrefix(final CharSequence prefix, final POS pos);

  /**
   * Return an iterator of all the {@code Synset}s whose gloss <b>contains</b> {@code substring}
   * <em>without stemming</em> (i.e., {@link #lookupBaseForms()}).
   * @param substring The substring to search for.
   * @param pos The part-of-speech.
   * @return An iterable of {@code Synset}s.
   */
  // Not yet implemented
//  public Iterable<Synset> searchGlossBySubstring(final CharSequence substring, final POS pos);

  /**
   * Return an iterator of <b>all</b> the {@code Synset}s in the database.
   * @param pos The part-of-speech.
   * @return An iterable of {@code Synset}s.
   */
  public Iterable<Synset> synsets(final POS pos);

  /**
   * Return an iterator of <b>all</b> the {@code WordSense}s in the database.
   * @param pos The part-of-speech.
   * @return An iterable of {@code WordSense}s.
   */
  public Iterable<WordSense> wordSenses(final POS pos);

  /**
   * Return an iterator of <b>all</b> the {@code Relation}s in the database.
   * @param pos The part-of-speech.
   * @return An iterable of {@code Relation}s.
   */
  public Iterable<Relation> relations(final POS pos);

  /**
   * Return an iterator of <b>all</b> the {@code Relation}s in the database of
   * type {@code RelationType}.
   * @param relationType The {@code RelationType}. {@code null} implies <b>all</b> {@code RelationType}s.
   * @param pos The part-of-speech.
   * @return An iterable of {@code Relation}s of type {@code RelationType}.
   */
  public Iterable<Relation> relations(final RelationType relationType, final POS pos);

  /**
   * Return an iterator of <b>all</b> the exceptions for the given part-of-speech.
   * @param pos The part-of-speech.
   * @return An iterable of the exceptional strings.
   */
  public Iterable<List<String>> exceptions(final POS pos);
}