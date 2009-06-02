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
 * the copyright notice.
 */
package org.yawni.wn;

import org.yawni.util.CharSequenceTokenizer;

/**
 * A <code>Relation</code> encodes a lexical <em>or</em> semantic relationship between WordNet entities.  A lexical
 * relationship holds between {@link WordSense}s; a semantic relationship holds between {@link Synset}s.
 * Relationships are <em>directional</em>:  the two roles of a relationship are the <em>source</em> and <em>target</em>.
 * Relationships are <em>typed</em>: the type of a relationship is a {@link RelationType}, and can
 * be retrieved via {@link Relation#getType RelationType.getType()}.
 *
 * <p> Note this class used to be called {@code Pointer}.
 *
 * @see Synset
 * @see WordSense
 */
public class Relation implements Comparable<Relation> {
  /**
   * These target* fields are used to avoid paging in the target before it is
   * required, and to prevent keeping a large portion of the database resident
   * once the target has been queried.  The first time they are used, they act as
   * an external key; subsequent uses, in conjunction with 
   * {@link FileBackedDictionary}'s caching mechanism, can be thought of as a
   * {@link java.lang.ref.WeakReference}.
   */
  private final int targetOffset;
  private final int targetIndex;
  private final byte targetPOSOrdinal;

  //
  // Instance variables
  //

  /**
   * The index of this Relation within the array of Relation's in the source Synset.
   * Used in <code>equals</code>.
   */
  private final int index;
  private final RelationTarget source;
  private final byte relationTypeOrdinal;

  //
  // Constructor
  //

  Relation(final int targetOffset, final int targetIndex, final byte targetPOSOrdinal,
    final int index, final RelationTarget source, final byte relationTypeOrdinal) {
    this.targetOffset = targetOffset;
    this.targetIndex = targetIndex;
    this.targetPOSOrdinal = targetPOSOrdinal;
    this.index = index;
    this.source = source;
    this.relationTypeOrdinal = relationTypeOrdinal;
  }

  /** Factory method */
  static Relation makeRelation(final Synset synset, final int index, final CharSequenceTokenizer tokenizer) {
    final byte relationTypeOrdinal = (byte) RelationType.parseKey(tokenizer.nextToken()).ordinal();

    final int targetOffset = tokenizer.nextInt();

    final byte targetPOSOrdinal = (byte) POS.lookup(tokenizer.nextToken()).ordinal();
    final int linkIndices = tokenizer.nextHexInt();
    final int sourceIndex = linkIndices >> 8;
    final int targetIndex = linkIndices & 0xFF;

    final RelationTarget source = Relation.resolveTarget(synset, sourceIndex);
    if (source instanceof WordSense) {
      return new LexicalRelation(targetOffset, targetIndex, targetPOSOrdinal, index, source, relationTypeOrdinal);
    } else if (source instanceof Synset) {
      return new SemanticRelation(targetOffset, targetIndex, targetPOSOrdinal, index, source, relationTypeOrdinal);
    } else {
      throw new IllegalStateException();
    }
  }

  //
  // Accessors
  //
  public RelationType getType() {
    return RelationType.fromOrdinal(relationTypeOrdinal);
  }

  /** A lexical relationship holds between {@link WordSense}s */
  public boolean isLexical() {
    return source instanceof WordSense;
    // else assert instanceof Synset;
  }

  /** A semantic relationship holds between {@link Synset}s */
  public boolean isSemantic() {
    return source instanceof Synset;
    // else assert instanceof WordSense;
  }

  public RelationTarget getSource() {
    return source;
  }

  public RelationTarget getTarget() {
    return Relation.resolveTarget(
        // using source.getSynset() to avoid requiring a local field
        source.getSynset().fileBackedDictionary.getSynsetAt(
          POS.fromOrdinal(targetPOSOrdinal),
          targetOffset),
        targetIndex);
  }

  private static RelationTarget resolveTarget(final Synset synset, final int index) {
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
  public boolean equals(final Object that) {
    return (that instanceof Relation)
      && ((Relation) that).source.equals(this.source)
      && ((Relation) that).index == this.index;
  }

  @Override
  public int hashCode() {
    return source.hashCode() + index;
  }

  @Override
  public String toString() {
    return new StringBuilder("[Relation").
      append(' ').
      append(getType().name()).
      //append("#").
      //append(index).
      append(" from ").
      //append(source).
      append(source).
      //append(" → ").
      append(" to ").
      append(getTarget()).
      append(']').toString();
  }

  /** {@inheritDoc} */
  public int compareTo(final Relation that) {
    // order by src Synset
    // then by 'index' field
    int result;
    result = this.getSource().getSynset().compareTo(that.getSource().getSynset());
    if (result == 0) {
      result = this.index - that.index;
    }
    return result;
  }
}
