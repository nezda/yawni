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
 * the copyright notice and this restriction, and label your changes.
 */
package edu.brandeis.cs.steele.wn;
import java.util.NoSuchElementException;

/**
 * Represents the major syntactic categories, or <b>p</b>arts <b>o</b>f
 * <b>s</b>peech used in WordNet.  Each <code>POS</code> has a human-readable
 * label that can be used to print it, and a key by which it can be looked up.
 */
public enum POS {
  //
  // Class variables
  //
  /**
   * Meta-<code>POS</code> representing all parts of speech.  For use in search methods.
   */
  ALL("all POS", null, 0),
  /** <b>NOTE: do not reorder - PointerTypes relies on this</b> */
  /**
   * <ol>
   *   <li> a content word that can be used to refer to a person, place, thing, quality, or action </li>
   *   <li> the word class that can serve as the subject or object of a verb, the object of a preposition, or in apposition </li>
   * <ol>
   */
  NOUN("noun", "n", 1),
  /**
   * <ol>
   *   <li> the word class that serves as the predicate of a sentence </li>
   *   <li> a content word that denotes an action, occurrence, or state of existence </li>
   * <ol>
   */
  VERB("verb", "v", 2),
  /**
   * <ol>
   *   <li> a word that expresses an attribute of something </li>
   *   <li> the word class that qualifies nouns </li>
   * <ol>
   */
  ADJ("adjective", "a", 3),
  /**
   * <ol>
   *   <li> the word class that qualifies verbs or clauses </li>
   *   <li> a word that modifies something other than a noun </li>
   * <ol>
   */
  ADV("adverb", "r", 4),
  /**
   * Basically a sub-<code>POS</code> of <code>ADJ</code>.
   * aka "adjective satellite", "ADJSAT"
   */
  SAT_ADJ("satellite adjective", "s", 5);

  private static final POS[] VALUES = values();

  // NOTE: ordinal+1 == wnCode
  static POS fromOrdinal(final byte ordinal) {
    return VALUES[ordinal];
  }

  /** A list of all <code>POS</code>s <b>except {@link POS#SAT_ADJ}</b> which doesn't
   * have its own data files.
   */
  public static final POS[] CATS = {NOUN, VERB, ADJ, ADV};

  //
  // Instance implementation
  //
  private final String label;
  private final String toString;
  private final String key;
  private final int wnCode;

  POS(final String label, final String key, final int wnCode) {
    this.label = label;
    this.key = key;
    this.wnCode = wnCode;
    this.toString = new StringBuffer("[POS ").append(label).append("]").toString();
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

  /** Return the <code>POS</code> whose key matches <var>key</var>.
   * @exception NoSuchElementException If <var>key</var> doesn't name any <code>POS</code>.
   */
  public static POS lookup(final CharSequence key) {
    for (final POS pos : CATS) {
      if (pos.key.contentEquals(key)) {
        return pos;
      }
    }
    throw new NoSuchElementException("unknown POS \"" + key + "\"");
  }
}
