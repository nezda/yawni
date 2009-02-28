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
package org.yawni.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.yawni.util.CharSequences.*;

/**
 * {@code CharSequenceTokenizer} is used to break a string apart into tokens.  Lighter
 * than a {@link java.util.Scanner}, more features than {@link java.util.StringTokenizer}, with full
 * support for {@link CharSequence}s.
 *
 * <p> Borrowed some code from Apache Harmony java.util.StringTokenizer
 */
public final class CharSequenceTokenizer implements Iterator<CharSequence> {
  private final CharSequence string;
  private String delimiters;
  private int position;

  /**
   * Constructs a new <code>CharSequenceTokenizer</code> for {@code string} using whitespace as
   * the delimiter.
   *
   * @param string the CharSequence to be tokenized
   */
  public CharSequenceTokenizer(final CharSequence string) {
    this(string, " \t\n\r\f");
  }

  /**
   * Constructs a new <code>CharSequenceTokenizer</code> for {@code string} using the specified
   * {@code delimiters} and returning delimiters as tokens when specified.
   *
   * @param string the string to be tokenized
   * @param delimiters the delimiters to use
   */
  public CharSequenceTokenizer(final CharSequence string, final String delimiters) {
    if (string != null) {
      this.string = string;
      this.delimiters = delimiters;
      this.position = 0;
    } else {
      throw new NullPointerException();
    }
  }

  /**
   * Returns the number of unprocessed tokens remaining in the string.
   *
   * @return number of tokens that can be retreived before an exception will
   *         result
   */
  public int countTokens() {
    return countTokens(string, position, string.length(), delimiters);
  }

  /**
   * Returns the number of tokens in {@code string} separated by
   * {@code delimiters} starting at {@code position}.
   *
   * @param string the string to be tokenized
   * @param delimiters the delimiters to use
   * @return number of tokens that can be retreived before an exception will
   *         result
   */
  public static int countTokens(final CharSequence string, final String delimiters) {
    return countTokens(string, 0, string.length(), delimiters);
  }

  /**
   * Returns the number of tokens in {@code string} separated by
   * {@code delimiters} starting at {@code position}.
   *
   * @param string the string to be tokenized
   * @param position in string
   * @param length after position
   * @param delimiters the delimiters to use
   * @return number of tokens that can be retreived before an exception will
   *         result
   */
  public static int countTokens(
    final CharSequence string,
    final int position,
    final int length,
    final String delimiters) {
    int count = 0;
    boolean inToken = false;
    for (int i = position; i < length; i++) {
      final char ci = string.charAt(i);
      if (delimiters.indexOf(ci, 0) >= 0) {
        if (inToken) {
          count++;
          inToken = false;
        }
      } else {
        inToken = true;
      }
    }
    if (inToken) {
      count++;
    }
    return count;
  }

  /**
   * Returns true if unprocessed tokens remain.
   *
   * @return true if unprocessed tokens remain
   */
  public boolean hasNext() {
    return hasMoreTokens();
  }

  /**
   * Returns true if unprocessed tokens remain.
   *
   * @return true if unprocessed tokens remain
   */
  public boolean hasMoreTokens() {
    final int length = string.length();
    if (position < length) {
      // otherwise find a character which is not a delimiter
      for (int i = position; i < length; i++) {
        if (delimiters.indexOf(string.charAt(i), 0) == -1) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Returns the next token in the string as an CharSequence.
   *
   * @return next token in the string as an CharSequence
   * @throws NoSuchElementException if no tokens remain
   */
  public CharSequence next() {
    return nextToken();
  }

  /**
   * @throws UnsupportedOperationException
   */
  public void remove() {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the next token in the string as a CharSequence.
   * @return next token in the string as a CharSequence
   * @throws NoSuchElementException if no tokens remain
   */
  public CharSequence nextToken() {
    final int s = scanToTokenStart();
    final int e = scanToTokenEnd();
    return string.subSequence(s, e);
  }

  // TODO better to use a mutable object with a little friendlier interface
  // like say CharSequence: MutableCharSequence
  // if we overloaded equals() & hashCode(), make it comparable with
  // String, etc. - still will be order dep (us.equals(them) vs. them.equals(us)
  // high-performance, high-risk method
//  public void nextToken(int[] startEnd) {
//    final int s = scanToTokenStart();
//    final int e = scanToTokenEnd();
//    startEnd[0] = s;
//    startEnd[1] = e;
//  }

  private int scanToTokenStart() {
    final int length = string.length();
    if (position < length) {
      while (position < length && delimiters.indexOf(string.charAt(position)) >= 0) {
        position++;
      }
      return position;
    }
    throw new NoSuchElementException();
  }

  private int scanToTokenEnd() {
    final int length = string.length();
    if (position < length) {
      for (position++; position < length; position++) {
        if (delimiters.indexOf(string.charAt(position)) >= 0) {
          return position;
        }
      }
      assert length == position;
      return length;
    }
    throw new NoSuchElementException();
  }

  public void skipNextToken() {
    scanToTokenStart();
    scanToTokenEnd();
  }

  /**
   * Returns the next token in the string as a CharSequence. The delimiters used are
   * changed to the specified delimiters.
   *
   * @param delims the new delimiters to use
   * @return next token in the string as a CharSequence
   * @exception NoSuchElementException if no tokens remain
   */
  public CharSequence nextToken(final String delims) {
    this.delimiters = delims;
    return nextToken();
  }

//  public byte nextByte() {
//    return Byte.parseByte(nextToken());
//  }

//  public int nextShort() {
//    return Short.parseShort(nextToken());
//  }

  public int nextInt() {
    return nextInt(10);
  }

  public int nextInt(final int radix) {
    final int s = scanToTokenStart();
    final int e = scanToTokenEnd();
    return parseInt(string, s, e, radix);
  }

  public int nextHexInt() {
    return nextInt(16);
  }

  public long nextLong() {
    final int s = scanToTokenStart();
    final int e = scanToTokenEnd();
    return parseLong(string, s, e, 10);
  }
}
