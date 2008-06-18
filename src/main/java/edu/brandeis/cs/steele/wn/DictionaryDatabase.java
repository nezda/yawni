/*
 * Copyright 1998 by Oliver Steele.  You can use this software freely so long as you preserve
 * the copyright notice and this restriction, and label your changes.
 */
package edu.brandeis.cs.steele.wn;

/** 
 * A class that implements this interface is a broker or factory
 * for objects that model WordNet lexical and semantic entities.
 *
 * @see FileBackedDictionary
 * @author Oliver Steele, steele@cs.brandeis.edu
 * @version 1.0
 */
public interface DictionaryDatabase {
  /** 
   * Look up a <code>Word</code> in the database by its <b>lemma</b>.  The search is
   * case-independent and phrases are separated by spaces (e.g. "look up", not
   * "look_up").
   * @param pos The part-of-speech.
   * @param lemma The orthographic representation of the word.
   * @return An <code>Word</code> representing the word, or
   * <code>null</code> if no such entry exists.
   */
  public Word lookupWord(final POS pos, final String lemma);

  /** 
   * Return the base form of an exceptional derivation, if an entry for it
   * exists in the database. e.g. returns "goose" from derivation query term
   * "geese" as <code>POS.NOUN</code>.
   * @param pos The part-of-speech.
   * @param derivationLemma A (possibly <i>inflected</i>) form of the word.
   * @return The <i>uninflected</i> word, or <code>null</code> if no exception entry exists.
   */
  public String lookupBaseForm(final POS pos, final String derivationLemma);

  /**
   * Return all base forms (aka "lemmas") of <var>someString</var> in <var>pos</var>.
   * Utilizes an implementation of the <code>morphstr()</code> and <code>getindex()</code> algorithms.
   * @param pos The part-of-speech.
   * @param someString
   * @return baseform(s) of <var>someString</var>
   * @see <a href="http://wordnet.princeton.edu/man/morphy.7WN">http://wordnet.princeton.edu/man/morphy.7WN</a>
   */
  public String[] lookupBaseForms(final POS pos, final String someString);

  /** 
   * Could be built from basic API methods <code>lookupBaseForms()</code> and <code>lookupWord()</code>
   * and <code>Word.getSynsets()</code>.
   * @param pos The part-of-speech.
   * @param someString
   * @return <code>Synset</code>(s) of <var>someString</var> in <var>pos</var>
   */
  public Synset[] lookupSynsets(final POS pos, final String someString);

  /**
   * Return an iterator of <b>all</b> the <code>Word</code>s of in the database.
   * @param pos The part-of-speech.
   * @return An iterable of <code>Word</code>s.
   */
  public Iterable<Word> words(final POS pos);

  /** 
   * Return an iterator of all the <code>Word</code>s whose lemmas contain <var>substring</var>
   * as a <b>substring</b>.
   * @param pos The part-of-speech.
   * @param substring
   * @return An iterable of <code>Word</code>s.
   */
  public Iterable<Word> searchBySubstring(final POS pos, final String substring);

  /** 
   * Return an iterator of all the <code>Word</code>s whose lemmas <b>begin with</b> <var>prefix</var>.
   * @param pos The part-of-speech.
   * @param substring
   * @return An iterable of <code>Word</code>s.
   */
  public Iterable<Word> searchByPrefix(final POS pos, final String substring);

  /** Return an iterator of <b>all</b> the <code>Synset</code>s in the database.
   * @param pos The part-of-speech.
   * @return An iterable of <code>Synset</code>s.
   */
  public Iterable<Synset> synsets(final POS pos);
  
  /** Return an iterator of <b>all</b> the <code>WordSense</code>s in the database.
   * @param pos The part-of-speech.
   * @return An iterable of <code>WordSense</code>s.
   */
  public Iterable<WordSense> wordSenses(final POS pos);

  /** Return an iterator of <b>all</b> the <code>Pointer</code>s in the database.
   * @param pos The part-of-speech.
   * @return An iterable of <code>Pointer</code>s.
   */
  public Iterable<Pointer> pointers(final POS pos);

  /** Return an iterator of <b>all</b> the <code>Pointer</code>s in the database of
   * type <code>PointerType</code>.
   * @param pos The part-of-speech.
   * @param pointerType The PointerType.
   * @return An iterable of <code>Pointer</code>s of type <code>PointerType</code>.
   */
  public Iterable<Pointer> pointers(final POS pos, final PointerType pointerType);
}
