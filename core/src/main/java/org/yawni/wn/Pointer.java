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
 * A <code>Pointer</code> encodes a lexical <i>or</i> semantic relationship between WordNet entities.  A lexical
 * relationship holds between {@link WordSense}s; a semantic relationship holds between {@link Synset}s.
 * Relationships are <i>directional</i>:  the two roles of a relationship are the <i>source</i> and <i>target</i>.
 * Relationships are <i>typed</i>: the type of a relationship is a {@link PointerType}, and can
 * be retrieved via {@link Pointer#getType PointerType.getType()}.
 *
 * @see Synset
 * @see WordSense
 */
public final class Pointer implements Comparable<Pointer> {
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
   * The index of this Pointer within the array of Pointer's in the source Synset.
   * Used in <code>equals</code>.
   */
  private final int index;
  private final PointerTarget source;
  private final byte pointerTypeOrdinal;

  //
  // Constructor
  //
  Pointer(final Synset synset, final int index, final CharSequenceTokenizer tokenizer) {
    this.index = index;
    this.pointerTypeOrdinal = (byte) PointerType.parseKey(tokenizer.nextToken()).ordinal();

    this.targetOffset = tokenizer.nextInt();

    this.targetPOSOrdinal = (byte) POS.lookup(tokenizer.nextToken()).ordinal();
    final int linkIndices = tokenizer.nextHexInt();
    final int sourceIndex = linkIndices >> 8;
    this.targetIndex = linkIndices & 0xFF;

    this.source = resolveTarget(synset, sourceIndex);
  }

  //
  // Accessors
  //
  public PointerType getType() {
    return PointerType.fromOrdinal(pointerTypeOrdinal);
  }

  /** A lexical relationship holds between {@link WordSense}s */
  public boolean isLexical() {
    return source instanceof WordSense;
    // else assert instanceof Synset;
  }

  /** A semantic relationship holds between {@link Synset}s */
  public boolean isSemantic() {
    return source instanceof Synset;
  }

  //
  // Targets
  //
  public PointerTarget getSource() {
    return source;
  }

  public PointerTarget getTarget() {
    return resolveTarget(
        // using source.getSynset() to avoid requiring a local field
        source.getSynset().fileBackedDictionary.getSynsetAt(
          POS.fromOrdinal(targetPOSOrdinal),
          targetOffset),
        targetIndex);
  }

  private static PointerTarget resolveTarget(final Synset synset, final int index) {
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
    return (that instanceof Pointer)
      && ((Pointer) that).source.equals(this.source)
      && ((Pointer) that).index == this.index;
  }

  @Override
  public int hashCode() {
    return source.hashCode() + index;
  }

  @Override
  public String toString() {
    return new StringBuilder("[Pointer").
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
  public int compareTo(final Pointer that) {
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
