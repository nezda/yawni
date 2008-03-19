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
  private final Synset synset;
  private final String lemma;
  private final int lexid;
  //FIXME only needs to be a byte since there are only 3 bits of flag values
  private final int flags;
  //TODO what are these ? and can this be an int or byte?
  private long verbFrameFlags;
  private short senseNumber;

  Word(final Synset synset, final String lemma, final int lexid, final int flags) {
    this.synset = synset;
    this.lemma = lemma;
    this.lexid = lexid;
    this.flags = flags;
    this.senseNumber = -1;
  }

  void setVerbFrameFlag(final int fnum) {
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
      append(synset.getOffset()).
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

  public String getLemma() {
    return lemma;
  }

  /**
   * 1-indexed value.
   */
  public int getSenseNumber() {
    if(senseNumber < 1) {
      final FileBackedDictionary dictionary = FileBackedDictionary.getInstance();
      final IndexWord indexWord = dictionary.lookupIndexWord(getPOS(), lemma);
      assert indexWord != null : "lookupIndexWord failed for \""+lemma+"\" "+getPOS();
      int senseNumber = 0;
      for(final Synset syn : indexWord.getSynsets()) {
        --senseNumber;
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

  /**
   * Build 'sensekey'.  Used for searching cntlist.rev<br>
   * <a href="http://wordnet.princeton.edu/man/senseidx.5WN#sect3">senseidx WordNet documentation</a>
   */
  String getSenseKey() {
    final String searchWord;
    final int headSense;
    if(synset.isAdjectiveCluster()) {
      final PointerTarget[] adjsses = synset.getTargets(PointerType.SIMILAR_TO);
      assert adjsses.length == 1;
      final Synset adjss = (Synset)adjsses[0];
      // if satellite, key lemma in cntlist.rev
      // is adjss's first word  (no case) and
      // adjss's lexid (aka lexfilenum) otherwise
      searchWord = adjss.getWords()[0].getLemma();
      headSense = adjss.getWords()[0].lexid;
    } else {
      searchWord = getLemma();
      headSense = lexid;
    }
    int synsetIndex;
    for (synsetIndex = 0; synsetIndex < getSynset().getWords().length; synsetIndex++) {
      if(getSynset().getWords()[synsetIndex].getLemma() == getLemma()) {
        break;
      }
    }
    assert synsetIndex != getSynset().getWords().length;
    final String senseKey;
    if (synset.isAdjectiveCluster()) {
      senseKey = String.format("%s%%%d:%02d:%02d:%s:%02d",
          getLemma().toLowerCase(),
          POS.SAT_ADJ.getWordNetCode(), 
          getSynset().lexfilenum(),
          getSynset().getWords()[synsetIndex].lexid,
          searchWord.toLowerCase(),
          headSense);
    } else {
      senseKey = String.format("%s%%%d:%02d:%02d::",
          getLemma().toLowerCase(), 
          getPOS().getWordNetCode(),
          getSynset().lexfilenum(),
          getSynset().getWords()[synsetIndex].lexid
          );
    }
    return senseKey;
  }

  /** 
   * <a href="http://wordnet.princeton.edu/man/cntlist.5WN.html">cntlist</a>
   */
  public int getSensesTaggedFrequency() {
    //TODO cache this value
    //TODO we could use this IndexWord's getTaggedSenseCount() to determine if
    //there were any tagged senses for *any* sense of it (including this one)
    //and really we wouldn't need to look at sense (numbers) exceeding that value
    //as an optimization
    final String senseKey = getSenseKey();
    final FileBackedDictionary dictionary = FileBackedDictionary.getInstance();
    final String line = dictionary.lookupCntlistDotRevLine(senseKey);
    int count = 0;
    if(line != null) {
      // cntlist.rev line format:
      // <sense_key>  <sense_number>  tag_cnt
      final int lastSpace = line.lastIndexOf(" ");
      assert lastSpace > 0;
      count = CharSequenceTokenizer.parseInt(line, lastSpace + 1, line.length());
      // sanity check final int firstSpace = line.indexOf(" ");
      // sanity check assert firstSpace > 0 && firstSpace != lastSpace;
      // sanity check final int mySenseNumber = getSenseNumber();
      // sanity check final int foundSenseNumber =
      // sanity check   CharSequenceTokenizer.parseInt(line, firstSpace + 1, lastSpace);
      // sanity check if(mySenseNumber != foundSenseNumber) {
      // sanity check   System.err.println(this+" foundSenseNumber: "+foundSenseNumber+" count: "+count);
      // sanity check } else {
      // sanity check   //System.err.println(this+" OK");
      // sanity check }
      //[Word 9465459@[POS noun]:"unit"#5] foundSenseNumber: 7
      //assert getSenseNumber() == 
      //  CharSequenceTokenizer.parseInt(line, firstSpace + 1, lastSpace);
    }
    return count;
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
    final StringBuilder buffer = new StringBuilder();
    buffer.append(getSenseNumber());
    buffer.append(". ");
    final int sensesTaggedFrequency = getSensesTaggedFrequency();
    if(sensesTaggedFrequency != 0) {
      buffer.append("(");
      buffer.append(sensesTaggedFrequency);
      buffer.append(") ");
    }
    buffer.append(getLemma());
    final String gloss = getSynset().getGloss();
    if (gloss != null) {
      buffer.append(" -- (");
      buffer.append(gloss);
      buffer.append(")");
    }
    return buffer.toString();
  }

  //
  // Pointers
  //
  private Pointer[] restrictPointers(final Pointer[] source) {
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
  
  private static final Pointer[] NO_POINTERS = new Pointer[0];

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
