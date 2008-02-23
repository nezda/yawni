/*
 * WordNet-Java
 *
 * Copyright 1998 by Oliver Steele.  You can use this software freely so long as you preserve
 * the copyright notice and this restriction, and label your changes.
 */
package edu.brandeis.cs.steele.wn;

import java.util.*;


/** A <code>Word</code> represents the lexical information related to a specific sense of an <code>IndexWord</code>.
 *
 * <code>Word</code>'s are linked by {@link Pointer}s into a network of lexically related Words.
 * {@link Word#getTargets} retrieves the targets of these links, and
 * {@link Word#getPointers} retrieves the pointers themselves.
 *
 * @see Pointer
 * @see Synset
 * @author Oliver Steele, steele@cs.brandeis.edu
 * @version 1.0
 */
public class Word implements PointerTarget {
  //
  // Adjective Position Flags
  //
  public static final int NONE = 0;
  public static final int PREDICATIVE = 1;
  public static final int ATTRIBUTIVE = 2;
  public static final int IMMEDIATE_POSTNOMINAL = 4;

  //
  // Instance implementation
  //
  protected final Synset synset;
  protected final String lemma;
  protected final int flags;
  protected long verbFrameFlags;
  protected short senseNumber;

  Word(final Synset synset, final String lemma, final int flags) {
    this.synset = synset;
    this.lemma = lemma;
    this.flags = flags;
    this.senseNumber = -1;
  }

  void setVerbFrameFlag(int fnum) {
    verbFrameFlags |= 1 << fnum;
  }

  //
  // Object methods
  //
  @Override public boolean equals(Object object) {
    return (object instanceof Word)
      && ((Word) object).synset.equals(synset)
      && ((Word) object).lemma.equals(lemma);
  }

  @Override public int hashCode() {
    return synset.hashCode() ^ lemma.hashCode();
  }

  @Override public String toString() {
    return new StringBuilder("[Word ").
      append(synset.offset).
      append("@").
      append(synset.getPOS()).
      append(":\"").
      append(getLemma()).
      append("\"#").
      append(getSenseNumber()).
      append("]").toString();
  }


  //
  // Accessors
  //
  public Synset getSynset() {
    return synset;
  }

  public POS getPOS() {
    return synset.getPOS();
  }

  public int getSenseNumber() {
    if(senseNumber < 1) {
      final FileBackedDictionary dictionary = FileBackedDictionary.getInstance();
      final IndexWord indexWord = dictionary.lookupIndexWord(getPOS(), lemma);
      assert indexWord != null : "lookupIndexWord failed for \""+lemma+"\" "+getPOS();
      int senseNumber = 0;
      for(final Synset syn : indexWord.getSynsets()) {
        --senseNumber;
        //XXX LN figure out why syn==synset won't work here
        if(syn.equals(synset)) {
          senseNumber = -senseNumber;
          break;
        }
      }
      assert senseNumber > 0 : "IndexWord lemma: "+lemma+" "+getPOS();
      assert senseNumber < Short.MAX_VALUE;
      this.senseNumber = (short)senseNumber;
    }
    return senseNumber;
  }

  public String getLemma() {
    return lemma;
  }

  public long getFlags() {
    return flags;
  }

  public long getVerbFrameFlags() {
    return verbFrameFlags;
  }

  public String getDescription() {
    return lemma;
  }

  public String getLongDescription() {
    String description = getDescription();
    String gloss = synset.getGloss();
    if (gloss != null) {
      description += " -- (" + gloss + ")";
    }
    return description;
  }

  private static final Pointer[] NO_POINTERS = new Pointer[0];
  
  //
  // Pointers
  //
  protected Pointer[] restrictPointers(final Pointer[] source) {
    List<Pointer> vector = null;
    for (int i = 0; i < source.length; ++i) {
      final Pointer pointer = source[i];
      if (pointer.getSource() == this) {
        if(vector == null) {
          vector = new ArrayList<Pointer>();
        }
        vector.add(pointer);
      }
    }
    if(vector == null) {
      return NO_POINTERS;
    }
    return vector.toArray(new Pointer[vector.size()]);
  }

  public Pointer[] getPointers() {
    return restrictPointers(synset.getPointers());
  }

  public Pointer[] getPointers(final PointerType type) {
    return restrictPointers(synset.getPointers(type));
  }

  public PointerTarget[] getTargets() {
    return Synset.collectTargets(getPointers());
  }

  public PointerTarget[] getTargets(final PointerType type) {
    return Synset.collectTargets(getPointers(type));
  }
}
