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

import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yawni.util.CharSequenceTokenizer;
import org.yawni.util.CharSequences;
import org.yawni.util.LightImmutableList;
import org.yawni.util.Utils;
import static org.yawni.util.Utils.add;

/**
 * A {@code Synset}, or <b>syn</b>onym <b>set</b>, represents a line of a WordNet <code>data.<em>pos</em></code> file (e.g., {@code data.noun}).
 * A {@code Synset} represents a concept, and contains a set of {@link WordSense}s, each of which has a sense
 * that names that concept (and each of which is therefore synonymous with the other {@code WordSense}s in the
 * {@code Synset}).
 *
 * <p> {@code Synset}s are linked by {@link Relation}s into a network of related concepts; this is the <em>Net</em>
 * in WordNet.  {@link Synset#getRelationTargets Synset.getRelationTargets()} retrieves the targets of these links, and
 * {@link Synset#getRelations Synset.getRelations()} retrieves the relations themselves.
 *
 * @see WordSense
 * @see Relation
 */
public final class Synset implements RelationArgument, Comparable<Synset>, Iterable<WordSense> {
  private static final Logger log = LoggerFactory.getLogger(Synset.class.getName());
  //
  // Instance implementation
  //
  /** package private for use by WordSense */
  final WordNet wordNet;
  /** offset in <code>data.<em>pos</em></code> file; {@code Synset.hereiam} in {@code wn.h} */
  private final int offset;
  private final LightImmutableList<WordSense> wordSenses;
  // TODO consider storing Relations indirectly
  // (all have common source == this?)
  // pos, synset offset, optionally Synset WordSense rank (NOT sense number which is Word-relative rank)
  // also requires WordNet ref
  // increases Synset's direct 32-bit size
  // to 4+1+4+2 = 11 B, though may be able to pack synset offset and rank into less bytes
  // ! SoftReference is the way to go here !
  private final LightImmutableList<Relation> relations;
  private final byte posOrdinal;
  private final byte lexfilenum;
  private final boolean isAdjectiveCluster;

  //
  // Constructor
  //
  Synset(final String line, final WordNet wordNet) {
    this.wordNet = wordNet;
    final CharSequenceTokenizer tokenizer = new CharSequenceTokenizer(line, " ");
    this.offset = tokenizer.nextInt();
    final int lexfilenumInt = tokenizer.nextInt();
    // there are currently only 45 lexfiles
    // http://wordnet.princeton.edu/man/lexnames.5WN.html
    // disable assert to be lenient for generated WordNets
    //assert lexfilenumInt < 45 : "lexfilenumInt: "+lexfilenumInt;
    this.lexfilenum = Utils.checkedCast(lexfilenumInt);
    CharSequence ss_type = tokenizer.nextToken();
    if ("s".contentEquals(ss_type)) {
      ss_type = "a";
      // satellite implies indirect antonym
      this.isAdjectiveCluster = true;
    } else {
      this.isAdjectiveCluster = false;
    }
    this.posOrdinal = Utils.checkedCast(POS.lookup(ss_type).ordinal());

    final int wordCount = tokenizer.nextHexInt();
    final WordSense[] localWordSenses = new WordSense[wordCount];
    for (int i = 0; i < wordCount; i++) {
      String lemma = tokenizer.nextToken().toString();
      final int lexid = tokenizer.nextHexInt();
      int flags = 0;
      // strip the syntactic marker, e.g., "(a)" || "(ip)" || ...
      final int lparenIdx;
      if (lemma.charAt(lemma.length() - 1) == ')' &&
        (lparenIdx = lemma.lastIndexOf('(')) > 0) {
        final int rparenIdx = lemma.length() - 1;
        assert ')' == lemma.charAt(rparenIdx);
        //TODO use String.regionMatches() instead of creating 'marker'
        final String marker = lemma.substring(lparenIdx + 1, rparenIdx);
        lemma = lemma.substring(0, lparenIdx);
        if (marker.equals("p")) {
          flags |= WordSense.AdjPosition.PREDICATIVE.flag;
        } else if (marker.equals("a")) {
          flags |= WordSense.AdjPosition.ATTRIBUTIVE.flag;
        } else if (marker.equals("ip")) {
          flags |= WordSense.AdjPosition.IMMEDIATE_POSTNOMINAL.flag;
        } else {
          throw new RuntimeException("unknown syntactic marker " + marker);
        }
      }
      localWordSenses[i] = new WordSense(this, lemma.replace('_', ' '), lexid, flags);
    }
    this.wordSenses = LightImmutableList.of(localWordSenses);

    final int relationCount = tokenizer.nextInt();
    // allocate extra space in this temporary for additional Relations (e.g., morphosemantic)
    final List<Relation> localRelations = new ArrayList<Relation>(2 * relationCount);
    for (int i = 0; i < relationCount; i++) {
      final Relation relation = Relation.makeRelation(this, localRelations.size(), tokenizer);
      localRelations.add(relation);
      addVerbGroupTransitiveClosureRelations(relation, localRelations);
      addExtraMorphosemanticRelations(relation, localRelations);
    }

    this.relations = LightImmutableList.copyOf(localRelations);
    //assert relations.equals(localRelations);

    if (posOrdinal == POS.VERB.ordinal()) {
      final int f_cnt = tokenizer.nextInt();
      for (int i = 0; i < f_cnt; i++) {
        final CharSequence skip = tokenizer.nextToken(); // "+"
        assert "+".contentEquals(skip) : "skip: "+skip;
        final int f_num = tokenizer.nextInt();
        final int w_num = tokenizer.nextHexInt();
        if (w_num > 0) {
          this.wordSenses.get(w_num - 1).setVerbFrameFlag(f_num);
        } else {
          for (int j = 0; j < localWordSenses.length; j++) {
            this.wordSenses.get(j).setVerbFrameFlag(f_num);
          }
        }
      }
    }
  }

  private boolean addVerbGroupTransitiveClosureRelations(final Relation relation, final List<Relation> localRelations) {
    if (relation.getType() != RelationType.VERB_GROUP) {
      return false;
    }
    assert posOrdinal == 2;
    // insert additional VERB_GROUP relation instances
    //TODO offsetKey can be created more efficiently with a custom method
    final CharSequence srcOffsetKey = String.format("%08d", offset);
    final Iterable<CharSequence> lexRelLines = wordNet.lookupVerbGroupLines(srcOffsetKey);
    if (Iterables.isEmpty(lexRelLines)) {
      return false;
    }
    final POS myTargetPOS = relation.getTargetPOS();
    assert myTargetPOS == POS.VERB;
    final int myTargetSynsetIdx = relation.getTargetIndex();
    assert myTargetSynsetIdx == 0;
    boolean foundMatch = false;
    for (final CharSequence vgRelLine : lexRelLines) {
      final CharSequenceTokenizer lexTokenizer = new CharSequenceTokenizer(vgRelLine, " ");
      // not really necessary, could just skipToken()
      final String srcOffset = lexTokenizer.nextToken();
      assert srcOffset.contentEquals(srcOffsetKey);
      while (lexTokenizer.hasMoreTokens()) {
        final int targetOffset = lexTokenizer.nextInt();
        final int targetIndex = 0; // targetIndex of Synset is 0; see Relation#getTarget()/Relation#resolveTarget
        final SemanticRelation vgRelation = new SemanticRelation(
          targetOffset, targetIndex, (byte)POS.VERB.ordinal(),
          localRelations.size(),
          this, (byte)RelationType.VERB_GROUP.ordinal()
          );
        // ensure not already in there
        if (! contains(vgRelation, localRelations)) {
          localRelations.add(vgRelation);
          foundMatch = true;
        } else {
//          System.err.println("AVOIDING DUP "+vgRelation); // toString causes stack overflow
//          System.err.println("AVOIDING DUP");
        }
      }
    }
    return foundMatch;
  }

  // because srcRelationIndex is generated, we can't use it in the comparison; ultimately,
  // the srcRelationIndex will be unique and effects the equals() and compareTo() ordering,
  // so we have to compare manually to prevent logical duplicates
  private static boolean contains(final Relation needle, final List<Relation> localRelations) {
    for (final Relation that : localRelations) {
      if (that.getSource().equals(needle.getSource())
        // DON'T USE SYNTHESIZED SOURCE RELATION INDEX && that.getSourceRelationIndex() == needle.getSourceRelationIndex()
         && that.getType() == needle.getType()
         && that.getTargetOffset() == needle.getTargetOffset()
         && that.getTargetIndex() == needle.getTargetIndex()) {
        return true;
      }
    }
    return false;
  }

  private boolean addExtraMorphosemanticRelations(final Relation relation, final List<Relation> localRelations) {
    if (relation.getType() != RelationType.DERIVATIONALLY_RELATED) {
      return false;
    }
    final int srcPOSOrdinal = posOrdinal;
    if (srcPOSOrdinal != POS.NOUN.ordinal() && srcPOSOrdinal != POS.VERB.ordinal()) {
      return false;
    }
    final POS targetPOS = relation.getTargetPOS();
    if (targetPOS != POS.NOUN && targetPOS != POS.VERB) {
      return false;
    }
    // insert MorphosemanticRelation instances
    final LexicalRelation lexRel = (LexicalRelation) relation;
//      showLine(lexRel);
    //TODO offsetKey can be created more efficiently with a custom method
    final CharSequence srcOffsetKey;
    if (srcPOSOrdinal == 1) {
      srcOffsetKey = String.format("1%08d", offset);
    } else {
      assert srcPOSOrdinal == 2;
      srcOffsetKey = String.format("2%08d", offset);
    }
    final Iterable<CharSequence> lexRelLines = wordNet.lookupMorphoSemanticRelationLines(srcOffsetKey);
    // 1331 of these
    if (Iterables.isEmpty(lexRelLines)) {
//        System.err.println("eek! "+srcOffsetKey+" "+getPOS()+" "+line);
//        continue;
      return false;
    }
    // this is invariant for this relation
    final int mySrcSynsetIdx = wordSenses.indexOf(lexRel.getSource());
    assert mySrcSynsetIdx >= 0;
    final POS myTargetPOS = lexRel.getTargetPOS();
    final int myTargetSynsetIdx = lexRel.getTargetIndex() - 1;
    assert myTargetSynsetIdx >= 0;
    final int myTargetOffset = lexRel.getTargetOffset();
    boolean foundMatch = false;
    RelationType mrtype = null;
    for (final CharSequence lexRelLine : lexRelLines) {
//          System.err.println("lexRelLine: "+lexRelLine);
      final CharSequenceTokenizer lexTokenizer = new CharSequenceTokenizer(lexRelLine, " ");
      // not really necessary, could just skipToken()
      final String srcPOSOffset = lexTokenizer.nextToken();
      assert srcPOSOffset.contentEquals(srcOffsetKey);
      final int srcSynsetIdx = lexTokenizer.nextInt();
      if (srcSynsetIdx != mySrcSynsetIdx) {
//            System.err.println("fail 0 :: "+mySrcSynsetIdx+" "+srcSynsetIdx);
          continue;
      }
      final String mtype = lexTokenizer.nextToken();
      //TODO pull off POS part using some new CharSequenceTokenizer nextChar()/charAt()/seek functionality
      final String targetPOSOffset = lexTokenizer.nextToken();
      final int targetSynsetIdx = lexTokenizer.nextInt();
      if (targetSynsetIdx != myTargetSynsetIdx) {
//            System.err.println("fail 1 :: "+myTargetSynsetIdx+" "+targetSynsetIdx);
          continue;
      }
      // POS mismatch is possible (rare)
      if (targetPOS != myTargetPOS) {
          continue;
      }

      final int targetOffset = CharSequences.parseInt(targetPOSOffset, 1, targetPOSOffset.length());
      if (targetOffset != myTargetOffset) {
//            System.err.println("fail 2 :: targetOffset "+myTargetOffset+" "+targetOffset);
          continue;
      }
      // Yahoo! full match
//          System.err.println("full match!");
      foundMatch = true;
      final MorphosemanticRelation morphorel = MorphosemanticRelation.fromValue(mtype);
      mrtype = RelationType.valueOf(morphorel.name());
//        break;
    }

    if (mrtype != null) {
//          System.err.println("full match! "+mrtype);
      final LexicalRelation morphosemanticRelation = new LexicalRelation(lexRel, mrtype, localRelations.size());
      localRelations.add(morphosemanticRelation);
    }
    //        assert foundMatch;
    if (! foundMatch) {
      // 4895 instances
//        showLine(lexRel);
    }
    return foundMatch;
  }

  // debug method
  private static void showLine(final LexicalRelation lexRel) {
    final int srcPOS = lexRel.getSource().getPOS().ordinal();
    final int srcOffset = lexRel.getSource().getSynset().getOffset();
    final int srcSynsetIdx = lexRel.getSource().getSynset().getWordSenses().indexOf(lexRel.getSource());

    final int targetPOS = lexRel.getTargetPOS().ordinal();
    final int targetOffset = lexRel.getTargetOffset();
    final int targetSynsetIdx = lexRel.getTargetIndex();
    System.err.println("actual lexRel: "+srcPOS+""+srcOffset+" "+srcSynsetIdx+" "+"XXX"+" "+targetPOS+""+targetOffset+" "+targetSynsetIdx);
  }

  //
  // Accessors
  //

	@Override
  public POS getPOS() {
    return POS.fromOrdinal(posOrdinal);
  }

  boolean isAdjectiveCluster() {
    return isAdjectiveCluster;
  }

  int lexfilenum() {
    return lexfilenum;
  }

  /**
   * Provides access to the 'lexicographer category' of this {@code Synset}.  This
   * is variously called the 'lexname' or 'supersense'.
   * @return the <em>lexname</em> this {@code Synset} is a member of, e.g., "noun.quantity"
   * @see <a href="http://wordnet.princeton.edu/wordnet/man/lexnames.5WN.html">
   *   http://wordnet.princeton.edu/wordnet/man/lexnames.5WN.html</a>
   */
  public String getLexCategory() {
    return Lexname.lookupLexCategory(lexfilenum());
  }

  Lexname getLexname() {
    return Lexname.lookupLexname(lexfilenum());
  }

  /**
   * Returns the "gloss", or definition of this Synset, and optionally some example sentences.
   */
  @SuppressWarnings("deprecation") // using Character.isSpace() for file compat
  public String getGloss() {
    final String line = wordNet.getSynsetLineAt(getPOS(), offset);
    // find gloss
    final int index = line.indexOf('|');
    if (index > 0) {
      // jump '|' and immediately following ' '
      assert line.charAt(index + 1) == ' ';
      int incEnd = line.length() - 1;
      for (int i = incEnd; i >= 0; i--) {
        if (! Character.isSpace(line.charAt(i))) {
          incEnd = i;
          break;
        }
      }
      final int finalLen = (incEnd + 1) - (index + 2);
      if (finalLen > 0) {
        return line.substring(index + 2, incEnd + 1);
      } else {
        // synset with no gloss (support generated WordNets)
        return "";
      }
    } else {
      log.warn("Synset has no gloss?:\n{}", line);
      return "";
    }
  }

  /**
   * The senses whose common meaning this Synset represents.
   */
  public List<WordSense> getWordSenses() {
    return wordSenses;
  }

  /**
   * If {@code word} is a member of this {@code Synset}, return the
   * {@code WordSense} it implies, else return {@code null}.
   */
	@Override
  public WordSense getWordSense(final Word word) {
    for (final WordSense wordSense : wordSenses) {
      if (wordSense.getLemma().equalsIgnoreCase(word.getLowercasedLemma())) {
        return wordSense;
      }
    }
    return null;
  }

  /** {@inheritDoc} */
	@Override
  public Iterator<WordSense> iterator() {
    return wordSenses.iterator();
  }

  /**
   * Offsets are used in some low-level applications as a means of referring to
   * a specific {@code Synset}; Sometimes they need to be in 8-digit padded form, i.e.,
   * {@code String.format("2%08d", synset.getOffset())}, so their lexicographic
   * sort order will match their numeric sort order.
   * @return this {@code Synset}'s offset in the data files.
   */
  public int getOffset() {
    return offset;
  }

  /** This is <strong>not</strong> {@link Word#getSense(int)} */
  WordSense getWordSense(final int index) {
    return wordSenses.get(index);
  }

  /** This is <strong>not</strong> {@link Word#getSense(int)} */
  int getSynsetIndex(final WordSense wordSense) {
    return wordSenses.indexOf(wordSense);
  }

  /**
   * If {@code soughtSenseKey} is a member of this {@code Synset}, return the
   * {@code WordSense} it implies, else return {@code null}.
   */
  WordSense getWordSense(final String soughtSenseKey) {
    final Comparator<CharSequence> comparator = WordNetLexicalComparator.TO_LOWERCASE_INSTANCE;
    for (final WordSense wordSense : wordSenses) {
      final CharSequence senseKey = wordSense.getSenseKey();
      // fail: doesn't ignore underscores! probably WordSense.getSenseKey() should've dealt with this
//      if (soughtSenseKey.contentEquals(senseKey)) {
//        return wordSense;
//      }
      if (comparator.compare(soughtSenseKey, senseKey) == 0) {
        return wordSense;
      }
    }
    return null;
  }

  /** {@inheritDoc} */
	@Override
  public String getDescription() {
    final StringBuilder buffer = new StringBuilder();
    buffer.append('{');
    //TODO use Joiner
    for (int i = 0, n = wordSenses.size(); i < n; i++) {
      final WordSense wordSense = wordSenses.get(i);
      if (i > 0) {
        buffer.append(", ");
      }
      buffer.append(wordSense.getLemma());
//      buffer.append(wordSense);
//      buffer.append('#');
//      buffer.append(wordSense.getSenseNumber());
    }
    buffer.append('}');
    return buffer.toString();
  }

  //
  // Relations
  //

  /**
   * {@inheritDoc}
   *
   * <p> {@code Synset} holds <em>all</em> {@code Relation}s for itself and
   * its {@code WordSense}s.  As a result, this method returns both {@link SemanticRelation}s
   * for which it is the source <em>and</em> {@link LexicalRelation}s for which one of its
   * senses is the source.
   *
   * @see Synset#getSemanticRelations(RelationType)
   */
	@Override
  public List<Relation> getRelations() {
    return relations;
  }

  /** {@inheritDoc} */
	@Override
  public List<Relation> getRelations(final RelationType soughtType) {
    // regardless of includeAuxiliaryTypes
    // - type == MERONYM should auxiliary type variants
    // - type == HOLONYM should auxiliary type variants
    // - type == DOMAIN should auxiliary type variants
    // - type == DOMAIN_MEMBER should auxiliary type variants
    // really, includeAuxiliaryTypes should only mean include instances
    final boolean includeInstances = true;
    List<Relation> list = null;
    //TODO
    // if superTypes exist, search them
    // if current type exists, search it
    // if subTypes exist, search them
    for (final Relation relation : relations) {
      final RelationType rType = relation.getType();
//      } else if (includeInstances && soughtType == HYPONYM && rType == INSTANCE_HYPONYM) {
//        list = add(list, relation);
//      } else if (includeInstances && soughtType == HYPERNYM && rType == INSTANCE_HYPERNYM) {
//        list = add(list, relation);
//      }
        if (rType == soughtType) {
          list = add(list, relation);
//        } else if (rType.auxiliaryTypes.contains(soughtType)) {
        } else if (soughtType.auxiliaryTypes.contains(rType)) {
          list = add(list, relation);
        }
    }
    // if list == null && type has auxType, recall this method with that auxtype
    if (list == null) {
//      if (! type.subTypes.isEmpty()) {
//        System.err.println("going for it "+type+" this: "+this+" subType: "+type.subTypes.get(0));
//        assert type.subTypes.size() == 1;
//        return getRelations(type.subTypes.get(0));
//      } else {
//        //System.err.println("type "+type+" for "+this+" has no subTypes");
//      }
      return LightImmutableList.of();
    }
    return LightImmutableList.copyOf(list);
  }

  // consider getSemanticRelations()

  /**
   * Returns <em>only</em> {@link SemanticRelation}s
   * which have this Synset as their source that have
   * type {@code type}.
   *
   * @see Synset#getRelations()
   */
  public List<SemanticRelation> getSemanticRelations(final RelationType type) {
    List<SemanticRelation> list = null;
    for (final Relation relation : relations) {
      if ((type == null || relation.getType() == type) &&
        relation.getSource().equals(this)) {
        final SemanticRelation semanticRelation = (SemanticRelation) relation;
        list = add(list, semanticRelation);
      }
    }
    if (list == null) {
      return LightImmutableList.of();
    }
    return LightImmutableList.copyOf(list);
  }

  /** {@inheritDoc} */
	@Override
  public List<RelationArgument> getRelationTargets() {
    return Synset.collectTargets(getRelations());
  }

  /** {@inheritDoc} */
	@Override
  public List<RelationArgument> getRelationTargets(final RelationType type) {
    return Synset.collectTargets(getRelations(type));
  }

  static List<RelationArgument> collectTargets(final List<? extends Relation> relations) {
    final RelationArgument[] targets = new RelationArgument[relations.size()];
    for (int i = 0, n = relations.size(); i < n; i++) {
      targets[i] = relations.get(i).getTarget();
    }
    return LightImmutableList.of(targets);
  }

  /** {@inheritDoc} */
	@Override
  public Synset getSynset() {
    return this;
  }

  //
  // Object methods
  //

  /** {@inheritDoc} */
  @Override
  public boolean equals(Object that) {
    return (that instanceof Synset)
      && ((Synset) that).posOrdinal == posOrdinal
      && ((Synset) that).getOffset() == getOffset();
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    // times 10 shifts left by 1 decimal place
    return (getOffset() * 10) + getPOS().hashCode();
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return new StringBuilder("[Synset ").
      append(getOffset()).
      append('@').
      append(getPOS()).
      append('<').
      //append('#').
      //append(lexfilenum()).
      //append("::").
      append(getLexCategory()).
      append('>').
      //append(": ").
      append(getDescription()).
      append("]").toString();
  }

  /** {@inheritDoc} */
	@Override
  public int compareTo(final Synset that) {
    int result;
    result = this.getPOS().compareTo(that.getPOS());
    if (result == 0) {
      result = this.getOffset() - that.getOffset();
    }
    return result;
  }
}