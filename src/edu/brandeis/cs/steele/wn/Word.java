/*
 * WordNet-Java
 *
 * Copyright 1998 by Oliver Steele.  You can use this software freely so long as you preserve
 * the copyright notice and this restriction, and label your changes.
 */
package edu.brandeis.cs.steele.wn;

import java.util.logging.*;
import java.util.EnumSet;
import java.util.Set;

/**
 * An <code>Word</code> represents a line of the <code>index.<em>pos</em></code> file.
 * An <code>Word</code> is created retrieved or retrieved via {@link DictionaryDatabase#lookupWord},
 * and has a <i>lemma</i>, a <i>pos</i>, and a set of <i>senses</i>, which are of type {@link Synset}.
 *
 * @author Oliver Steele, steele@cs.brandeis.edu
 * @version 1.0
 */
public class Word {
  private static final Logger log = Logger.getLogger(Word.class.getName());
  
  /** offset in <var>pos</var><code>.index</code> file */
  private final int offset;
  /** No case "lemma". Each {@link WordSense} has at least 1 true case lemma
   * (could vary by POS). 
   */
  private final String lemma; 
  // number of senses with counts in sense tagged corpora
  private final int taggedSenseCount;
  // senses are initially stored as offsets, and paged in on demand.
  private int[] synsetOffsets;
  /** This is <code>null</code> until {@link #getSynsets()} has been called. */
  private Synset[] synsets;

  private EnumSet<PointerType> ptrTypes;
  private final byte posOrdinal;
  //
  // Constructor
  //
  Word(final CharSequence line, final int offset) {
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
        tokenizer.skipNextToken(); // a pointertype (maybe incorrect - see getPointerTypes() comments
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
      this.synsetOffsets = new int[senseCount];
      for (int i = 0; i < senseCount; i++) {
        synsetOffsets[i] = tokenizer.nextInt();
      }
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
  // Object methods
  //
  @Override public boolean equals(final Object object) {
    return (object instanceof Word)
      && ((Word) object).posOrdinal == posOrdinal
      && ((Word) object).offset == offset;
  }

  @Override public int hashCode() {
    // times 10 shifts left by 1 decimal place
    return ((int) offset * 10) + getPOS().hashCode();
  }

  @Override public String toString() {
    return new StringBuilder("[Word ").
      append(offset).
      append("@").
      append(getPOS().getLabel()).
      append(": \"").
      append(getLemma()).
      append("\"]").toString();
  }

  //
  // Accessors
  //
  public POS getPOS() {
    return POS.fromOrdinal(posOrdinal);
  }

  /**
   * The pointer types available for this indexed word.  May not apply to all
   * senses of the word.
   */
  public EnumSet<PointerType> getPointerTypes() {
    if(ptrTypes == null) {
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

  /** Return the word's lowercased <i>lemma</i>.  Its lemma is its orthographic
   * representation, for example <code>"dog"</code> or <code>"get up"</code>
   * or <code>"u.s."</code>.
   */
  public String getLemma() {
    return lemma; 
  }

  public int getTaggedSenseCount() {
    return taggedSenseCount;
  }

  public Synset[] getSynsets() {
    if (synsets == null) {
      final FileBackedDictionary dictionary = FileBackedDictionary.getInstance();
      //XXX could synsets be a WeakReference ?
      final Synset[] syns = new Synset[synsetOffsets.length];
      for (int i = 0; i < synsetOffsets.length; i++) {
        syns[i] = dictionary.getSynsetAt(getPOS(), synsetOffsets[i]);
        assert syns[i] != null : "null Synset at index "+i+" of "+this;
      }
      synsets = syns;
    }
    return synsets;
  }
  
  public WordSense[] getSenses() {
    final WordSense[] senses = new WordSense[getSynsets().length];
    int senseNumberMinusOne = 0;
    for(final Synset synset : getSynsets()) {
      final WordSense wordSense = synset.getWordSense(this);
      senses[senseNumberMinusOne] = wordSense;
      assert senses[senseNumberMinusOne] != null : 
        this+" null WordSense at senseNumberMinusOne: "+senseNumberMinusOne;
      senseNumberMinusOne++;
    }
    return senses;
  }

  /** Note, <param>senseNumber</param> is a 1-indexed value. */
  public WordSense getSense(int senseNumber) {
    if(senseNumber <= 0) {
      return null;
    }
    final WordSense[] senses = getSenses();
    if(senseNumber > senses.length) {
      return null;
    }
    return senses[senseNumber - 1];
  }

  int getOffset() {
    return offset;
  }
}
