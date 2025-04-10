/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.yawni.wordnet;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.SignedBytes;

import java.util.NoSuchElementException;

/**
 * Represents the major syntactic categories, or <b>p</b>arts <b>o</b>f
 * <b>s</b>peech, used in WordNet.  Each {@code POS} has a human-readable
 * label that can be used to print it, and a key by which it can be looked up.
 */
public enum POS {
  //
  // Class variables
  //
  /**
   * Meta-{@code POS} representing all parts of speech; for use in search methods.
   */
  ALL("all POS", Character.MIN_VALUE, 0),
  /** <b>NOTE: do not reorder - RelationTypes relies on this</b> */
  /**
   * <ol>
   *   <li> a content word that can be used to refer to a person, place, thing, quality, or action </li>
   *   <li> the word class that can serve as the subject or object of a verb, the object of a preposition, or in apposition </li>
   * </ol>
   */
  NOUN("noun", 'n', 1),
  /**
   * <ol>
   *   <li> the word class that serves as the predicate of a sentence </li>
   *   <li> a content word that denotes an action, occurrence, or state of existence </li>
   * </ol>
   */
  VERB("verb", 'v', 2),
  /**
   * Adjective:
   * <ol>
   *   <li> a word that expresses an attribute of something </li>
   *   <li> the word class that qualifies nouns </li>
   * </ol>
   */
  ADJ("adjective", 'a', 3),
  /**
   * Adverb:
   * <ol>
   *   <li> the word class that qualifies verbs or clauses </li>
   *   <li> a word that modifies something other than a noun </li>
   * </ol>
   */
  ADV("adverb", 'r', 4),
  /**
   * "Satellite adjective";
   * Basically a sub-{@code POS} of {@code ADJ}.
   * aka "adjective satellite", "ADJSAT"
   */
  SAT_ADJ("satellite adjective", 's', 5);

  private static final POS[] VALUES = values();

  // NOTE: ordinal+1 == wnCode
  static POS fromOrdinal(final byte ordinal) {
    return VALUES[ordinal];
  }

  // NOTE: ordinal+1 == wnCode
  static POS fromOrdinalChar(final char ordinalChar) {
    final int ordinal = ordinalChar - '0';
    return VALUES[ordinal];
  }

  static {
    // FIXME ad-hoc inline test
    assert ALL == fromOrdinalChar('0');
    assert NOUN == fromOrdinalChar('1');
    assert VERB == fromOrdinalChar('2');
    assert ADJ == fromOrdinalChar('3');
    assert ADV == fromOrdinalChar('4');
  }

  /**
   * A list of all {@code POS}s <b>except {@link POS#SAT_ADJ}</b> which doesn't
   * have its own data files.
   */
  public static final ImmutableList<POS> CATS = ImmutableList.of(NOUN, VERB, ADJ, ADV);

  //
  // Instance implementation
  //
  private final String label;
  private final String toString;
  private final char keyChar;
  private final int wnCode;
  private final byte byteOrdinal;

  POS(final String label, final char key, final int wnCode) {
    this.label = label;
    this.keyChar = key;
    this.wnCode = wnCode;
    this.toString = "[POS " + label + "]";
    this.byteOrdinal = SignedBytes.checkedCast(wnCode);
  }

  //
  // Object methods
  //

  @Override
  public String toString() {
    return toString;
  }

  //
  // Accessor
  //
  /** Return a label intended for textual presentation. */
  public String getLabel() {
    return label;
  }

  /** The integer used in the original C WordNet APIs. */
  int getWordNetCode() {
    return wnCode;
  }

  byte getByteOrdinal() {
    assert byteOrdinal == ordinal();
    return byteOrdinal;
  }

  /**
   * Return the {@code POS} whose key matches {@code key}.
   * @throws NoSuchElementException If {@code key} doesn't name any {@code POS}.
   */
  public static POS lookup(final CharSequence key) {
    if (key.length() == 1) {
      return lookup(key.charAt(0));
    }
    throw new NoSuchElementException("unknown POS \"" + key + "\"");
  }

  /**
   * Return the {@code POS} whose key matches {@code key}.
   * @throws NoSuchElementException If {@code key} doesn't name any {@code POS}.
   */
  public static POS lookup(final char keyChar) {
    for (final POS pos : CATS) {
      if (pos.keyChar == keyChar) {
        return pos;
      }
    }
    throw new NoSuchElementException("unknown POS \"" + keyChar + "\"");
  }
}