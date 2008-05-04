/*
 * Copyright 1998 by Oliver Steele.  You can use this software freely so long as you preserve
 * the copyright notice and this restriction, and label your changes.
 */
package edu.brandeis.cs.steele.wn;

import java.util.*;


/** A <code>WordSense</code> represents the lexical information related to a specific sense of a {@link Word}.
 *
 * <code>WordSense</code>'s are linked by {@link Pointer}s into a network of lexically related <code>Synset</code>s
 * and <code>WordSense</code>s.
 * {@link WordSense#getTargets} retrieves the targets of these links, and
 * {@link WordSense#getPointers} retrieves the pointers themselves.
 *
 * @see Pointer
 * @see Synset
 * @see Word
 * @author Oliver Steele, steele@cs.brandeis.edu
 * @version 1.0
 */
public class WordSense implements PointerTarget {
  //
  // Adjective Position Flags
  //
  public static final int NONE = 0;
  public static final int PREDICATIVE = 1;
  public static final int ATTRIBUTIVE = 2; // synonymous with PRENOMINAL
  public static final int PRENOMINAL = 2; // synonymous with ATTRIBUTIVE
  public static final int IMMEDIATE_POSTNOMINAL = 4;

  //
  // Instance implementation
  //
  private final Synset synset;
  private final String lemma;
  private final int lexid;
  //FIXME only needs to be a byte since there are only 3 bits of flag values
  private final int flags;
  // represents up to 64 different verb frames are possible (as of now, 35 exist)
  private long verbFrameFlags;
  private short senseNumber;

  WordSense(final Synset synset, final String lemma, final int lexid, final int flags) {
    this.synset = synset;
    this.lemma = lemma;
    this.lexid = lexid;
    this.flags = flags;
    this.senseNumber = -1;
  }

  void setVerbFrameFlag(final int fnum) {
    verbFrameFlags |= 1L << (fnum - 1);
  }

  //
  // Object methods
  //
  @Override public boolean equals(Object object) {
    return (object instanceof WordSense)
      && ((WordSense) object).synset.equals(synset)
      && ((WordSense) object).lemma.equals(lemma);
  }

  @Override public int hashCode() {
    return synset.hashCode() ^ lemma.hashCode();
  }

  @Override public String toString() {
    return new StringBuilder("[WordSense ").
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

  public Iterator<WordSense> iterator() {
    return Collections.singleton(this).iterator();
  }

  String flagsToString() {
    if(flags == NONE) {
      return "NONE";
    }
    final StringBuilder flagString = new StringBuilder();
    if(0 != (PREDICATIVE & flags)) {
      flagString.append("predicative");
    }
    if(0 != (PRENOMINAL & flags)) {
      if(flagString.length() != 0) {
        flagString.append(",");
      }
      flagString.append("prenominal");
    }
    if(0 != (IMMEDIATE_POSTNOMINAL & flags)) {
      if(flagString.length() != 0) {
        flagString.append(",");
      }
      flagString.append("immediate_postnominal");
    }
    return flagString.toString();
  }


  /**
   * 1-indexed value.
   */
  public int getSenseNumber() {
    if(senseNumber < 1) {
      final FileBackedDictionary dictionary = FileBackedDictionary.getInstance();
      final Word word = dictionary.lookupWord(getPOS(), lemma);
      assert word != null : "lookupWord failed for \""+lemma+"\" "+getPOS();
      int senseNumber = 0;
      for(final Synset syn : word.getSynsets()) {
        --senseNumber;
        if(syn.equals(synset)) {
          senseNumber = -senseNumber;
          break;
        }
      }
      assert senseNumber > 0 : "Word lemma: "+lemma+" "+getPOS();
      assert senseNumber < Short.MAX_VALUE;
      this.senseNumber = (short)senseNumber;
    }
    return senseNumber;
  }

  /**
   * Build 'sensekey'.  Used for searching <tt>cntlist.rev</tt><br>
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
   * <a href="http://wordnet.princeton.edu/man/cntlist.5WN.html"><tt>cntlist</tt></a>
   */
  public int getSensesTaggedFrequency() {
    //TODO cache this value
    //TODO we could use this Word's getTaggedSenseCount() to determine if
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
      //[WordSense 9465459@[POS noun]:"unit"#5] foundSenseNumber: 7
      //assert getSenseNumber() == 
      //  CharSequenceTokenizer.parseInt(line, firstSpace + 1, lastSpace);
    }
    return count;
  }

  //FIXME publish as EnumSet (though store set as a byte for max efficiency
  long getFlags() {
    return flags;
  }

  //TODO expert only. maybe publish as EnumSet
  long getVerbFrameFlags() {
    return verbFrameFlags;
  }

  /** 
   * <p>Returns illustrative sentences and generic verb frames.  This <b>only</b> has values
   * for {@link POS#VERB} <code>WordSense</code>s.
   *
   * <p>For illustrative sentences (<tt>sents.vrb</tt>), "%s" is replaced with the verb lemma
   * which seems unnecessarily ineeficient since you have the WordSense anyway.
   *
   * <p>Official WordNet 3.0 documentation indicates "specific" and generic frames
   * are mutually exclusive which is not the case.
   *
   * @see <a href="http://wordnet.princeton.edu/man/wndb.5WN#sect6">http://wordnet.princeton.edu/man/wndb.5WN#sect6</a>
   */
  public List<String> getVerbFrames() {
    if(getPOS() != POS.VERB) {
      return Collections.emptyList();
    }
    final String senseKey = getSenseKey();
    final FileBackedDictionary dictionary = FileBackedDictionary.getInstance();
    final String sentenceNumbers = dictionary.lookupVerbSentencesNumbers(senseKey);
    List<String> frames = Collections.emptyList();
    if(sentenceNumbers != null) {
      frames = new ArrayList<String>();
      // fetch the illustrative sentences indicated in sentenceNumbers
      //TODO consider substibuting in lemma for "%s" in each
      //FIXME this logic is a bit too complex/duplicated!!
      int s = 0;
      int e = sentenceNumbers.indexOf(",");
      final int n = sentenceNumbers.length();
      e = e > 0 ? e : n;
      for( ; s < n;
        // e = next comma OR if no more commas, e = n
        s = e + 1, e = sentenceNumbers.indexOf(",", s), e = e > 0 ? e : n) {
        final String sentNum = sentenceNumbers.substring(s, e);
        final String sentence = dictionary.lookupVerbSentence(sentNum);
        assert sentence != null;
        frames.add(sentence);
      }
    } else {
      //assert verbFrameFlags == 0L : "not mutually exclusive for "+this;
    }
    if(verbFrameFlags != 0L) {
      final int numGenericFrames = Long.bitCount(verbFrameFlags);
      if(frames.isEmpty()) {
        frames = new ArrayList<String>();
      } else {
        ((ArrayList)frames).ensureCapacity(frames.size() + numGenericFrames);
      }

      // fetch any generic verb frames indicated by verbFrameFlags
      // numberOfLeadingZeros (leftmost), numberOfTrailingZeros (rightmost)
      // 001111111111100
      //  ^-lead      ^-trail
      // simple scan between these (inclusive) should cover rest
      for(int fn = Long.numberOfTrailingZeros(verbFrameFlags),
          lfn = Long.SIZE - Long.numberOfLeadingZeros(verbFrameFlags);
          fn < lfn;
          fn++) {
        if((verbFrameFlags & (1L << fn)) != 0L) {
          final String frame = dictionary.lookupGenericFrame(fn + 1);
          assert frame != null : 
            "this: "+this+" fn: "+fn+
            " shift: "+((1L << fn)+
            " verbFrameFlags: "+Long.toBinaryString(verbFrameFlags))+
            " verbFrameFlags: "+verbFrameFlags;
          frames.add(frame);
        }
      }
    }
    return frames;
  }

  public String getDescription() {
    if (getPOS() != POS.ADJ && getPOS() != POS.SAT_ADJ) {
      return lemma;
    }
    final StringBuilder description = new StringBuilder(lemma);
    if (flags != 0) {
      description.append("(");
      description.append(flagsToString());
      description.append(")");
    }
    final PointerTarget[] targets = getTargets(PointerType.ANTONYM);
    if (targets.length > 0) {
      // adj acidic has more than 1 antonym (alkaline and amphoteric)
      for (final PointerTarget target : targets) {
        description.append(" (vs. ");
        final WordSense antonym = (WordSense)target;
        description.append(antonym.getLemma());
        description.append(")");
      }
    }
    return description.toString();
  }

  public String getLongDescription() {
    final StringBuilder buffer = new StringBuilder();
    //buffer.append(getSenseNumber());
    //buffer.append(". ");
    //final int sensesTaggedFrequency = getSensesTaggedFrequency();
    //if(sensesTaggedFrequency != 0) {
    //  buffer.append("(");
    //  buffer.append(sensesTaggedFrequency);
    //  buffer.append(") ");
    //}
    buffer.append(getLemma());
    if(flags != 0) {
      buffer.append("(");
      buffer.append(flagsToString());
      buffer.append(")");
    }
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
      if (pointer.getSource().equals(this)) {
        assert pointer.getSource() == this;
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
    //TODO could be a little more efficient (no need for intermediate Pointer[])
    return restrictPointers(synset.getPointers(type));
  }

  public PointerTarget[] getTargets() {
    return Synset.collectTargets(getPointers());
  }

  public PointerTarget[] getTargets(final PointerType type) {
    //TODO could be a little more efficient (no need for intermediate Pointer[])
    return Synset.collectTargets(getPointers(type));
  }
}
