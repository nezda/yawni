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

import java.util.AbstractList;
import org.yawni.util.CharSequences;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * Function object port of {@code getindex()} 'smart' search variants function
 * in {@code search.c}: creates all combinations of spaces and dashes
 * for a given string form, e.g.,
 * input: "
 *
 * general outline:
 * <ul>
 *   <li> nextState encodes the current state of the buffer
 *   <li> method next() transforms (i.e., mutates) to the next form
 * </ul>
 *
 * TODO
 * * alternations involving periods, e.g.,
 *   "F.D." → "F. D.", and maybe "FD" → "F. D."
 *   - add periods to all upper searchstr's
 *   - ... see commented test cases in {@link MorphyTest}
 */
class GetIndex extends AbstractList<CharSequence> {
  private final String givenForm;
  private static final char[] DEFAULT_TO_ALTERNATE = new char[]{ '_', '-' };
  private final char[] toAlternate;
  private final int numAlternations;

  // backwards compat hack
  // could vary behavior based on POS, Morphy, but actually dont'
  GetIndex(final String searchStr, final POS pos, final Morphy morphy) {
    this(searchStr, DEFAULT_TO_ALTERNATE);
  }

  // could vary behavior based on POS, Morphy, but actually dont'
  GetIndex(final String searchStr, final char[] toAlternate) {
    this.givenForm = checkNotNull(searchStr);
    this.toAlternate = checkNotNull(toAlternate);
    checkArgument(toAlternate.length > 0);
    // count candidates
    int numCandidates = 0;
    for (int i = findNextVictimIndex(toAlternate, searchStr, 0);
      i >= 0;
      i = findNextVictimIndex(toAlternate, searchStr, i + 1)) {
      //TODO could set initialState according to the given values (e.g., '_' → 0, '-' → 1)
      //NOTE, commented logic below assumes toAlternate == DEFAULT_TO_ALTERNATE
//      if (searchStr.charAt(i) == '_') {
//      } else if (searchStr.charAt(i) == '-') {
//      }  else {
//        throw new IllegalStateException();
//      }
      numCandidates++;
    }
    // - there will be 2^n total variants where -- 2 because |{'-','_'}| == 2
    this.numAlternations = (int) Math.pow(toAlternate.length, numCandidates);
  }

	@Override
  public int size() {
    return numAlternations;
  }

	@Override
  public CharSequence get(int nextState) {
    // TODO
    // ? does any term in WordNet actually have n dashes (say n=2+) ?
    // - strip dashes
    // ~ handle single token as special case
    // - create alternations for "F.D." → "F. D.", and maybe "FD" → "F. D."
    return set(givenForm, toAlternate, new StringBuilder(givenForm), nextState);
  }

  /**
   * use bits of nextState to set candidate positions
   * @return convenience return
   */
  private static CharSequence set(
    final String givenForm,
    final char[] toAlternate,
    final StringBuilder buffer,
    final int nextState) {
    for (int stateIdx = 0, i = findNextVictimIndex(toAlternate, givenForm, 0);
      i >= 0;
      i = findNextVictimIndex(toAlternate, givenForm, i + 1)) {
      buffer.setCharAt(i, toAlternate[baseNDigitX(nextState, toAlternate.length, stateIdx)]);
      stateIdx++;
    }
    return buffer;
  }

  /** find next candidate hyphen/space index starting at i */
  private static int findNextVictimIndex(final char [] toAlternate, final CharSequence str, final int fromIndex) {
    return CharSequences.indexIn(str, fromIndex, toAlternate);
  }

  @Override
  public int hashCode() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean equals(Object that) {
    throw new UnsupportedOperationException();
  }

  static int baseNDigitX(final int number, final int radix, final int x) {
    checkArgument(radix > 0);
    checkArgument(x >= 0);
    int idx = -1;
    int num = number;
    int div, rem;
    do {
      div = num / radix;
      rem = num % radix;
      num = div;
      idx++;
      if (div == 0 && idx < x) {
        //throw new IndexOutOfBoundsException(number+" has no digit "+x+" (not large enough)");
        return 0;
      }
    } while (idx < x);
    return rem;
  }

  /**
   * XXX unused original notes XXX
   *
   * 'Smart' search of WordNet index file.  Attempts to find word in index file
   * by trying different transformations including:
   * <ul>
   *   <li> replace hyphens with underscores (spaces) </li>
   *   <li> replace underscores (spaces) with hyphens </li>
   *   <li> strip hyphens and underscores (spaces) XXX dangerous to just strip spaces </li>
   *   <li> strip periods </li>
   * </ul>
   * ??? Typically this operates on the output(s) of <code>morphstr()</code>.
   */
  private void getindex(String searchstr, final POS pos) {
    // better strategy:
    // - try variants where each underscore (space) switched to
    //   a dash and each dash is switched to an underscore (space)
    //   in turn to create ALL possible variants
    //   - numDashes * numUnderscores variants
    //  * def do "X.[^_]" → "X. " (e.g., "F.D." → "F. D.")
    //  * consider similar for periods so "U.S.A." produces "U.S.A"
    // - implement algorithm as a series of modifcations of a single StringBuilder:
    //   * make mod, issue search (morphy.is_defined()) and store result

    // remove ALL spaces AND hyphens from search string
    // [3] is no underscores (spaces) or dashes
    //FIXME this strategy is a little crazy
    // * only allow this if a version with dashes exists ?
//    strings[3] = strings[3].replace("_", "").replace("-", "");
    // remove ALL periods
    // [4] is no periods
    //FIXME this strategy is a also little crazy ("u.s." → "us")
//    strings[4] = strings[4].replace(".", "");
  }
}