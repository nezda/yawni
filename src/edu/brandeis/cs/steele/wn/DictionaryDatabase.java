/*
 * WordNet-Java
 *
 * Copyright 1998 by Oliver Steele.  You can use this software freely so long as you preserve
 * the copyright notice and this restriction, and label your changes.
 */
package edu.brandeis.cs.steele.wn;
import java.util.*;

/** 
 * An object in a class that implements this interface is a broker or factory
 * for objects that model WordNet lexical and semantic entities.
 *
 * @see FileBackedDictionary
 * @author Oliver Steele, steele@cs.brandeis.edu
 * @version 1.0
 */
public interface DictionaryDatabase {
  /** 
   * Look up a word in the database.  The search is case-independent and
   * phrases are separated by spaces (e.g. "look up", not "look_up").
   * @param pos The part-of-speech.
   * @param lemma The orthographic representation of the word.
   * @return An <code>IndexWord</code> representing the word, or
   * <code>null</code> if no such entry exists.
   */
  public IndexWord lookupIndexWord(final POS pos, final String lemma);

  /** 
   * Return the base form of an exceptional derivation, if an entry for it
   * exists in the database. e.g. returns "goose" from derivation query term
   * "geese" as <code>POS.NOUN</code>.
   * @param pos The part-of-speech.
   * @param derivationLemma A (possibly <i>inflected</i>) form of the a word.
   * @return The <i>uninflected</i> word, or <code>null</code> if no exception entry exists.
   */
  public String lookupBaseForm(final POS pos, final String derivationLemma);

  /**
   * Return all base forms (aka "lemmas") of <var>someString</var> in <var>pos</var>.
   * Utilizes an implementation of the <code>moprhstr()</code> and <code>getindex()</code> algorithms.
   * @param pos The part-of-speech.
   * @param someString
   * @return baseform(s) of <var>someString</var>
   */
  public String[] lookupBaseForms(final POS pos, final String someString);

  /** 
   * Could be built from basic API methods lookupBaseForms() and lookupIndexWord() and IndexWord.getSynsets().
   * LN Added. 
   * @param pos The part-of-speech.
   * @param someString
   * @return <code>Synset</code>(s) of <var>someString</var> in <var>pos</var>
   */
  public Synset[] lookupSynsets(final POS pos, final String someString);

  /**
   * Return an enumeration of <b>all</b> the IndexWords of in the database.
   * @param pos The part-of-speech.
   * @return An iterable of <code>IndexWord</code>s.
   */
  public Iterable<IndexWord> indexWords(final POS pos);

  /** 
   * Return an enumeration of all the IndexWords whose lemmas contain <var>substring</var>
   * as a <b>substring</b>.
   * @param pos The part-of-speech.
   * @param substring
   * @return An iterable of <code>IndexWord</code>s.
   */
  public Iterable<IndexWord> searchIndexWords(final POS pos, final String substring);

  /** 
   * Return an enumeration of all the IndexWords whose lemmas <b>begin with</b> <var>prefix</var>.
   * @param pos The part-of-speech.
   * @param substring
   * @return An iterable of <code>IndexWord</code>s.
   */
  public Iterable<IndexWord> searchIndexBeginning(final POS pos, final String substring);

  /** Return an enumeration over <b>all</b> the Synsets in the database.
   * @param pos The part-of-speech.
   * @return An iterable of <code>Synset</code>s.
   */
  public Iterable<Synset> synsets(final POS pos);
}
