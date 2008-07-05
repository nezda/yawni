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

package edu.brandeis.cs.steele.wn;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * XXX borrowed from Apache Harmony java.util.StringTokenizer<br><br>
 *
 * <code>CharSequenceTokenizer</code> is used to break a string apart into tokens.  Lighter
 * than a {@link java.util.Scanner}, more features than {@link java.util.StringTokenizer}, with full
 * support for {@link CharSequence}s.
 */
public class CharSequenceTokenizer implements Iterator<CharSequence> {
  private final CharSequence string;
  private String delimiters;
  private int position;

  /**
   * Constructs a new <code>CharSequenceTokenizer</code> for <var>string</var> using whitespace as
   * the delimiter.
   *
   * @param string the CharSequence to be tokenized
   */
  public CharSequenceTokenizer(final CharSequence string) {
    this(string, " \t\n\r\f");
  }

  /**
   * Constructs a new <code>CharSequenceTokenizer</code> for <var>string</var> using the specified
   * <var>delimiters</var> and returning delimiters as tokens when specified.
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
    return countTokens(string, position, delimiters);
  }

  /**
   * Returns the number of tokens in <var>string</var> separated by
   * <var>delimiters</var> starting at <var>position</var>.
   *
   * @param string the string to be tokenized
   * @param delimiters the delimiters to use
   * @return number of tokens that can be retreived before an exception will
   *         result
   */
  public static int countTokens(final CharSequence string, final int position,
      final String delimiters) {
    int count = 0;
    boolean inToken = false;
    for (int i = position, length = string.length(); i < length; i++) {
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
   * @exception NoSuchElementException if no tokens remain
   */
  public CharSequence next() {
    return nextToken();
  }

  /**
   * @exception UnsupportedOperationException
   */
  public void remove() {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the next token in the string as a CharSequence.
   *
   * @return next token in the string as a CharSequence
   * @exception NoSuchElementException if no tokens remain
   */
  public CharSequence nextToken() {
    final int s = scanToTokenStart();
    final int e = scanToTokenEnd();
    return string.subSequence(s, e);
  }

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

  //public int nextByte() {
  //  return Byte.parseByte(nextToken());
  //}

  //public int nextShort() {
  //  return Short.parseShort(nextToken());
  //}

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

  //TODO move to a new CharSequenceUtils class - is there one of these in Commons?

  /**
   * XXX borrowed from Apache Harmony
   * Parses the string argument as if it was an int value and returns the
   * result. Throws NumberFormatException if the string does not represent an
   * int quantity. The second argument specifies the radix to use when parsing
   * the value.
   *
   * @param string a string representation of an int quantity.
   * @param radix the base to use for conversion.
   * @return int the value represented by the argument
   * @exception NumberFormatException
   *                if the argument could not be parsed as an int quantity.
   */
  static int parseInt(final CharSequence string, final int radix)
    throws NumberFormatException {
      return parseInt(string, 0, string.length(), radix);
    }

  static int parseInt(final CharSequence string, int offset,
      final int end) {
    return parseInt(string, offset, end, 10);
  }

  // XXX borrowed from Apache Harmony
  static int parseInt(final CharSequence string, int offset,
      final int end, final int radix) {
    final int start = offset;
    if (string == null || radix < Character.MIN_RADIX
        || radix > Character.MAX_RADIX) {
      throw new NumberFormatException();
    }
    if (start >= end) {
      throw new NumberFormatException(toString(string, start, end));
    }
    final boolean negative = string.charAt(offset) == '-';
    if (negative && ++offset == end) {
      throw new NumberFormatException(toString(string, start, end));
    }

    final int max = Integer.MIN_VALUE / radix;
    int result = 0;
    while (offset < end) {
      final int digit = Character.digit(string.charAt(offset++), radix);
      if (digit == -1) {
        throw new NumberFormatException(toString(string, start, end));
      }
      if (max > result) {
        throw new NumberFormatException(toString(string, start, end));
      }
      final int next = result * radix - digit;
      if (next > result) {
        throw new NumberFormatException(toString(string, start, end));
      }
      result = next;
    }
    if (!negative) {
      result = -result;
      if (result < 0) {
        throw new NumberFormatException(toString(string, start, end));
      }
    }
    return result;
  }

  /**
   * XXX borrowed from Apache Harmony
   * Parses the string argument as if it was a long value and returns the
   * result. Throws NumberFormatException if the string does not represent a
   * long quantity. The second argument specifies the radix to use when
   * parsing the value.
   * @param string a string representation of an long quantity.
   * @param radix the base to use for conversion.
   * @return long the value represented by the argument
   * @exception NumberFormatException
   *                if the argument could not be parsed as a long quantity.
   */
  private static long parseLong(final CharSequence string, final int radix)
    throws NumberFormatException {
      return parseLong(string, 0, string.length(), radix);
    }

  // XXX borrowed from Apache Harmony
  private static long parseLong(final CharSequence string, int offset,
      final int end, final int radix) {
    final int start = offset;
    if (string == null || radix < Character.MIN_RADIX
        || radix > Character.MAX_RADIX) {
      throw new NumberFormatException();
    }
    if (start >= end) {
      throw new NumberFormatException(toString(string, start, end));
    }
    final boolean negative = string.charAt(offset) == '-';
    if (negative && ++offset == end) {
      throw new NumberFormatException(toString(string, start, end));
    }
    final long max = Long.MIN_VALUE / radix;
    long result = 0;
    while (offset < end) {
      final int digit = Character.digit(string.charAt(offset++), radix);
      if (digit == -1) {
        throw new NumberFormatException(toString(string, start, end));
      }
      if (max > result) {
        throw new NumberFormatException(toString(string, start, end));
      }
      final long next = result * radix - digit;
      if (next > result) {
        throw new NumberFormatException(toString(string, start, end));
      }
      result = next;
    }
    if (!negative) {
      result = -result;
      if (result < 0) {
        throw new NumberFormatException(toString(string, start, end));
      }
    }
    return result;
  }

  private static String toString(final CharSequence charSequence,
      final int offset, final int end) {
    return charSequence.subSequence(offset, end).toString();
  }
}
