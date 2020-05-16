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

import com.google.common.base.Objects;
import java.util.ArrayList;
import com.google.common.collect.Lists;
import com.google.common.primitives.SignedBytes;
import org.yawni.util.EnumAliases;
import java.util.Iterator;
import java.util.List;

import org.yawni.util.CharSequences;
import org.yawni.util.LightImmutableList;
import static org.yawni.util.Utils.add;

/**
 * A {@code WordSense} represents the precise lexical information related to a specific sense of a {@link Word}.
 *
 * <p> {@code WordSense}s are linked by {@link Relation}s into a network of lexically related {@link Synset}s
 * and {@code WordSense}s.
 * {@link WordSense#getRelationTargets WordSense.getRelationTargets()} retrieves the targets of these links, and
 * {@link WordSense#getRelations WordSense.getRelations()} retrieves the relations themselves.
 *
 * <p> Each {@code WordSense} has exactly one associated {@code Word} (however, a given {@code Word} may have one
 * or more {@code WordSense}s with different orthographic case (e.g., the nouns "CD" vs. "Cd")).
 *
 * @see Relation
 * @see Synset
 * @see Word
 */
public final class WordSense implements RelationArgument, Comparable<WordSense> {
  /**
   * <em>Optional</em> restrictions for the position(s) an {@linkplain POS#ADJ adjective} can take
   * relative to the noun it modifies. aka "adjclass".
   */
  public enum AdjPosition {
    NONE(0),
    /**
     * of adjectives; relating to or occurring within the predicate of a sentence
     */
    PREDICATIVE(1),
    /**
     * of adjectives; placed before the nouns they modify
     */
    ATTRIBUTIVE(2), // synonymous with PRENOMINAL
    //PRENOMINAL(2), // synonymous with ATTRIBUTIVE
    IMMEDIATE_POSTNOMINAL(4),
    ;
    final int flag;
    AdjPosition(final int flag) {
      staticThis.ALIASES.registerAlias(this, name(), name().toLowerCase());
      this.flag = flag;
    }
    static boolean isActive(final AdjPosition adjPosFlag, final int flags) {
      return 0 != (adjPosFlag.flag & flags);
    }
    static AdjPosition fromValue(final String label) {
      return staticThis.ALIASES.valueOf(label);
    }
    private static class staticThis {
      static EnumAliases<AdjPosition> ALIASES = EnumAliases.make(AdjPosition.class);
    }
  } // end enum AdjPosition

  //
  // Instance implementation
  //
  private final Synset synset;
  /** case sensitive lemma */
  private final String lemma;
  private final int lexid;
  // represents up to 64 different verb frames are possible (as of now, 35 exist)
  private long verbFrameFlags;
  private short senseNumber;
  private short sensesTaggedFrequency;
  private short coreRank;
  // only needs to be a byte since there are only 3 bits of flag values
  private final byte adjPositionFlags;

  //
  // Constructor
  //
  WordSense(final Synset synset, final String lemma, final int lexid, final int flags) {
    this.synset = synset;
    this.lemma = lemma;
    this.lexid = lexid;
    this.adjPositionFlags = SignedBytes.checkedCast(flags);
    this.senseNumber = -1;
    this.sensesTaggedFrequency = -1;
  }

  void setVerbFrameFlag(final int fnum) {
    verbFrameFlags |= 1L << (fnum - 1);
  }

  //
  // Accessors
  //

  @Override
  public POS getPOS() {
    return synset.getPOS();
  }

  @Override
  public Synset getSynset() {
    return synset;
  }

  /**
   * If {@code word} lemma and {@code POS} are compatible with this
   * {@code WordSense}, return {@code this}, else return {@code null}.
   * Provided for API congruency between {@code WordSense} and {@code Synset}.
   */
  @Override
  public WordSense getWordSense(final Word word) {
    // alternate (less efficient, less pedagogical) implementations:
    //   return word.equals(getWord());
    //   return getSynset().getWordSense(word);
    if (word.getPOS() == getPOS() && getLemma().equalsIgnoreCase(word.getLowercasedLemma())) {
      return this;
    }
    return null;
  }

  /**
   * Returns the <em>natural-cased</em> lemma representation of this {@code WordSense} (aka "true cased"). Its
	 * lemma is its orthographic representation, for example <tt>"dog"</tt> or <tt>"U.S.A."</tt> or <tt>"George
	 * Washington"</tt>. Contrast to the canonical lemma provided by {@link Word#getLowercasedLemma()}.
   */
  public String getLemma() {
    return lemma;
  }

  @Override
  public Iterator<WordSense> iterator() {
    return LightImmutableList.of(this).iterator();
  }

  public String adjFlagsToString() {
    if (adjPositionFlags == 0) {
      return "NONE";
    }
    final StringBuilder flagString = new StringBuilder();
    if (AdjPosition.isActive(AdjPosition.PREDICATIVE, adjPositionFlags)) {
      flagString.append("predicative");
    }
    if (AdjPosition.isActive(AdjPosition.ATTRIBUTIVE, adjPositionFlags)) {
      if (flagString.length() != 0) {
        flagString.append(',');
      }
      // synonymous with attributive - WordNet browser seems to use this
      // while the database files seem to indicate it as attributive
      flagString.append("prenominal");
    }
    if (AdjPosition.isActive(AdjPosition.IMMEDIATE_POSTNOMINAL, adjPositionFlags)) {
      if (flagString.length() != 0) {
        flagString.append(',');
      }
      flagString.append("immediate_postnominal");
    }
    return flagString.toString();
  }

  /**
   * 1-indexed value whose order is relative to its {@link Word}.
   *
   * <p> Note that this value often varies across WordNet versions.
   * For those senses which never occurred in sense tagged corpora, it is
   * arbitrarily chosen.
   * @see <a href="http://wordnet.princeton.edu/wordnet/man/cntlist.5WN.html#toc4">
   *   'NOTES' in cntlist WordNet documentation</a>
   */
  public int getSenseNumber() {
    if (senseNumber < 1) {
      // Ordering of Word's Synsets (combo(Word, Synset)=WordSense)
      // is defined by sense tagged frequency (but this is implicit).
      // Get Word and scan this WordSense's Synsets and
      // find the one with this Synset.
      final Word word = getWord();
      int localSenseNumber = 0;
      for (final Synset syn : word.getSynsets()) {
        localSenseNumber--;
        if (syn.equals(synset)) {
          localSenseNumber = -localSenseNumber;
          break;
        }
      }
      assert localSenseNumber > 0 : "Word lemma: "+lemma+" "+getPOS();
      assert localSenseNumber < Short.MAX_VALUE;
      this.senseNumber = (short)localSenseNumber;
    }
    return senseNumber;
  }

  /**
   * Uses this {@code WordSense}'s lemma as key to find its {@code Word}; not very efficient
   * and probably not necessary.
   */
  // WordSense contains everything Word does - no need to expose this
  Word getWord() {
    final Word word = synset.wordNet.lookupWord(lemma, getPOS());
    assert word != null : "lookupWord failed for \""+lemma+"\" "+getPOS();
    return word;
  }

  /**
   * Builds '{@code sensekey}'; used for searching <tt>cntlist.rev</tt> and <tt>sents.vrb</tt>.
   *
   * <p> High-level description:
   * <blockquote>{@code lemma}<b>%</b>{@code ss_type}<b>:</b>{@code lex_filenum}<b>:</b>{@code lex_id}<b>:</b>{@code head_word}<b>:</b>{@code head_id}</blockquote>
   * <ul>
   *   <li> {@code lemma} is lowercase, spaces are represented as underscores </li>
   *   <li> {@code sstype} is {@link POS}; see {@link POS#getWordNetCode()} </li>
   *   <li> {@code lex_filenum} is {@link Synset#lexfilenum()} </li>
   *   <li> {@code lex_id} is {@link WordSense#getLexid()} </li>
   *   <li> {@code head_word:head_id} applies <em>only</em> to {@link POS#ADJ adjectives} </li>
   * </ul>
   * For example, <tt>communicate%2:40:10::</tt>
   * @see <a href="http://wordnet.princeton.edu/wordnet/man/senseidx.5WN.html#sect3">
   *   http://wordnet.princeton.edu/wordnet/man/senseidx.5WN.html#sect3</a>
   */
  //TODO cache this ? does it ever become not useful to cache this ? better to cache getSensesTaggedFrequency()
  // power users might be into this: https://sourceforge.net/tracker/index.php?func=detail&aid=2009619&group_id=33824&atid=409470
  public CharSequence getSenseKey() {
    final String searchWord;
    final int headSense;
    if (getSynset().isAdjectiveCluster()) {
      final List<RelationArgument> adjsses = getSynset().getRelationTargets(RelationType.SIMILAR_TO);
      //assert adjsses.size() == 1 : this + " adjsses: " + adjsses;
			// failed with WN20: [WordSense 2093443@[POS adjective]:"acerate"#1] adjsses: [[Synset 2092764@[POS adjective]<adj.all>{simple, unsubdivided}], [Synset 1749884@[POS adjective]<adj.all>{pointed}]]
      final Synset adjss = (Synset) adjsses.get(0);
      // if satellite, key lemma in cntlist.rev
      // is adjss's first word (no case) and
      // adjss's lexid (aka lexfilenum) otherwise
      searchWord = adjss.getWordSenses().get(0).getLemma();
      headSense = adjss.getWordSenses().get(0).lexid;
    } else {
      searchWord = getLemma();
      headSense = lexid;
    }
    final int lex_filenum = getSynset().lexfilenum();
    final StringBuilder senseKey;
    if (getSynset().isAdjectiveCluster()) {
      // slow equivalent
      // senseKey = String.format("%s%%%d:%02d:%02d:%s:%02d",
      //     getLemma().toLowerCase(),
      //     POS.SAT_ADJ.getWordNetCode(),
      //     lex_filenum,
      //     lexid,
      //     searchWord.toLowerCase(),
      //     headSense);
      final int keyLength = getLemma().length() + 1 /* POS code length */ +
        2 /* lex_filenum length */ + 2 /* lexid length */ +
        searchWord.length() + 2 /* headSense lexid length */ + 5 /* delimiters */;
      senseKey = new StringBuilder(keyLength);
      for (int i = 0, n = getLemma().length(); i != n; i++) {
        senseKey.append(Character.toLowerCase(getLemma().charAt(i)));
      }
      senseKey.append('%');
      senseKey.append(POS.SAT_ADJ.getWordNetCode());
      senseKey.append(':');
      if (lex_filenum < 10) {
        senseKey.append('0');
      }
      senseKey.append(lex_filenum);
      senseKey.append(':');
      if (lexid < 10) {
        senseKey.append('0');
      }
      senseKey.append(lexid);
      senseKey.append(':');
      for (int i = 0, n = searchWord.length(); i != n; i++) {
        senseKey.append(Character.toLowerCase(searchWord.charAt(i)));
      }
      senseKey.append(':');
      if (headSense < 10) {
        senseKey.append('0');
      }
      senseKey.append(headSense);
      //assert oldAdjClusterSenseKey(searchWord, headSense).contentEquals(senseKey);
      //assert senseKey.length() <= keyLength;
    } else {
      // slow equivalent
      // senseKey = String.format("%s%%%d:%02d:%02d::",
      //     getLemma().toLowerCase(),
      //     getPOS().getWordNetCode(),
      //     lex_filenum,
      //     lexid
      //     );
      final int keyLength = getLemma().length() + 1 /* POS code length */ +
        2 /* lex_filenum length */ + 2 /* lexid length */ + 5 /* delimiters */;
      senseKey = new StringBuilder(keyLength);
      for (int i = 0, n = getLemma().length(); i != n; i++) {
        senseKey.append(Character.toLowerCase(getLemma().charAt(i)));
      }
      senseKey.append('%');
      senseKey.append(getPOS().getWordNetCode());
      senseKey.append(':');
      if (lex_filenum < 10) {
        senseKey.append('0');
      }
      senseKey.append(lex_filenum);
      senseKey.append(':');
      if (lexid < 10) {
        senseKey.append('0');
      }
      senseKey.append(lexid);
      senseKey.append("::");
      //assert oldNonAdjClusterSenseKey().contentEquals(senseKey);
      //assert senseKey.length() <= keyLength;
    }
    return senseKey;
  }

  /**
   * @see <a href="http://wordnet.princeton.edu/wordnet/man/cntlist.5WN.html">
   *   <tt>cntlist.rev</tt></a>
   */
  public int getSensesTaggedFrequency() {
    if (sensesTaggedFrequency < 0) {
      // caching sensesTaggedFrequency requires minimal memory and provides a lot of value
      // (high-level and eliminating redundant work)
      // TODO we could use this Word's getTaggedSenseCount() to determine if
      // there were any tagged senses for *any* sense of it (including this one)
      // and really we wouldn't need to look at sense (numbers) exceeding that value
      // as an optimization
      final CharSequence senseKey = getSenseKey();
      final WordNet wn = synset.wordNet;
      final String line = wn.lookupCntlistDotRevLine(senseKey);
      int count;
      if (line != null) {
        // cntlist.rev line format:
        // <sense_key>  <sense_number>  tag_cnt
        final int lastSpace = line.lastIndexOf(' ');
        assert lastSpace > 0;
        count = CharSequences.parseInt(line, lastSpace + 1, line.length());
        // sanity check final int firstSpace = line.indexOf(" ");
        // sanity check assert firstSpace > 0 && firstSpace != lastSpace;
        // sanity check final int mySenseNumber = getSenseNumber();
        // sanity check final int foundSenseNumber =
        // sanity check   CharSequenceTokenizer.parseInt(line, firstSpace + 1, lastSpace);
        // sanity check if (mySenseNumber != foundSenseNumber) {
        // sanity check   System.err.println(this+" foundSenseNumber: "+foundSenseNumber+" count: "+count);
        // sanity check } else {
        // sanity check   //System.err.println(this+" OK");
        // sanity check }
        //[WordSense 9465459@[POS noun]:"unit"#5] foundSenseNumber: 7
        //assert getSenseNumber() ==
        //  CharSequenceTokenizer.parseInt(line, firstSpace + 1, lastSpace);
        assert count <= Short.MAX_VALUE;
        sensesTaggedFrequency = (short) count;
      } else {
        sensesTaggedFrequency = 0;
      }
    }
    return sensesTaggedFrequency;
  }

  /**
   * Adjective position indicator.
   * @see AdjPosition
   */
  public AdjPosition getAdjPosition() {
    if (adjPositionFlags == 0) {
      return AdjPosition.NONE;
    }
    assert getPOS() == POS.ADJ;
    if (AdjPosition.isActive(AdjPosition.PREDICATIVE, adjPositionFlags)) {
      assert ! AdjPosition.isActive(AdjPosition.ATTRIBUTIVE, adjPositionFlags);
      assert ! AdjPosition.isActive(AdjPosition.IMMEDIATE_POSTNOMINAL, adjPositionFlags);
      return AdjPosition.PREDICATIVE;
    }
    if (AdjPosition.isActive(AdjPosition.ATTRIBUTIVE, adjPositionFlags)) {
      assert ! AdjPosition.isActive(AdjPosition.PREDICATIVE, adjPositionFlags);
      assert ! AdjPosition.isActive(AdjPosition.IMMEDIATE_POSTNOMINAL, adjPositionFlags);
      return AdjPosition.ATTRIBUTIVE;
    }
    if (AdjPosition.isActive(AdjPosition.IMMEDIATE_POSTNOMINAL, adjPositionFlags)) {
      assert ! AdjPosition.isActive(AdjPosition.ATTRIBUTIVE, adjPositionFlags);
      assert ! AdjPosition.isActive(AdjPosition.PREDICATIVE, adjPositionFlags);
      return AdjPosition.IMMEDIATE_POSTNOMINAL;
    }
    throw new IllegalStateException("invalid flags "+adjPositionFlags);
  }

  /**
   * Unique number that when combined with lemma, uniquely identifies a sense within a lexicographer file.
   * @see Synset#getLexCategory()
   */
  int getLexid() {
    return lexid;
  }

  // published as long, stored as byte for max efficiency
  long getAdjPositionFlags() {
    return adjPositionFlags;
  }

  //TODO expert only. maybe publish as EnumSet
  long getVerbFrameFlags() {
    return verbFrameFlags;
  }

  /**
   * <p> Returns illustrative sentences <em>and</em> generic verb frames.  This <b>only</b> has values
   * for {@link POS#VERB} {@code WordSense}s.
   *
   * <p> For illustrative sentences (<tt>sents.vrb</tt>), "%s" is replaced with the verb lemma
   * which seems unnecessarily inefficient since you have the WordSense anyway.
   *
   * <pre>{@literal Example: verb "complete"#1 has 4 generic verb frames:
   *    1. *> Somebody ----s
   *    2. *> Somebody ----s something
   *    3. *> Something ----s something
   *    4. *> Somebody ----s VERB-ing
   *  and 1 specific verb frame:
   *    1. EX: They won't %s the story
   * }</pre>
   *
   * <p> Official WordNet 3.0 documentation indicates "specific" and generic frames
   * are mutually exclusive which is not the case.
   *
   * @see <a href="http://wordnet.princeton.edu/wordnet/man/wndb.5WN.html#sect6">
   *   http://wordnet.princeton.edu/wordnet/man/wndb.5WN.html#sect6</a>
   */
  public List<String> getVerbFrames() {
    if (getPOS() != POS.VERB) {
      return LightImmutableList.of();
    }
    final CharSequence senseKey = getSenseKey();
    final WordNet wn = synset.wordNet;
    final String sentenceNumbers = wn.lookupVerbSentencesNumbers(senseKey);
    List<String> frames = LightImmutableList.of();
    if (sentenceNumbers != null) {
      frames = Lists.newArrayList();
      // fetch the illustrative sentences indicated in sentenceNumbers
      //TODO consider substituting in lemma for "%s" in each
      //FIXME this logic is a bit too complex/duplicated!!
      int s = 0;
      int e = sentenceNumbers.indexOf(',');
      final int n = sentenceNumbers.length();
      e = e > 0 ? e : n;
      for ( ; s < n;
        // e = next comma OR if no more commas, e = n
        s = e + 1, e = sentenceNumbers.indexOf(',', s), e = e > 0 ? e : n) {
        final String sentNum = sentenceNumbers.substring(s, e);
        final String sentence = wn.lookupVerbSentence(sentNum);
        assert sentence != null;
        frames.add(sentence);
      }
    }
    //else {
      //assert verbFrameFlags == 0L : "not mutually exclusive for "+this;
    //}
    if (verbFrameFlags != 0L) {
      final int numGenericFrames = Long.bitCount(verbFrameFlags);
      if (frames.isEmpty()) {
        frames = Lists.newArrayList();
      } else {
        ((ArrayList<String>)frames).ensureCapacity(frames.size() + numGenericFrames);
      }

      // fetch any generic verb frames indicated by verbFrameFlags
      // numberOfLeadingZeros (leftmost), numberOfTrailingZeros (rightmost)
      // 001111111111100
      //  ^-lead      ^-trail
      // simple scan between these (inclusive) should cover rest
      for (int fn = Long.numberOfTrailingZeros(verbFrameFlags),
          lfn = Long.SIZE - Long.numberOfLeadingZeros(verbFrameFlags);
          fn < lfn;
          fn++) {
        if ((verbFrameFlags & (1L << fn)) != 0L) {
          final String frame = wn.lookupGenericFrame(fn + 1);
          assert frame != null :
            "this: "+this+" fn: "+fn+
            " shift: "+((1L << fn)+
            " verbFrameFlags: "+Long.toBinaryString(verbFrameFlags))+
            " verbFrameFlags: "+verbFrameFlags;
          frames.add(frame);
        }
      }
    }
    return LightImmutableList.copyOf(frames);
  }

  /**
   * A ranking of the top 5,000 "core" WordSenses derived from word frequencies in the <a href="http://www.natcorp.ox.ac.uk">
   *   British National Corpus</a>; WordSenses were selected by salience.
   * This data was created as part of Evocation project at Princeton;
   * This method provides access to a simple distillation of
   * <a href="http://wordnet.cs.princeton.edu/downloads/5K.clean.txt">
   * http://wordnet.cs.princeton.edu/downloads/5K.clean.txt</a>
   * @return 1-based rank, -1 if sense is unranked, or 0 to indicate required
   * data file missing
   *
   * @see <a href="http://wordnet.cs.princeton.edu/downloads.html">
   *   http://wordnet.cs.princeton.edu/downloads.html</a>
   */
  public int getCoreRank() {
    if (coreRank == 0) {
      final CharSequence senseKey = getSenseKey();
      final WordNet wordNet = synset.wordNet;
      final String line;
      try {
        line = wordNet.lookupCoreRankLine(senseKey);
      } catch (IllegalStateException ise) {
        return 0;
      }
      int rank;
      if (line != null) {
        // core-wordnet.ranked line format:
        // <sense_key> <? bracketed lemma, comma separated evocations ?> <1-based rank>
        final int lastSpace = line.lastIndexOf(' ');
        assert lastSpace > 0;
        rank = CharSequences.parseInt(line, lastSpace + 1, line.length());
        // sanity check final int firstSpace = line.indexOf(" ");
        // sanity check assert firstSpace > 0 && firstSpace != lastSpace;
        // sanity check final int mySenseNumber = getSenseNumber();
        // sanity check final int foundSenseNumber =
        // sanity check   CharSequenceTokenizer.parseInt(line, firstSpace + 1, lastSpace);
        // sanity check if (mySenseNumber != foundSenseNumber) {
        // sanity check   System.err.println(this+" foundSenseNumber: "+foundSenseNumber+" count: "+count);
        // sanity check } else {
        // sanity check   //System.err.println(this+" OK");
        // sanity check }
        //[WordSense 9465459@[POS noun]:"unit"#5] foundSenseNumber: 7
        //assert getSenseNumber() ==
        //  CharSequenceTokenizer.parseInt(line, firstSpace + 1, lastSpace);
        assert rank <= 5000;
        coreRank = (short) rank;
      } else {
        coreRank = -1;
      }
    }
    return coreRank;
  }

  @Override
  public String getDescription() {
    if (getPOS() != POS.ADJ && getPOS() != POS.SAT_ADJ) {
      return lemma;
    }
    final StringBuilder description = new StringBuilder(lemma);
    if (adjPositionFlags != 0) {
      description.append('(');
      description.append(adjFlagsToString());
      description.append(')');
    }
    final List<RelationArgument> targets = getRelationTargets(RelationType.ANTONYM);
    if (! targets.isEmpty()) {
      // adj 'acidic' has more than 1 antonym ('alkaline' and 'amphoteric')
      for (final RelationArgument target : targets) {
        description.append(" (vs. ");
        final WordSense antonym = (WordSense)target;
        description.append(antonym.getLemma());
        description.append(')');
      }
    }
    return description.toString();
  }

  //
  // Relations
  //

  private List<Relation> restrictRelations(final RelationType type) {
    final List<Relation> relations = synset.getRelations();
    List<Relation> list = null;
    for (final Relation relation : relations) {
      // consider all isSemantic Relations, but only isLexical Relations
      // which have this as their source
      if (relation.isLexical() && !relation.getSource().equals(this)) {
        continue;
      }
      if (type != null && type != relation.getType()) {
        continue;
      }
      list = add(list, relation);
    }
    if (list == null) {
      return LightImmutableList.of();
    }
    return LightImmutableList.copyOf(list);
  }

  @Override
  public List<Relation> getRelations() {
    return restrictRelations(null);
  }

  @Override
  public List<Relation> getRelations(final RelationType type) {
    return restrictRelations(type);
  }

  @Override
  public List<RelationArgument> getRelationTargets() {
    return Synset.collectTargets(getRelations());
  }

  @Override
  public List<RelationArgument> getRelationTargets(final RelationType type) {
    return Synset.collectTargets(getRelations(type));
  }

  // TODO consider getLexicalRelations() / getLexicalRelations(RelationType) / getLexicalRelationTargets()

  //
  // Object methods
  //

  @Override
  public boolean equals(Object object) {
    return (object instanceof WordSense)
      && ((WordSense) object).synset.equals(synset)
      && ((WordSense) object).lemma.equals(lemma);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(synset, lemma);
  }

  @Override
  public String toString() {
    return new StringBuilder("[WordSense ").
      append(synset.getOffset()).
      append('@').
      append(synset.getPOS()).
      append(":\"").
      append(getLemma()).
      append("\"#").
      append(getSenseNumber()).
      append(']').toString();
  }

  @Override
  public int compareTo(final WordSense that) {
    int result;
    result = WordNetLexicalComparator.TO_LOWERCASE_INSTANCE.compare(this.getLemma(), that.getLemma());
    if (result == 0) {
      result = this.getSenseNumber() - that.getSenseNumber();
      if (result == 0) {
        result = this.getSynset().compareTo(that.getSynset());
      }
    }
    return result;
  }
}