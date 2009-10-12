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
package org.yawni.wn;

import java.math.BigInteger;
import java.util.*;

/**
 * Function object port of <code>getindex()</code> search variants function
 * in <code>search.c</code>.
 * TODO implement ListIterator<CharSequence>, ListIterator<Word>
 */
// general plan
// * offset is the step we're in in the set of transformations to undergo
// * method next() mutates the outward appearance of this object to the next state
class GetIndex implements CharSequence, Iterable<CharSequence>, Iterator<CharSequence> {
  private final String base;
  private final POS pos;
  private final Morphy morphy;
  private final BigInteger numAlternations;
  private final BigInteger initialState;
  private final StringBuilder buffer;
  private BigInteger nextState;

  GetIndex(final String searchStr, final POS pos, final Morphy morphy) {
    this.base = Morphy.searchNormalize(searchStr);
//    this.base = searchStr;
    this.pos = pos == POS.SAT_ADJ ? POS.ADJ : pos;
    this.morphy = morphy;
    int numCandidates = 0;
    BigInteger initialState = BigInteger.ZERO;
    for (int i = next(searchStr, 0);
      i >= 0;
      i = next(searchStr, i + 1)) {
      if (searchStr.charAt(i) == '_') {
        // initialState[numCandidates] = 0
      } else if (searchStr.charAt(i) == '-') {
        // initialState = initialState.setBit(numCandidates);
      }  else {
        throw new IllegalStateException();
      }
      ++numCandidates;
    }
    this.initialState = initialState;
    this.nextState = initialState;
    // - there will be 2^n total variants where -- 2 because |{'-','_'}| == 2
    this.numAlternations = BigInteger.valueOf(1 << numCandidates);
    this.buffer = new StringBuilder(searchStr);
//    getindex(searchStr, pos);
  }

  private void reset() {
    nextState = initialState;
    buffer.replace(0, base.length(), base);
  }

  public char charAt(int i) {
    return buffer.charAt(i);
  }

  public int length() {
    return base.length();
  }

  public int size() {
    return numAlternations.intValue();
  }

  public CharSequence subSequence(final int s, final int e) {
    return buffer.subSequence(s, e);
  }

  public Iterator<CharSequence> iterator() {
    reset();
    return new GetIndex(base, pos, morphy);
  }

  public boolean hasNext() {
    return nextState.compareTo(numAlternations) < 0;
  }

  public CharSequence next() {
    // TODO
    // - does any term in WordNet actually have n dashes (say n=2+) ?
    // - strip dashes
    // - handle single token as special case
    // - create alternations for "F.D." -> "F. D.", and maybe "FD" -> "F. D."
    int candIdx = 0;
    for (int i = next(base, 0);
      i >= 0;
      i = next(base, i + 1)) {
      if (nextState.testBit(candIdx)) {
        buffer.setCharAt(i, '-');
      } else {
        buffer.setCharAt(i, '_');
      }
      candIdx++;
    }
    nextState = nextState.add(BigInteger.ONE);
    return this;
  }

  public void remove() {
    throw new UnsupportedOperationException();
  }

  // Comparable<CharSequence>
  //public int compareTo(final CharSequence that) {
  //  throw new UnsupportedOperationException();
  //}
  
  @Override
  public int hashCode() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean equals(Object that) {
    throw new UnsupportedOperationException();
  }

  // note this is part of the CharSequence interface
  @Override
  public String toString() {
    return buffer.toString();
  }

  private static int next(final CharSequence str, final int i) {
    final int s = indexOf(str, '_', i);
    final int h = indexOf(str, '-', i);
    if (s < 0) {
      return h;
    } else if (h < 0) {
      return s;
    } else {
      return Math.min(s, h);
    }
  }

  private static int indexOf(final CharSequence str, final char c, final int i) {
    if (str instanceof String) {
      return ((String)str).indexOf(c, i);
    } else {
      for (int j = i, n = str.length(); j < n; j++) {
        if (c == str.charAt(j)) {
          return j;
        }
      }
      return -1;
    }
  }

  /**
   * 'Smart' search of WordNet index file.  Attempts to find word in index file
   * by trying different transformations including:
   * <ul>
   *   <li> replace hyphens with underscores (spaces) </li>
   *   <li> replace underscores (spaces) with hyphens </li>
   *   <li> strip hyphens and underscores (spaces) XXX dangerous to just strip spaces </li>
   *   <li> strip periods </li>
   * </ul>
   * ??? Typically this operates on the output(s) of <code>morphstr()</code>.
   *
   * <p>Port of <code>search.c</code>'s function <code>getindex()</code> function.
   *
   * TODO
   * - add periods to all upper searchstr's
   * - ...
   * - see commented test cases in {@link MorphyTest}
   */
  private void getindex(String searchstr, final POS pos) {
    // typical input:
    // needs "-" -> ""  { POS.NOUN, "fire-bomb",   "firebomb" }, // WN does this
    // needs "-" -> " " { POS.NOUN, "letter-bomb", "letter bomb" }, // WN does do this
    // needs "" -> " "  { POS.NOUN, "letterbomb", "letter bomb" }, // WN doesn't do this
    // - requires prefix AND suffix match or forgiving match -- slippery slope "I ran" -> "Iran"
    // needs "" -> "." AND "X.X." -> "X. X." { "FD Roosevelt", "F.D. Roosevelt", "F. D. Roosevelt"} // WN doesn't do this

    //FIXME this strategy fails for special 3+ word
    // collocations like "wild-goose chase" and "internal-combustion engine"
    // when the query has no dashes (common).
    //
    // better strategy:
    // - try variants where each underscore (space) switched to
    //   a dash and each dash is switched to an underscore (space)
    //   in turn to create ALL possible variants
    //   - numDashes * numUnderscores variants
    //  * def do "X.[^_]" -> "X. " (e.g., "F.D." -> "F. D.")
    //  * consider similar for periods so "U.S.A." produces "U.S.A"
    // ? consider implementing algorithm as a series of modifcations of
    //   a single StringBuilder:
    //   * make mod, issue search (morphy.is_defined()) and store result

    //FIXME short circuit this if searchstr contains
    //no underscores (spaces), dashes, or periods as this
    //algorithm will do nothing
    //FIXME FIXME not true for insertion-variants
    final int firstUnderscore = searchstr.indexOf('_');
    final int firstDash = searchstr.indexOf('-');
    final int firstPeriod = searchstr.indexOf('.');
    if (firstUnderscore < 0 && firstDash < 0 && firstPeriod < 0) {
      return;
    }

    // vector of search strings
    final int MAX_FORMS = 5;
    final String[] strings = new String[MAX_FORMS];

    searchstr = searchstr.toLowerCase();
    for (int i = 0; i < MAX_FORMS; i++) {
      strings[i] = searchstr;
    }
    // [0] is original string (lowercased)

    // [1] is ALL underscores (spaces) to dashes
    strings[1] = strings[1].replace('_', '-');
    // [2] is ALL dashes to underscores (spaces)
    strings[2] = strings[2].replace('-', '_');

    // remove ALL spaces AND hyphens from search string
    // [3] is no underscores (spaces) or dashes
    //FIXME this strategy is a little crazy
    // * only allow this if a version with dashes exists ?
    strings[3] = strings[3].replace("_", "").replace("-", "");
    // remove ALL periods
    // [4] is no periods
    strings[4] = strings[4].replace(".", "");

    int j = -1;
    for (final String s : strings) {
      System.err.println("s[" + (++j) + "]: " + s);
    }

    // Get offset of first entry.  Then eliminate duplicates
    // and get offsets of unique strings.

    List<Word> words = null;
    Word word = morphy.is_defined(strings[0], pos);
    if (word != null) {
      words.add(word);
    }

    for (int i = 1; i < MAX_FORMS; i++) {
      if (strings[i] == null || strings[0].equals(strings[i])) {
        continue;
      }
      word = morphy.is_defined(strings[i], pos);
      if (word != null) {
        words.add(word);
      }
    }
    // cannot return duplicate Word since each is tied a specific String
  }
}