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

import org.yawni.util.CharSequences;
import java.math.BigInteger;
import java.util.Iterator;
import com.google.common.collect.UnmodifiableIterator;
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
// * subclass AbstractList<CharSequence> would be slicker
//   - just make new buffer per get(nextState) to ensure thread-safe
// * for any practical numbers, BigInteger is overkill - could just use an int and some bit manipulation
// * this class is NOT threadsafe - mutates internal buffer - synchronize ? go immutable
class GetIndex extends UnmodifiableIterator<CharSequence> implements Iterable<CharSequence> {
  private final String givenForm;
  private final POS pos;
  private final Morphy morphy;
  private static final char[] TO_ALTERNATE = new char[]{ '_', '-' };
  private final BigInteger numAlternations;
  private final BigInteger initialState;
  private final StringBuilder buffer;
  private BigInteger nextState;

  // could vary behavior based on POS, Morphy, but actually dont'
  GetIndex(final String searchStr, final POS pos, final Morphy morphy) {
    this.givenForm = checkNotNull(searchStr);
    this.pos = pos == POS.SAT_ADJ ? POS.ADJ : pos;
    this.morphy = morphy;
    // count candidates
    int numCandidates = 0;
    for (int i = findNextVictimIndex(searchStr, 0);
      i >= 0;
      i = findNextVictimIndex(searchStr, i + 1)) {
      //TODO could set initialState according to the given values (e.g., '_' → 0, '-' → 1)
      if (searchStr.charAt(i) == '_') {
      } else if (searchStr.charAt(i) == '-') {
      }  else {
        throw new IllegalStateException();
      }
      numCandidates++;
    }
    this.initialState = BigInteger.ZERO;
    this.nextState = initialState;
    // - there will be 2^n total variants where -- 2 because |{'-','_'}| == 2
    this.numAlternations = BigInteger.valueOf(TO_ALTERNATE.length).pow(numCandidates);
    this.buffer = new StringBuilder(searchStr);
  }

  public int size() {
    return numAlternations.intValue();
  }
  
  private void reset() {
    nextState = initialState;
    buffer.replace(0, givenForm.length(), givenForm);
  }

  public Iterator<CharSequence> iterator() {
    reset();
    return new GetIndex(givenForm, pos, morphy);
  }

  public boolean hasNext() {
    return nextState.compareTo(numAlternations) < 0;
  }

  public CharSequence next() {
    // TODO
    // ? does any term in WordNet actually have n dashes (say n=2+) ?
    // - strip dashes
    // ~ handle single token as special case
    // - create alternations for "F.D." → "F. D.", and maybe "FD" → "F. D."
    set(givenForm, buffer, nextState);
    nextState = nextState.add(BigInteger.ONE);
    return buffer;
  }

  /**
   * use bits of nextState to set candidate positions
   * @return convenience return
   */
  private static CharSequence set(final String givenForm, final StringBuilder buffer, final BigInteger nextState) {
    for (int stateIdx = 0, i = findNextVictimIndex(givenForm, 0);
      i >= 0;
      i = findNextVictimIndex(givenForm, i + 1)) {
      // base2(nextState)[stateIdx] == 1
//      if (nextState.testBit(stateIdx)) {
//        buffer.setCharAt(i, '-');
//      } else {
//        buffer.setCharAt(i, '_');
//      }
      buffer.setCharAt(i, TO_ALTERNATE[baseNDigitX(nextState, TO_ALTERNATE.length, stateIdx)]);
      stateIdx++;
    }
    return buffer;
  }

  /** find next candidate hyphen/space index starting at i */
  private static int findNextVictimIndex(final CharSequence str, final int fromIndex) {
    return CharSequences.indexIn(str, fromIndex, TO_ALTERNATE);
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
    return baseNDigitX(BigInteger.valueOf(number), radix, x);
  }

  static int baseNDigitX(final BigInteger number, final int radix, final int x) {
    checkArgument(radix > 0);
    checkArgument(x >= 0);
    final BigInteger bigRadix = BigInteger.valueOf(radix);
    int idx = -1;
    BigInteger num = number;
    BigInteger[] divRem;
    do {
      divRem = num.divideAndRemainder(bigRadix);
      num = divRem[0];
      idx++;
      if (divRem[0].equals(BigInteger.ZERO) && idx < x) {
        //throw new IndexOutOfBoundsException(number+" has no digit "+x+" (not large enough)");
        return 0;
      }
    } while (idx < x);
    return divRem[1].intValue();
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