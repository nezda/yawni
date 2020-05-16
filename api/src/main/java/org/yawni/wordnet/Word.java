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

//import java.lang.ref.SoftReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.EnumSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.yawni.util.CharSequenceTokenizer;
import org.yawni.util.LightImmutableList;
import com.google.common.primitives.SignedBytes;

/**
 * A {@code Word} represents a line of a WordNet <code>index.<em>pos</em></code> file (e.g., {@code index.noun}).
 * A {@code Word} is retrieved via {@link WordNetInterface#lookupWord(CharSequence, POS)},
 * and has a lowercase <em>lemma</em>, a <em>part of speech ({@link POS})</em>, and a set of <em>senses</em> ({@link WordSense}s).
 *
 * <p> This class used to be called {@code IndexWord} which arguably makes more sense from the
 * WordNet perspective.
 *
 * @see Synset
 * @see WordSense
 * @see Relation
 */
public final class Word implements Comparable<Word>, Iterable<WordSense> {
  private static final Logger log = LoggerFactory.getLogger(Word.class);

  private final WordNet wordNet;
  /** offset in <code>index.<em>pos</em></code> file; {@code Index.idxoffset} in {@code wn.h} */
  private final int offset;
  /**
   * Lowercase form of lemma. Each {@link WordSense} has at least 1 true case lemma
   * (could vary by POS). {@link Word}s have exactly 1 lowercase lemma.
   */
  private final String lowerCasedLemma;
  // number of senses with counts in sense tagged corpora
  private final int taggedSenseCount;
  /**
   * Synsets are initially stored as offsets, and paged in on demand
   * of the first call of {@link #getSynsets()}.
   */
  private Object synsets;
//  private SoftReference<List<WordSense>> senses;

  private Set<RelationType> relationTypes;
  private final byte posOrdinal;

  //
  // Constructor
  //
  Word(final CharSequence line, final int offset, final WordNet wordNet) {
    this.wordNet = wordNet;
    try {
      log.trace("parsing line: {}", line);
      final CharSequenceTokenizer tokenizer = new CharSequenceTokenizer(line, " ");
      this.lowerCasedLemma = tokenizer.nextToken().replace('_', ' ');
      this.posOrdinal = SignedBytes.checkedCast(POS.lookup(tokenizer.nextToken()).ordinal());
      this.offset = offset;

      tokenizer.skipNextToken(); // poly_cnt
      //final int poly_cnt = tokenizer.nextInt(); // poly_cnt
      final int relationCount = tokenizer.nextInt();
      //this.relationTypes = EnumSet.noneOf(RelationType.class);
      for (int i = 0; i < relationCount; i++) {
        //XXX each of these tokens is a relationtype, although it may be may be
        //incorrect - see getRelationTypes() comments)
        tokenizer.skipNextToken();
        //  try {
        //    relationtypes.add(RelationType.parseKey(tokenizer.nextToken()));
        //  } catch (final java.util.NoSuchElementException exc) {
        //    log.log(Level.SEVERE, "Word() got RelationType.parseKey() error:", exc);
        //  }
      }

      final int senseCount = tokenizer.nextInt();
      // this is redundant information
      //assert senseCount == poly_cnt;
      this.taggedSenseCount = tokenizer.nextInt();
      final int[] synsetOffsets = new int[senseCount];
      for (int i = 0; i < senseCount; i++) {
        synsetOffsets[i] = tokenizer.nextInt();
      }
      this.synsets = synsetOffsets;
      //final EnumSet<RelationType> actualRelationTypes = EnumSet.noneOf(RelationType.class);
      //for (final Synset synset : getSynsets()) {
      //  for (final Relation relation : synset.getRelations()) {
      //    final RelationType relationType = relation.getType();
      //    actualRelationTypes.add(relationType);
      //  }
      //}
      //// in actualRelationTypes, NOT relationTypes
      //final EnumSet<RelationType> missing = EnumSet.copyOf(actualRelationTypes); missing.removeAll(relationTypes);
      //// in relationTypes, NOT actualRelationTypes
      //final EnumSet<RelationType> extra = EnumSet.copyOf(relationTypes); extra.removeAll(actualRelationTypes);
      //if(! missing.isEmpty()) {
      //  //log.error("missing: {}", missing);
      //}
      //if(! extra.isEmpty()) {
      //  //log.error("extra: {}", extra);
      //}
//      senses = new SoftReference<List<WordSense>>(null);
    } catch (final RuntimeException e) {
      log.error("Word parse error on offset: {} line:\n\"{}\"", offset, line);
      log.error("",  e);
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
   * The {@link RelationType}s of {@link Relation}s which involve this {@code Word}; includes {@link LexicalRelation}s
   * which involve {@code WordSenses}s of this {@code Word} and all {@link SemanticRelation}s for all {@link Synset}s
   * which involve senses of this {@code Word}.
   */
  public Set<RelationType> getRelationTypes() {
    if (relationTypes == null) {
      final EnumSet<RelationType> localRelationTypes = EnumSet.noneOf(RelationType.class);
      for (final WordSense sense : getWordSenses()) {
        for (final Relation relation : sense.getRelations()) {
          final RelationType relationType = relation.getType();
          localRelationTypes.add(relationType);
        }
      }
      this.relationTypes = Collections.unmodifiableSet(localRelationTypes);
    }
    return relationTypes;
  }

  /**
   * Returns this {@code Word}'s <em>lowercased</em> lemma.  Its 'lemma' is its orthographic
   * representation, for example "<tt>dog</tt>" or "<tt>get up</tt>"
   * or "<tt>u.s.a.</tt>".
   *
   * <p> Note that different senses of this word may have different lemmas - this
   * is the canonical one (e.g., "cd" for "Cd", "CD", "cd").
   */
  public String getLowercasedLemma() {
    return lowerCasedLemma;
  }

  /**
   * Number of "words" (aka "tokens") in this {@code Word}'s lemma.
   */
  public int getWordCount() {
    // Morphy.counts() default implementation already counts
    // space (' ') and underscore ('_') separated words
    return Morphy.countWords(lowerCasedLemma, '-');
  }

  /**
   * @return true if this {@code Word}'s {@link #getWordCount()} &gt; 1}.
   */
  public boolean isCollocation() {
    return getWordCount() > 1;
  }

  // little tricky to implement efficiently once we switch to LightImmutableList
  // if we maintain the sometimes-offets/sometimes-Synsets optimization because somewhat inefficient to store Integer vs int
  // still much smaller than Synset objects, and still prevents "leaks"
//  public int getSenseCount() {
//  }

  public int getTaggedSenseCount() {
    return taggedSenseCount;
  }

  @Override
  public Iterator<WordSense> iterator() {
    return getWordSenses().iterator();
  }

  /**
   * All synsets which include senses of this word.
   * @return all synsets which include senses of this word.
   */
  public List<Synset> getSynsets() {
    // careful with this.synsets
    synchronized (this) {
      if (this.synsets instanceof int[]) {
        final int[] synsetOffsets = (int[])synsets;
        // This memory optimization allows this.synsets as an int[] until this
        // method is called to avoid needing to store both the offset and synset
        // arrays.
        // TODO This might be better as a Soft or Weak -Reference
        final Synset[] syns = new Synset[synsetOffsets.length];
        for (int i = 0; i < synsetOffsets.length; i++) {
          syns[i] = wordNet.getSynsetAt(getPOS(), synsetOffsets[i]);
          assert syns[i] != null : "null Synset at index "+i+" of "+this;
        }
        this.synsets = LightImmutableList.of(syns);
      }
      // else assert this.synsets instanceof List<Synset> already
      @SuppressWarnings("unchecked")
      final List<Synset> toReturn = (List<Synset>)this.synsets;
      return toReturn;
    }
  }

  /**
   * All {@code WordSense}s of this {@code Word}.
   * @return All {@code WordSense}s of this {@code Word}.
   */
  public List<WordSense> getWordSenses() {
    // caching senses since we are Iterable on it; should make getSense cheaper too
//    List<WordSense> toReturn = senses.get();
//    if (toReturn == null) {
      final WordSense[] sensesArray = new WordSense[getSynsets().size()];
      int senseNumberMinusOne = 0;
      for (final Synset synset : getSynsets()) {
        final WordSense wordSense = synset.getWordSense(this);
        sensesArray[senseNumberMinusOne] = wordSense;
        assert sensesArray[senseNumberMinusOne] != null :
          this + " null WordSense at senseNumberMinusOne: " + senseNumberMinusOne;
        senseNumberMinusOne++;
      }
      return LightImmutableList.of(sensesArray);
//      toReturn = LightImmutableList.of(sensesArray);
//      senses = new SoftReference<List<WordSense>>(toReturn);
//    }
//    return toReturn;
  }

  /**
   * A specific sense of this word - note that {@code senseNumber} is a <em>1</em>-indexed value.
   */
  public WordSense getSense(final int senseNumber) {
    if (senseNumber <= 0) {
      throw new IllegalArgumentException("Invalid senseNumber "+senseNumber+" requested");
    }
    final List<Synset> localSynsets = getSynsets();
    if (senseNumber > localSynsets.size()) {
      throw new IllegalArgumentException(this + " only has "+simplePluralizer(localSynsets.size(), "sense"));
    }
    return localSynsets.get(senseNumber - 1).getWordSense(this);
  }

  private static String simplePluralizer(int count, final String itemWord) {
    assert count >= 1;
    if (count == 1) {
      return count + " " + itemWord;
    } else {
      return count + " " + itemWord + 's';
    }
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
      && ((Word) that).getOffset() == getOffset();
  }

  @Override
  public int hashCode() {
    // times 10 shifts left by 1 decimal place
    return (getOffset() * 10) + getPOS().hashCode();
  }

  @Override
  public String toString() {
    return new StringBuilder("[Word ").
      append(getOffset()).
      append('@').
      append(getPOS().getLabel()).
      append(": \"").
      append(getLowercasedLemma()).
      append("\"]").toString();
  }

  @Override
  public int compareTo(final Word that) {
    // if these ' ' → '_' replaces aren't done resulting sort will not match
    // index files.
    int result = WordNetLexicalComparator.GIVEN_CASE_INSTANCE.compare(this.getLowercasedLemma(), that.getLowercasedLemma());
    if (result == 0) {
      result = this.getPOS().compareTo(that.getPOS());
    }
    return result;
  }
}