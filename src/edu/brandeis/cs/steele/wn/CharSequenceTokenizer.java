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
 * XXX borrowed from Apache Harmony java.util.StringTokenizer
 *
 * CharSequenceTokenizer is used to break a string apart into tokens.  Lighter
 * than a Scanner, more features than java.util.StringTokenizer, with full
 * support for CharSequences.
 * 
 * If returnDelimiters is false, successive calls to nextToken() return maximal
 * blocks of characters that do not contain a delimiter.
 * 
 * If returnDelimiters is true, delimiters are considered to be tokens, and
 * successive calls to nextToken() return either a one character delimiter, or a
 * maximal block of text between delimiters.
 */
public class CharSequenceTokenizer implements Iterator<CharSequence> {
  private final CharSequence string;
  private String delimiters;
  private final boolean returnDelimiters;
  private int position;

  /**
   * Constructs a new CharSequenceTokenizer for string using whitespace as
   * the delimiter, returnDelimiters is false.
   * 
   * @param string the CharSequence to be tokenized
   */
  public CharSequenceTokenizer(final CharSequence string) {
    this(string, " \t\n\r\f", false);
  }

  /**
   * Constructs a new CharSequenceTokenizer for string using the specified
   * delimiters, returnDelimiters is false.
   * 
   * @param string the string to be tokenized
   * @param delimiters the delimiters to use
   */
  public CharSequenceTokenizer(final CharSequence string, final String delimiters) {
    this(string, delimiters, false);
  }

  /**
   * Constructs a new CharSequenceTokenizer for string using the specified
   * delimiters and returning delimiters as tokens when specified.
   * 
   * @param string the string to be tokenized
   * @param delimiters the delimiters to use
   * @param returnDelimiters true to return each delimiter as a token
   */
  public CharSequenceTokenizer(final CharSequence string, final String delimiters,
      final boolean returnDelimiters) {
    if (string != null) {
      this.string = string;
      this.delimiters = delimiters;
      this.returnDelimiters = returnDelimiters;
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
    int count = 0;
    boolean inToken = false;
    for (int i = position, length = string.length(); i < length; i++) {
      if (delimiters.indexOf(string.charAt(i), 0) >= 0) {
        if (returnDelimiters) {
          count++;
        }
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
    int length = string.length();
    if (position < length) {
      if (returnDelimiters) {
        return true; // there is at least one character and even if
      }
      // it is a delimiter it is a token

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
    int i = position;
    int length = string.length();

    if (i < length) {
      if (returnDelimiters) {
        if (delimiters.indexOf(string.charAt(position), 0) >= 0) {
          return String.valueOf(string.charAt(position++));
        }
        for (position++; position < length; position++) {
          if (delimiters.indexOf(string.charAt(position), 0) >= 0) {
            return string.subSequence(i, position);
          }
        }
        return string.subSequence(i, length);
      }

      while (i < length && delimiters.indexOf(string.charAt(i), 0) >= 0) {
        i++;
      }
      position = i;
      if (i < length) {
        for (position++; position < length; position++) {
          if (delimiters.indexOf(string.charAt(position), 0) >= 0) {
            return string.subSequence(i, position);
          }
        }
        return string.subSequence(i, length);
      }
    }
    throw new NoSuchElementException();
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
    return parseInt(nextToken(), 10);
  }

  public int nextInt(final int radix) {
    return parseInt(nextToken(), radix);
  }

  public int nextHexInt() {
    return nextInt(16);
  }

  public long nextLong() {
    return parseLong(nextToken(), 10);
  }

  //TODO move to a new CharSequenceUtils class

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
  public static int parseInt(final CharSequence string, final int radix)
    throws NumberFormatException {
      if (string == null || radix < Character.MIN_RADIX
          || radix > Character.MAX_RADIX) {
        throw new NumberFormatException();
      }
      int length = string.length(), i = 0;
      if (length == 0) {
        throw new NumberFormatException(string.toString());
      }
      boolean negative = string.charAt(i) == '-';
      if (negative && ++i == length) {
        throw new NumberFormatException(string.toString());
      }

      return parseInt(string, i, radix, negative);
    }

  // XXX borrowed from Apache Harmony
  private static int parseInt(final CharSequence string, int offset, final int radix,
      final boolean negative) throws NumberFormatException {
    int max = Integer.MIN_VALUE / radix;
    int result = 0, length = string.length();
    while (offset < length) {
      int digit = Character.digit(string.charAt(offset++), radix);
      if (digit == -1) {
        throw new NumberFormatException(string.toString());
      }
      if (max > result) {
        throw new NumberFormatException(string.toString());
      }
      int next = result * radix - digit;
      if (next > result) {
        throw new NumberFormatException(string.toString());
      }
      result = next;
    }
    if (!negative) {
      result = -result;
      if (result < 0) {
        throw new NumberFormatException(string.toString());
      }
    }
    return result;
  }

  /**
   * XXX borrowed from Apache Harmony
   * Parses the string argument as if it was an long value and returns the
   * result. Throws NumberFormatException if the string does not represent an
   * long quantity. The second argument specifies the radix to use when
   * parsing the value.
   * 
   * @param string a string representation of an long quantity.
   * @param radix the base to use for conversion.
   * @return long the value represented by the argument
   * @exception NumberFormatException
   *                if the argument could not be parsed as an long quantity.
   */
  public static long parseLong(final CharSequence string, final int radix)
    throws NumberFormatException {
      if (string == null || radix < Character.MIN_RADIX
          || radix > Character.MAX_RADIX) {
        throw new NumberFormatException();
      }
      int length = string.length(), i = 0;
      if (length == 0) {
        throw new NumberFormatException(string.toString());
      }
      boolean negative = string.charAt(i) == '-';
      if (negative && ++i == length) {
        throw new NumberFormatException(string.toString());
      }

      return parseLong(string, i, radix, negative);
    }

  // XXX borrowed from Apache Harmony
  private static long parseLong(final CharSequence string, int offset, final int radix,
      final boolean negative) {
    long max = Long.MIN_VALUE / radix;
    long result = 0, length = string.length();
    while (offset < length) {
      int digit = Character.digit(string.charAt(offset++), radix);
      if (digit == -1) {
        throw new NumberFormatException(string.toString());
      }
      if (max > result) {
        throw new NumberFormatException(string.toString());
      }
      long next = result * radix - digit;
      if (next > result) {
        throw new NumberFormatException(string.toString());
      }
      result = next;
    }
    if (!negative) {
      result = -result;
      if (result < 0) {
        throw new NumberFormatException(string.toString());
      }
    }
    return result;
  }
}
