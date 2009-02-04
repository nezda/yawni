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
/*
 * Copyright 1998 by Oliver Steele.  You can use this software freely so long as you preserve
 * the copyright notice and this restriction, and label your changes.
 */
package edu.brandeis.cs.steele.wn;

import edu.brandeis.cs.steele.util.Utils;

import java.util.logging.*;
import java.util.EnumSet;
import java.util.Arrays;
import java.util.Iterator;

/**
 * A <code>Word</code> represents a line of a WordNet <code>index.<em>pos</em></code> file.
 * A <code>Word</code> is retrieved via {@link DictionaryDatabase#lookupWord},
 * and has a <i>lemma</i>, a <i>part of speech (POS)</i>, and a set of <i>senses</i>, which are of type {@link Synset}.
 *
 * XXX<p>Debatable what the type of each sense is - Steele said Synset, i'd say WordSense.
 *
 * <p>Note this class used to be called <tt>IndexWord</tt> which arguably makes more sense from the
 * WordNet perspective.
 *
 * @see Synset
 * @see WordSense
 * @see Pointer
 */
public final class Word implements Comparable<Word>, Iterable<WordSense> {
  private static final Logger log = Logger.getLogger(Word.class.getName());

  private final FileBackedDictionary fileBackedDictionary;
  /** offset in <var>pos</var><code>.index</code> file */
  private final int offset;
  /** No case "lemma". Each {@link WordSense} has at least 1 true case lemma
   * (could vary by POS).
   */
  private final String lemma;
  // number of senses with counts in sense tagged corpora
  private final int taggedSenseCount;
  /** Synsets are initially stored as offsets, and paged in on demand
   * of the first call of {@link #getSynsets()}.
   */
  private Object synsets;

  private EnumSet<PointerType> ptrTypes;
  private final byte posOrdinal;
  //
  // Constructor
  //
  Word(final CharSequence line, final int offset, final FileBackedDictionary fileBackedDictionary) {
    this.fileBackedDictionary = fileBackedDictionary;
    try {
      log.log(Level.FINEST, "parsing line: {0}", line);
      final CharSequenceTokenizer tokenizer = new CharSequenceTokenizer(line, " ");
      this.lemma = tokenizer.nextToken().toString().replace('_', ' ');
      this.posOrdinal = (byte) POS.lookup(tokenizer.nextToken()).ordinal();

      tokenizer.skipNextToken(); // poly_cnt
      //final int poly_cnt = tokenizer.nextInt(); // poly_cnt
      final int pointerCount = tokenizer.nextInt();
      //this.ptrTypes = EnumSet.noneOf(PointerType.class);
      for (int i = 0; i < pointerCount; i++) {
        //XXX each of these tokens is a pointertype, although it may be may be
        //incorrect - see getPointerTypes() comments)
        tokenizer.skipNextToken();
        //  try {
        //    ptrTypes.add(PointerType.parseKey(tokenizer.nextToken()));
        //  } catch (final java.util.NoSuchElementException exc) {
        //    log.log(Level.SEVERE, "Word() got PointerType.parseKey() error:", exc);
        //  }
      }

      this.offset = offset;
      final int senseCount = tokenizer.nextInt();
      // this is redundant information
      //assert senseCount == poly_cnt;
      this.taggedSenseCount = tokenizer.nextInt();
      final int[] synsetOffsets = new int[senseCount];
      for (int i = 0; i < senseCount; i++) {
        synsetOffsets[i] = tokenizer.nextInt();
      }
      this.synsets = synsetOffsets;
      //final EnumSet<PointerType> actualPtrTypes = EnumSet.noneOf(PointerType.class);
      //for (final Synset synset : getSynsets()) {
      //  for (final Pointer pointer : synset.getPointers()) {
      //    final PointerType ptrType = pointer.getType();
      //    actualPtrTypes.add(ptrType);
      //  }
      //}
      //// in actualPtrTypes, NOT ptrTypes
      //final EnumSet<PointerType> missing = EnumSet.copyOf(actualPtrTypes); missing.removeAll(ptrTypes);
      //// in ptrTypes, NOT actualPtrTypes
      //final EnumSet<PointerType> extra = EnumSet.copyOf(ptrTypes); extra.removeAll(actualPtrTypes);
      //if(false == missing.isEmpty()) {
      //  //log.log(Level.SEVERE, "missing: {0}", missing);
      //}
      //if(false == extra.isEmpty()) {
      //  //log.log(Level.SEVERE, "extra: {0}", extra);
      //}
    } catch (final RuntimeException e) {
      log.log(Level.SEVERE, "Word parse error on offset: {0} line:\n\"{1}\"",
          new Object[]{ offset, line });
      log.log(Level.SEVERE, "",  e);
      throw e;
    }
  }

  //
  // Accessors
  //
  public POS getPOS() {
    return POS.fromOrdinal(posOrdinal);
  }

  /**
   * The pointer types available for this word.  May not apply to all
   * senses of the word.
   */
  public EnumSet<PointerType> getPointerTypes() {
    if (ptrTypes == null) {
      // these are not always correct
      // PointerType.INSTANCE_HYPERNYM
      // PointerType.HYPERNYM
      // PointerType.INSTANCE_HYPONYM
      // PointerType.HYPONYM
      final EnumSet<PointerType> localPtrTypes = EnumSet.noneOf(PointerType.class);
      for (final Synset synset : getSynsets()) {
        for (final Pointer pointer : synset.getPointers()) {
          final PointerType ptrType = pointer.getType();
          localPtrTypes.add(ptrType);
        }
      }
      this.ptrTypes = localPtrTypes;
    }
    return ptrTypes;
  }

  /**
   * Returns the <code>Word</code>'s lowercased <i>lemma</i>.  Its lemma is its orthographic
   * representation, for example <tt>"dog"</tt> or <tt>"get up"</tt>
   * or <tt>"u.s.a."</tt>.
   * <p>Note that different senses of this word may have different lemmas - this
   * is the canonical one (e.g. "cd" for "Cd", "CD", "cd").
   */
  public String getLemma() {
    return lemma;
  }

  /**
   * Number of "words" (aka "tokens") in this <tt>Word</tt>'s lemma.
   */
  public int getWordCount() {
    // Morphy.counts() default implementation already counts
    // space (' ') and underscore ('_') separated words
    return Morphy.countWords(lemma, '-');
  }

  /**
   * @return true if this <tt>Word</tt>'s {@code {@link #getWordCount()} > 1}.
   */
  public boolean isCollocation() {
    return getWordCount() > 1;
  }

  // little tricky to implement efficiently once we switch to ImmutableList
  // if we maintain the sometimes offets sometimes Synsets optimization because somewhat inefficient to store Integer vs int
  // still much smaller than Synset objects, and still prevents "leaks"
  //public int getSenseCount() {
  //}

  public int getTaggedSenseCount() {
    return taggedSenseCount;
  }

  /** {@inheritDoc} */
  public Iterator<WordSense> iterator() {
    return Arrays.asList(getSenses()).iterator();
  }

  public Synset[] getSynsets() {
    // careful with this.synsets
    synchronized(this) {
      if (this.synsets instanceof int[]) {
        final int[] synsetOffsets = (int[])synsets;
        // This memory optimization allows this.synsets as an int[] until this
        // method is called to avoid needing to store both the offset and synset
        // arrays.
        // TODO This might be better as a Soft or Weak reference
        final Synset[] syns = new Synset[synsetOffsets.length];
        for (int i = 0; i < synsetOffsets.length; i++) {
          syns[i] = fileBackedDictionary.getSynsetAt(getPOS(), synsetOffsets[i]);
          assert syns[i] != null : "null Synset at index "+i+" of "+this;
        }
        this.synsets = syns;
      }
      // else assert this.synsets instanceof Synset[] already
      return (Synset[])this.synsets;
    }
  }

  public WordSense[] getSenses() {
    //TODO consider caching senses - we are Iterable on it and getSense would also be much cheaper
    final WordSense[] senses = new WordSense[getSynsets().length];
    int senseNumberMinusOne = 0;
    for (final Synset synset : getSynsets()) {
      final WordSense wordSense = synset.getWordSense(this);
      senses[senseNumberMinusOne] = wordSense;
      assert senses[senseNumberMinusOne] != null :
        this+" null WordSense at senseNumberMinusOne: "+senseNumberMinusOne;
      senseNumberMinusOne++;
    }
    return senses;
  }

  /** Note, <param>senseNumber</param> is a <em>1</em>-indexed value. */
  public WordSense getSense(final int senseNumber) {
    if (senseNumber <= 0) {
      throw new IllegalArgumentException("Invalid senseNumber "+senseNumber+" requested");
    }
    final Synset[] localSynsets = getSynsets();
    if (senseNumber >= localSynsets.length) {
      throw new IllegalArgumentException(this+" only has "+localSynsets.length+" senses");
    }
    return localSynsets[senseNumber - 1].getWordSense(this);
  }

  int getOffset() {
    return offset;
  }

  //
  // Object methods
  //
  @Override
  public boolean equals(final Object that) {
    return (that instanceof Word)
      && ((Word) that).posOrdinal == posOrdinal
      && ((Word) that).offset == offset;
  }

  @Override
  public int hashCode() {
    // times 10 shifts left by 1 decimal place
    return (offset * 10) + getPOS().hashCode();
  }

  @Override
  public String toString() {
    return new StringBuilder("[Word ").
      append(offset).
      append("@").
      append(getPOS().getLabel()).
      append(": \"").
      append(getLemma()).
      append("\"]").toString();
  }

  /**
   * {@inheritDoc}
   */
  public int compareTo(final Word that) {
    // if these ' ' -> '_' replaces aren't done resulting sort will not match
    // index files.
    int result = Utils.WordNetLexicalComparator.GIVEN_CASE_INSTANCE.compare(this.getLemma(), that.getLemma());
    if (result == 0) {
      result = this.getPOS().compareTo(that.getPOS());
    }
    return result;
  }
}