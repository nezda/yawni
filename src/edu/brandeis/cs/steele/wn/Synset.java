/*
 * WordNet-Java
 *
 * Copyright 1998 by Oliver Steele.  You can use this software freely so long as you preserve
 * the copyright notice and this restriction, and label your changes.
 */
package edu.brandeis.cs.steele.wn;
import java.io.*;
import java.util.*;
import java.util.logging.*;

/** A <code>Synset</code>, or <b>syn</b>onym <b>set</b>, represents a line of a WordNet <var>pos</var><code>.data</code> file.
 * A <code>Synset</code> represents a concept, and contains a set of <code>WordSense</code>s, each of which has a sense
 * that names that concept (and each of which is therefore synonymous with the other wordSenses in the
 * <code>Synset</code>).
 *
 * <code>Synset</code>'s are linked by {@link Pointer}s into a network of related concepts; this is the <i>Net</i>
 * in WordNet.  {@link Synset#getTargets} retrieves the targets of these links, and
 * {@link Synset#getPointers} retrieves the pointers themselves.
 *
 * @see WordSense
 * @see Pointer
 * @author Oliver Steele, steele@cs.brandeis.edu
 * @version 1.0
 */
public class Synset implements PointerTarget {
  private static final Logger log = Logger.getLogger(Synset.class.getName());
  // 
  // Instance implementation
  // 
  /** offset in <code>data.</code><var>pos</var> file */
  private final int offset;
  private final WordSense[] wordSenses;
  private final Pointer[] pointers;
  //TODO make this a byte[]
  private final char[] gloss;
  private final boolean isAdjectiveCluster;
  private final byte posOrdinal;
  private final byte lexfilenum;

  //
  // Constructor
  //
  @SuppressWarnings("deprecation") // using Character.isSpace() for file compat
  Synset(final String line) {
    final CharSequenceTokenizer tokenizer = new CharSequenceTokenizer(line, " ");
    this.offset = tokenizer.nextInt();
    final int lexfilenumInt = tokenizer.nextInt(); 
    // there are currently only 45 lexfiles
    // http://wordnet.princeton.edu/man/lexnames.5WN
    assert lexfilenumInt < 45 : "lexfilenumInt: "+lexfilenumInt;
    this.lexfilenum = (byte)lexfilenumInt;
    CharSequence ss_type = tokenizer.nextToken();
    if ("s".contentEquals(ss_type)) {
      ss_type = "a";
      this.isAdjectiveCluster = true;
    } else {
      this.isAdjectiveCluster = false;
    }
    this.posOrdinal = (byte) POS.lookup(ss_type).ordinal();

    final int wordCount = tokenizer.nextHexInt();
    this.wordSenses = new WordSense[wordCount];
    for (int i = 0; i < wordCount; i++) {
      String lemma = tokenizer.nextToken().toString();
      final String originalLemma = lemma;
      final int lexid = tokenizer.nextHexInt();
      int flags = WordSense.NONE;
      // strip the syntactic marker, e.g. "(a)" || "(ip)" || ...
      if (lemma.charAt(lemma.length() - 1) == ')' && lemma.indexOf('(') > 0) {
        final int lparenIdx = lemma.indexOf('(');
        final int rparenIdx = lemma.length() - 1;
        assert ')' == lemma.charAt(rparenIdx);
        final String marker = lemma.substring(lparenIdx + 1, rparenIdx);
        lemma = lemma.substring(0, lparenIdx);
        if (marker.equals("p")) {
          flags |= WordSense.PREDICATIVE;
        } else if (marker.equals("a")) {
          flags |= WordSense.ATTRIBUTIVE;
        } else if (marker.equals("ip")) {
          flags |= WordSense.IMMEDIATE_POSTNOMINAL;
        } else {
          throw new RuntimeException("unknown syntactic marker " + marker);
        }
      }
      wordSenses[i] = new WordSense(this, lemma.replace('_', ' '), lexid, flags);
    }

    final int pointerCount = tokenizer.nextInt();
    this.pointers = new Pointer[pointerCount];
    for (int i = 0; i < pointerCount; i++) {
      pointers[i] = new Pointer(this, i, tokenizer);
    }

    if (posOrdinal == POS.VERB.ordinal()) {
      final int f_cnt = tokenizer.nextInt();
      for (int i = 0; i < f_cnt; i++) {
        final CharSequence skip = tokenizer.nextToken(); // "+"
        assert "+".contentEquals(skip) : "skip: "+skip;
        //FIXME what is f_num?
        final int f_num = tokenizer.nextInt();
        //LN guess int f_num = tokenizer.nextHexInt();
        //FIXME what is w_num?
        final int w_num = tokenizer.nextHexInt();
        if (w_num > 0) {
          wordSenses[w_num - 1].setVerbFrameFlag(f_num);
        } else {
          for (int j = 0; j < wordSenses.length; j++) {
            wordSenses[j].setVerbFrameFlag(f_num);
          }
        }
      }
    }

    final int index = line.indexOf('|');
    if (index > 0) {
      // jump '|' and immediately following ' '
      assert line.charAt(index + 1) == ' ';
      int incEnd = line.length() - 1;
      for(int i = incEnd; i >= 0; i--) {
        if(Character.isSpace(line.charAt(i)) == false) {
          incEnd = i;
          break;
        }
      }
      final int finalLen = (incEnd + 1) - (index + 2);
      this.gloss = new char[finalLen];
      assert gloss.length == finalLen: "gloss.length: "+gloss.length+" finalLen: "+finalLen;
      line.getChars(index + 2, incEnd + 1, gloss, 0);
    } else {
      log.log(Level.SEVERE, "Synset has no gloss?:\n" + line);
      this.gloss = null;
    }
  }

  //
  // Object methods
  //
  @Override public boolean equals(Object object) {
    return (object instanceof Synset)
      && ((Synset) object).posOrdinal == posOrdinal
      && ((Synset) object).offset == offset;
  }

  @Override public int hashCode() {
    // times 10 shifts right by 1 decimal place
    return ((int) offset * 10) + getPOS().hashCode();
  }

  @Override public String toString() {
    return new StringBuilder("[Synset ").
      append(offset).
      append("@").
      append(getPOS()).
      append("<").
      append("#").
      append(lexfilenum()).
      append("::").
      append(getLexCategory()).
      append(">").
      append(": \"").
      append(getDescription()).
      append("\"]").toString();
  }


  //
  // Accessors
  //
  public POS getPOS() {
    return POS.fromOrdinal(posOrdinal);
  }

  boolean isAdjectiveCluster() {
    return isAdjectiveCluster;
  }
  
  int lexfilenum() {
    return lexfilenum;
  }

  /** XXX DOCUMENT ME */
  public String getLexCategory() {
    final FileBackedDictionary dictionary = FileBackedDictionary.getInstance();
    return dictionary.lookupLexCategory(lexfilenum());
  }

  public String getGloss() {
    return new String(gloss);
  }

  public WordSense[] getWords() {
    return wordSenses;
  }

  /** XXX DOCUMENT ME */
  public WordSense getWord(final IndexWord indexWord) {
    for(final WordSense wordSense : wordSenses) {
      if(wordSense.getLemma().equalsIgnoreCase(indexWord.getLemma())) {
        return wordSense;
      }
    }
    return null;
  }

  public Iterator<WordSense> iterator() {
    return Arrays.asList(wordSenses).iterator();
  }

  int getOffset() {
    return offset;
  }
  
  WordSense getWord(final int index) {
    return wordSenses[index];
  }

  //
  // Description
  //
  
  public String getDescription() {
    return getDescription(false);
  }

  public String getDescription(final boolean verbose) {
    final StringBuilder buffer = new StringBuilder();
    buffer.append("{");
    for (int i = 0; i < wordSenses.length; i++) {
      if (i > 0) {
        buffer.append(", ");
      }
      if(verbose) {
        buffer.append(wordSenses[i].getDescription());
      } else {
        buffer.append(wordSenses[i].getLemma());
      }
    }
    buffer.append("}");
    return buffer.toString();
  }

  public String getLongDescription() {
    return getLongDescription(false);
  }

  public String getLongDescription(final boolean verbose) {
    final StringBuilder description = new StringBuilder(this.getDescription(verbose));
    final String gloss = this.getGloss();
    if (gloss != null) {
      description.
        append(" -- (").
        append(gloss).
        append(")");
    }
    return description.toString();
  }


  //
  // Pointers
  //
  static PointerTarget[] collectTargets(final Pointer[] pointers) {
    final PointerTarget[] targets = new PointerTarget[pointers.length];
    for (int i = 0; i < pointers.length; i++) {
      targets[i] = pointers[i].getTarget();
    }
    return targets;
  }

  public Pointer[] getPointers() {
    return pointers;
  }
  
  private static final Pointer[] NO_POINTERS = new Pointer[0];
  
  public Pointer[] getPointers(final PointerType type) {
    List<Pointer> vector = null;
    for (final Pointer pointer : pointers) {
      if (pointer.getType() == type) {
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

  public PointerTarget[] getTargets() {
    return collectTargets(getPointers());
  }

  public PointerTarget[] getTargets(final PointerType type) {
    //TODO could be a little more efficient (no need for intermediate Pointer[])
    return collectTargets(getPointers(type));
  }

  /** @see PointerTarget */
  public Synset getSynset() { 
    return this; 
  }
}
