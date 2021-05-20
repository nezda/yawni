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

import org.yawni.util.CharSequenceTokenizer;
import com.google.common.collect.ComparisonChain;

import static org.yawni.util.Utils.hash;

/**
 * A {@code Relation} encodes a lexical <em>or</em> semantic relationship between WordNet entities.  A lexical
 * relationship holds between {@link WordSense}s; a semantic relationship holds between {@link Synset}s.
 * Relationships are <em>directional</em>:  the two roles of a relationship are the <em>source</em> and <em>target</em>.
 * Relationships are <em>typed</em>: the type of a relationship is a {@link RelationType}, and can
 * be retrieved via {@link Relation#getType Relation.getType()}.
 *
 * <p> This class used to be called {@code Pointer} as it is often referred to in the official WordNet documentation.
 *
 * @see Synset
 * @see WordSense
 */
public abstract class Relation implements Comparable<Relation> {
  /**
   * These target* and source* fields are used to avoid paging in the target/source before it is
   * required, and to prevent keeping a large portion of the database resident
   * once the target/source has been queried.  The first time they are used, they act as
   * an external key; subsequent uses, in conjunction with
   * {@link WordNet}'s caching mechanism, can be thought of as a
   * {@link java.lang.ref.WeakReference}.
   */
  private final int targetOffset;
  private final int targetIndex;
  private final byte targetPOSOrdinal;

  private final int sourceOffset;
  private final int sourceIndex;
  private final byte sourcePOSOrdinal;

  private final WordNet wordNet;

  //
  // Instance variables
  //

  private final byte relationTypeOrdinal;
  /**
   * The index of this {@code Relation} within the array of {@code Relation}s in the source {@code Synset}.
   * Only used in {@code equals}, {@code compare}, {@code hashCode}: differentiates distinct relations of
   * the same type emanating from the same {@code Synset}.
   */
  private final int sourceRelationIndex;

  //
  // Constructor
  //

  Relation(final int targetOffset, final int targetIndex, final POS targetPOS, final WordNet wordNet,
    final int sourceRelationIndex, final int sourceOffset, final int sourceIndex, final POS sourcePOS,
      final RelationType relationType) {
    this.targetOffset = targetOffset;
    this.targetIndex = targetIndex;
    this.targetPOSOrdinal = targetPOS.getByteOrdinal();
    this.wordNet = wordNet;
    this.sourceRelationIndex = sourceRelationIndex;
    this.sourceOffset = sourceOffset;
    this.sourceIndex = sourceIndex;
    this.sourcePOSOrdinal = sourcePOS.getByteOrdinal();
    this.relationTypeOrdinal = relationType.getByteOrdinal();
  }

  /**
   * Copy constructor to create Relation with equal source and target, but different type
   */
  Relation(final Relation that, final RelationType relationType, final int relationIndex) {
    this(that.targetOffset,
         that.targetIndex,
         that.getTargetPOS(),
         that.wordNet,
         relationIndex,
         that.sourceOffset,
         that.sourceIndex,
         that.getSourcePOS(),
         relationType);
  }

  /** Factory method */
  static Relation makeRelation(final Synset synset, final int index, final CharSequenceTokenizer tokenizer) {
    final RelationType relationType = RelationType.parseKey(tokenizer.nextToken(), synset.getPOS());

    final int targetOffset = tokenizer.nextInt();

    final POS targetPOS = POS.lookup(tokenizer.nextToken());
    final int linkIndices = tokenizer.nextHexInt();
    assert linkIndices >> 16 == 0;
    final int sourceIndex = linkIndices >> 8; // select high byte
    final int targetIndex = linkIndices & 0xFF; // select low byte

    final RelationArgument source = Relation.resolve(synset, sourceIndex);
    if (source instanceof WordSense) {
      return new LexicalRelation(targetOffset, targetIndex, targetPOS,
          synset.wordNet, index, synset.getOffset(), sourceIndex, synset.getPOS(), relationType);
    } else if (source instanceof Synset) {
      return new SemanticRelation(targetOffset, targetIndex, targetPOS,
          synset.wordNet, index, synset.getOffset(), sourceIndex, synset.getPOS(), relationType);
    } else {
      throw new IllegalArgumentException();
    }
  }

  //
  // Accessors
  //
  public RelationType getType() {
    return RelationType.fromOrdinal(relationTypeOrdinal);
  }

  /** A lexical relationship holds between {@link WordSense}s */
  public abstract boolean isLexical();

  /** A semantic relationship holds between {@link Synset}s */
  public abstract boolean isSemantic();

  /**
   * @return source vertex of this directed relationship
   */
  public RelationArgument getSource() {
    return resolve(
        wordNet.getSynsetAt(
            getSourcePOS(),
            getSourceOffset()).orElse(null),
        getSourceIndex());
  }

  // internal dev method
  final POS getTargetPOS() {
    return POS.fromOrdinal(targetPOSOrdinal);
  }

  // internal dev method
  final int getTargetOffset() {
    return targetOffset;
  }

  // internal dev method
  // 1-based index; see resolve(synset, index)
  final int getTargetIndex() {
    return targetIndex;
  }

  // internal dev method
  final POS getSourcePOS() {
    return POS.fromOrdinal(sourcePOSOrdinal);
  }

  // internal dev method
  final int getSourceOffset() {
    return sourceOffset;
  }

  // internal dev method
  // 1-based index; see resolve(synset, index)
  final int getSourceIndex() {
    return sourceIndex;
  }

  final int getSourceRelationIndex() {
    return sourceRelationIndex;
  }

  public abstract boolean hasSource(RelationArgument that);

  /**
   * @return target vertex of this directed relationship
   */
  public RelationArgument getTarget() {
    return resolve(
        wordNet.getSynsetAt(
          getTargetPOS(),
          getTargetOffset()).orElse(null),
        targetIndex);
  }

  private static RelationArgument resolve(final Synset synset, final int index) {
    if (index == 0) {
      return synset;
    } else {
      return synset.getWordSense(index - 1);
    }
  }

  //
  // Object methods
  //

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof Relation) {
      Relation that = (Relation) obj;
      return that.sourceOffset == this.sourceOffset
          && that.sourceIndex == this.sourceIndex
          && that.sourcePOSOrdinal == this.sourcePOSOrdinal
          && that.sourceRelationIndex == this.sourceRelationIndex
          && that.relationTypeOrdinal == this.relationTypeOrdinal;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return hash(sourceOffset, sourceIndex, sourcePOSOrdinal, sourceRelationIndex);
  }

  @Override
  public String toString() {
    return new StringBuilder("[").
      append(getClass().getSimpleName()).
      //append("Relation").
      append(' ').
      append(getType().name()).
      //append("#").
      //append(index).
      append(" from ").
      //append(source).
      append(getSource()).
      //append(" → ").
      append(" to ").
      append(getTarget()).
      append(']').toString();
  }

  @Override
  public int compareTo(final Relation that) {
    return ComparisonChain.start()
        .compare(this.sourcePOSOrdinal, that.sourcePOSOrdinal)
        .compare(this.sourceOffset, that.sourceOffset)
        .compare(this.sourceRelationIndex, that.sourceRelationIndex)
        .compare(this.sourceIndex, that.sourceIndex)
        .compare(this.relationTypeOrdinal, that.sourceRelationIndex)
        .result();
  }
}