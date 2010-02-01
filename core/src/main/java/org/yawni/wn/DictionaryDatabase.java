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
   * Look up a {@code Word} in the database by its <strong>lemma</strong>.  The search is
   * case-independent and phrases are separated by spaces (e.g., "look up", not
   * "look_up"), but otherwise {@code lemma}  must match the form in the
   * database <em>exactly</em>.  Similar to C function {@code index_lookup}.
   * Note that {@link POS#ALL} doesn't make sense here because the result
   * would no longer be unique (i.e., a scalar, single {@code Word}).
   * @param lemma The orthographic representation of the word.
   * @param pos The part-of-speech.
   * @return An {@code Word} representing the word, or
   * {@code null} if no such entry exists.
   */
  public Word lookupWord(final CharSequence lemma, final POS pos);

  /**
   * Return all <strong>properly cased</strong> (aka "true cased") base forms (aka "lemmas", "stems"),
   * as well as any exceptional forms, of {@code someString} in {@code pos}.
   * Utilizes an implementation of the {@code morphstr()} and {@code getindex()} algorithms.
   * See {@link WordSense#getLemma()} for a description of "true cased" base forms.
   * @param someString someString does <em>not</em> need to be a base form
   * @param pos The part-of-speech.
   * @return an immutable list of the baseform(s) of {@code someString}
   * @see <a href="http://wordnet.princeton.edu/man/morphy.7WN.html">
   *   http://wordnet.princeton.edu/man/morphy.7WN.html</a>
   * @see <a href="http://wordnet.princeton.edu/man/morphy.7WN.html#sect3">
   *   http://wordnet.princeton.edu/man/morphy.7WN.html#sect3 describes 'exceptional forms'</a>
   */
  public List<String> lookupBaseForms(final String someString, final POS pos);

  /**
   * Convenient combination of basic API methods {@link #lookupBaseForms}, {@link #lookupWord}
   * and {@link Word#getSynsets}.
   * @param someString
   * @param pos The part-of-speech.
   * @return an immutable list of the {@code Synset}(s) of {@code someString} in {@code pos}
   */
  public List<Synset> lookupSynsets(final String someString, final POS pos);

  /**
   * Convenient combination of basic API methods {@link #lookupBaseForms}, {@link #lookupWord}
   * and {@link Word#getWordSenses()}.
   * For {@code pos !=}{@link POS#ALL}, usually returns a single result, though there are
   * numerous exceptions (TODO e.g., XXX).
   * For {@code pos == ALL}, multiple results are even more common (TODO e.g., XXX).
   * @see #lookupSynsets
   */
  public List<WordSense> lookupWordSenses(final String someString, final POS pos);

  /**
   * Return an iterator of <strong>all</strong> the {@code Word}s in the database.
   * @param pos The part-of-speech.
   * @return An iterable of {@code Word}s.
   */
  public Iterable<Word> words(final POS pos);

  /**
   * Return an iterator of <strong>all</strong> the {@code Word}s whose <em>lemmas</em> contain {@code substring}
   * as a <strong>substring</strong>.
   * @param substring The substring to search for.
   * @param pos The part-of-speech.
   * @return An iterable of {@code Word}s.
   * @throws java.util.regex.PatternSyntaxException
   * @see <a href="http://wordnet.princeton.edu/wordnet/man/wn.1WN.html">
   *   <code>wn -grep (<i>n</i>|<i>v</i>|<i>a</i>|<i>r</i>)</code></a>
   */
  public Iterable<Word> searchBySubstring(final CharSequence substring, final POS pos);

  /**
   * Return an iterator of <strong>all</strong> the {@code Word}s whose <em>lemmas</em>
   * <strong>begin with</strong> {@code prefix} (case insensitive).
   * @param prefix The prefix to search for.
   * @param pos The part-of-speech.
   * @return An iterable of {@code Word}s.
   */
  public Iterable<Word> searchByPrefix(final CharSequence prefix, final POS pos);

  /**
   * Return an iterator of all the {@code Synset}s whose gloss <strong>contains</strong> {@code substring} (case sensitive).
   * @param substring The substring to search for.
   * @param pos The part-of-speech.
   * @return An iterable of {@code Synset}s.
   * @throws java.util.regex.PatternSyntaxException
   */
   public Iterable<Synset> searchGlossBySubstring(final CharSequence substring, final POS pos);

  /**
   * Return an iterator of <strong>all</strong> the {@code Synset}s in the database.
   * @param pos The part-of-speech.
   * @return An iterable of {@code Synset}s.
   */
  public Iterable<Synset> synsets(final POS pos);

  /**
   * Return an iterator of <strong>all</strong> the {@code WordSense}s in the database.
   * @param pos The part-of-speech.
   * @return An iterable of {@code WordSense}s.
   */
  public Iterable<WordSense> wordSenses(final POS pos);

  /**
   * Return an iterator of <strong>all</strong> the {@code Relation}s in the database.
   * @param pos The part-of-speech.
   * @return An iterable of {@code Relation}s.
   */
  public Iterable<Relation> relations(final POS pos);

  /**
   * Return an iterator of <strong>all</strong> the {@code Relation}s in the database of
   * type {@code RelationType}.
   * @param relationType The {@code RelationType}. {@code null} implies <strong>all</strong> {@code RelationType}s.
   * @param pos The part-of-speech.
   * @return An iterable of {@code Relation}s of type {@code RelationType}.
   */
  public Iterable<Relation> relations(final RelationType relationType, final POS pos);

  /**
   * Return an iterator of <strong>all</strong> the exceptions for the given part-of-speech.
   * @param pos The part-of-speech.
   * @return An iterable of the exceptional strings.
   * @see <a href="http://wordnet.princeton.edu/man/morphy.7WN.html#sect3">
   *   http://wordnet.princeton.edu/man/morphy.7WN.html#sect3</a>
   */
  public Iterable<List<String>> exceptions(final POS pos);

  /**
   * Return an iterator of {@code Synset}s matching {@code query}.
   * @param query
   * @return An iterable of {@code Synset}s.
   * @throws IllegalArgumentException, and other {@link RuntimeException}s indicate an unsupported and/or malformed query.
   * @yawni.experimental
   */
  public Iterable<Synset> synsets(final String query);

  /**
   * Return an iterator of {@code WordSense}s matching {@code query}.
   * @param query
   * @return An iterable of {@code WordSense}s.
   * @throws IllegalArgumentException, and other {@link RuntimeException}s indicate an unsupported and/or malformed query.
   * @yawni.experimental
   */
  public Iterable<WordSense> wordSenses(final String query);
}