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

import com.google.common.base.CharMatcher;
import java.util.NoSuchElementException;

import static org.yawni.util.CharSequences.*;

/**
 * {@code AbstractCharSequenceTokenizer}s are used to break a string apart into tokens based on sequence of 1 or more of
 * of n delimiter {@code char}s.
 * Lighter than a {@link java.util.Scanner}, more features than {@link java.util.StringTokenizer java.util.StringTokenizer}, with full
 * support for {@link CharSequence}s.
 *
 * <p> Borrowed some code from Apache Harmony {@link java.util.StringTokenizer}
 */
public abstract class AbstractCharSequenceTokenizer {
  protected static final String DEFAULT_DELIMITERS = " \t\n\r\f";
  protected final CharSequence string;
  protected String delimiters;
  private int position;

  /**
   * Constructs a new {@code AbstractCharSequenceTokenizer} for {@code string} using whitespace as
   * the delimiter.
   * @param string the CharSequence to be tokenized
   */
  public AbstractCharSequenceTokenizer(final CharSequence string) {
    this(string, DEFAULT_DELIMITERS);
  }

  /**
   * Constructs a new {@code AbstractCharSequenceTokenizer} for {@code string} using the specified
   * {@code delimiters} and returning delimiters as tokens when specified.
   * @param string the string to be tokenized
   * @param delimiters the delimiters to use
   */
  public AbstractCharSequenceTokenizer(final CharSequence string, final String delimiters) {
    this(string, 0, delimiters);
  }

  /**
   * Constructs a new {@code AbstractCharSequenceTokenizer} for {@code string} using the specified
   * {@code delimiters} and returning delimiters as tokens when specified.
   * @param string the string to be tokenized
   * @param position position in string
   * @param delimiters the delimiters to use
   */
  public AbstractCharSequenceTokenizer(final CharSequence string, final int position, final String delimiters) {
    if (string == null || delimiters == null) {
      throw new NullPointerException();
    }
    if (position < 0 || position > string.length()) {
      throw new IndexOutOfBoundsException("invalid position "+position+" for string of length: "+string.length());
    }
    this.string = string;
    this.delimiters = delimiters;
    this.position = position;
  }

  /**
   * Returns the number of unprocessed tokens remaining in the string.
   * @return number of tokens that can be retrieved before an exception will result
   */
  public final int countTokens() {
    return countTokens(string, position, string.length(), delimiters);
  }

  /**
   * Returns the number of tokens in {@code string} separated by
   * the default delimiters.
   * @param string the string to be tokenized
   * @return number of tokens that can be retrieved before an exception will result
   */
  public static int countTokens(final CharSequence string) {
    return countTokens(string, 0, string.length(), DEFAULT_DELIMITERS);
  }

  public static int countTokens(final CharSequence string, CharMatcher delimiter) {
    return delimiter.countIn(string) + 1;
  }

  /**
   * Returns the number of tokens in {@code string} separated by
   * {@code delimiters}.
   * @param string the string to be tokenized
   * @param delimiters the delimiters to use
   * @return number of tokens that can be retrieved before an exception will result
   */
  public static int countTokens(final CharSequence string, final String delimiters) {
    return countTokens(string, 0, string.length(), delimiters);
  }

  /**
   * Returns the number of tokens in {@code string} separated by
   * {@code delimiters} starting at {@code position}.
   * @param string the string to be tokenized
   * @param position position in string
   * @param length length after position
   * @param delimiters the delimiters to use
   * @return number of tokens that can be retrieved before an exception will result
   */
  public static int countTokens(
    final CharSequence string,
    final int position,
    final int length,
    final String delimiters) {
    int count = 0;
    int i = position;
    while (true) {
      final int s = scanToTokenStart(string, i, delimiters);
      if (s == length) {
        break;
      }
      final int e = scanToTokenEnd(string, s, delimiters);
      count++;
      i = e;
    }
    return count;
  }

  /**
   * Returns true if unprocessed tokens remain.
   * @return true if unprocessed tokens remain
   */
  public final boolean hasNext() {
    return hasMoreTokens();
  }

  /**
   * Returns true if unprocessed tokens remain.
   * @return true if unprocessed tokens remain
   */
  public final boolean hasMoreTokens() {
    return scanToTokenStart(string, position, delimiters) != string.length();
  }

  /**
   * Returns the next token in the string as an CharSequence.
   * @return next token in the string as an CharSequence
   * @throws NoSuchElementException if no tokens remain
   */
  public CharSequence next() {
    return nextToken();
  }

  /**
   * Returns the next token in the string as a CharSequence.
   * @return next token in the string as a CharSequence
   * @throws NoSuchElementException if no tokens remain
   */
  public String nextToken() {
    final int s = scanToTokenStart();
    final int e = scanToTokenEnd();
    return string.subSequence(s, e).toString();
  }

  /**
   * Returns the next token in the string as a CharSequence. The delimiters used are
   * changed to the specified delimiters.
   * @param delims the new delimiters to use
   * @return next token in the string as a CharSequence
   * @throws NoSuchElementException if no tokens remain
   */
  public CharSequence nextToken(final String delims) {
    this.delimiters = delims;
    return nextToken();
  }

  public boolean hasPrevious() {
    final int e = scanBackToTokenLast(string, position, delimiters);
    return e >= 0;
  }

  public CharSequence previous() {
    return previousToken();
  }

  public String previousToken() {
    final int e = scanBackToTokenLast();
    final int s = scanBackToTokenFirst();
    return string.subSequence(s, e + 1).toString();
  }

  /**
   * Advance before previous token without generating any objects; same semantics
   * as calling {@link #previous previous()}
   */
  public final void skipPreviousToken() {
    scanBackToTokenLast();
    scanBackToTokenFirst();
  }

  /**
   * @throws UnsupportedOperationException
   */
  public final void remove() {
    throw new UnsupportedOperationException();
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

  protected final int scanToTokenStart() {
    position = scanToTokenStart(string, position, delimiters);
    if (position == string.length()) {
      throw new NoSuchElementException();
    }
    return position;
  }

  protected static int scanToTokenStart(
    final CharSequence string,
    int position,
    final String delimiters) {
    final int length = string.length();
    if (position < length) {
      while (position < length && delimiters.indexOf(string.charAt(position)) != -1) {
        position++;
      }
      return position;
    }
    return length;
  }

  protected final int scanToTokenEnd() {
    position = scanToTokenEnd(string, position, delimiters);
    if (position < 0) {
      throw new NoSuchElementException();
    }
    return position;
  }

  protected static int scanToTokenEnd(
    final CharSequence string,
    int position,
    final String delimiters) {
    final int length = string.length();
    if (position < length) {
      for (position++; position < length; position++) {
        if (delimiters.indexOf(string.charAt(position)) != -1) {
          break;
        }
      }
      return position;
    }
    return -1;
  }

  protected final int scanBackToTokenFirst() {
    position = scanBackToTokenFirst(string, position, delimiters);
    if (position < 0) {
      throw new NoSuchElementException();
    }
    return position;
  }

  // precondition: on last of curr line (can include 0)
  // postcondition: on first of curr line (can include 0)
  protected static int scanBackToTokenFirst(
    final CharSequence string,
    int position,
    final String delimiters) {
    if (position >= 0) {
      while (position > 0) {
        // if prev delim, break
        if (delimiters.indexOf(string.charAt(position - 1)) != -1) {
          break;
        }
        position--;
      }
      return position;
    }
    return -1;
  }

  protected final int scanBackToTokenLast() {
    position = scanBackToTokenLast(string, position, delimiters);
    if (position < 0) {
      throw new NoSuchElementException();
    }
    return position;
  }

  // precondition: on first of curr line
  // postcondition: on last of prev line (can include 0)
  protected static int scanBackToTokenLast(
    final CharSequence string,
    int position,
    final String delimiters) {
    if (position > 0) {
      while (position > 0) {
        position--;
        // if not on delim, break
        if (delimiters.indexOf(string.charAt(position)) == -1) {
          return position;
        }
      }
    }
    return -1;
  }

  /**
   * Advance past next token without generating any objects; same semantics
   * as calling {@link #next next()}
   */
  public final void skipNextToken() {
    scanToTokenStart();
    scanToTokenEnd();
  }

//  public byte nextByte() {
//    return Byte.parseByte(nextToken());
//  }

//  public int nextShort() {
//    return Short.parseShort(nextToken());
//  }

  public final int nextInt() {
    return nextInt(10);
  }

  public final int nextInt(final int radix) {
    final int s = scanToTokenStart();
    final int e = scanToTokenEnd();
    return parseInt(string, s, e, radix);
  }

  public final int nextHexInt() {
    return nextInt(16);
  }

  public final long nextLong() {
    final int s = scanToTokenStart();
    final int e = scanToTokenEnd();
    return parseLong(string, s, e, 10);
  }
}