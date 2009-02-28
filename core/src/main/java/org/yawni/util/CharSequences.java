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

/**
 * Utility methods for {@link CharSequence}s.
 * 
 * <p> Borrowed some code from Apache Harmony {@link java.util.StringTokenizer}
 */
public class CharSequences {
  private CharSequences() {}

  /**
   * @see java.lang.String#hashCode
   */
  public static int hashCode(final CharSequence seq) {
    if (seq instanceof String) {
      return seq.hashCode();
    }
    // s[0]*31^(n-1) + s[1]*31^(n-2) + ... + s[n-1]*31^(n-n)
    // 31 = 2^0 + 2^1 + 2^2 + 2^3 + 2^4 = 2^5 - 1
    // 2^4 * 2*16
    int hash = 0, multiplier = 1;
    for (int i = 0, n = seq.length(); i < n; i++) {
      hash += seq.charAt(i) * multiplier;
      int shifted = multiplier << 5;
      multiplier = shifted - multiplier;
    }
    return hash;
  }

  /**
   * @see java.lang.String#equals
   */
  public static boolean equals(final CharSequence s1, final CharSequence s2) {
    final int s2Len = s2.length();
    return s1.length() == s2Len && regionMatches(s1, 0, s2, 0, s2Len);
  }

  /**
   * @see java.lang.String#startsWith
   */
  public static boolean startsWith(final CharSequence s1, final CharSequence s2) {
    return regionMatches(s1, 0, s2, 0, s2.length());
  }

  /**
   * @see java.lang.String#regionMatches
   */
  public static boolean regionMatches(final CharSequence s1, int offset1,
      final CharSequence s2, int offset2, int len) {
    return regionMatches(false, s1, offset1, s2, offset2, len);
  }

  /**
   * @see java.lang.String#regionMatches
   */
  public static boolean regionMatches(final boolean ignoreCase, final CharSequence s1, int offset1,
      final CharSequence s2, int offset2, int len) {
    //System.err.println("s1: "+s1+" s2: "+s2);
    final int s1Len = s1.length();
    //System.err.println("s1Len: "+s1Len);
    //System.err.println("(s1Len-offset1): "+(s1Len-offset1)+ " len: "+len);
    if ((s1Len - offset1) < len) {
      return false;
    }
    final int s2Len = s2.length();
    //System.err.println("s2Len: "+s1Len);
    //System.err.println("(s2Len-offset2): "+(s2Len-offset2)+ " len: "+len);
    if ((s2Len - offset2) < len) {
      return false;
    }
    //System.err.println("len: "+len);

    char c1, c2;
    while (len-- != 0) {
      c1 = s1.charAt(offset1++);
      c2 = s2.charAt(offset2++);
      //System.err.println("c1: "+c1+" c2: "+c2);
      if (c1 == c2) {
        continue;
      }
      if (ignoreCase) {
        c1 = Character.toLowerCase(c1);
        c2 = Character.toLowerCase(c2);
      }
      if (c1 == c2) {
        continue;
      }
      return false;
    }
    return true;
  }

  public static boolean containsUpper(final CharSequence string) {
    int len = string.length();
    while (len-- != 0) {
      if (Character.isUpperCase(string.charAt(len))) {
        return true;
      }
    }
    return false;
  }

  /**
   * A comparison predicate that specifically ignores case and ' ', '.', '-', '_'
   * letters and their order must still match.
   */
  public static boolean sameLetterDigitSequence(final CharSequence s1, final CharSequence s2) {
    final int s1Len = s1.length();
    final int s2Len = s2.length();
    int len = Math.min(s1Len, s2Len);
    int i = 0, j = 0;
    char c1, c2;
    while (len-- != 0) {

    }

    throw new UnsupportedOperationException("IMPLEMENT ME");
  }

  // what could be done to make this work for String, StringBuilder, StringBuffer, ...
  // interchangably ? could turn out tricky due to #equals()
//  static class CharSequenceComparator implements Comparator<CharSequence> {
//    private static final long serialVersionUID = 1L;
//
//    /** {@inheritDoc} */
//    public int compare(final CharSequence s1, final CharSequence s2) {
//      int i = 0;
//      int n = Math.min(s1.length(), s2.length());
//      while (n-- != 0) {
//        final char c1 = s1.charAt(i);
//        final char c2 = s2.charAt(i++);
//        if (c1 != c2) {
//          return c1 - c2;
//        }
//      }
//      return s1.length() - s2.length();
//    }
//    /** {@inheritDoc} */
//    @Override
//    public boolean equals(final Object obj) {
//      return obj instanceof CharSequenceComparator;
//    }
//    public static final CharSequenceComparator INSTANCE = new CharSequenceComparator();
//  } // end class CharSequenceComparator

  /**
   * Parses the {@code string} argument as if it was an {@code int} value and returns the
   * result. Throws {@linkplain NumberFormatException} if the string does not represent an
   * {@code int} quantity. {@code radix} is the radix to use when parsing
   * the value.
   *
   * @param string a string representation of an int quantity.
   * @param radix the base to use for conversion.
   * @return int the value represented by the argument
   * @throws NumberFormatException
   *                if the argument could not be parsed as an int quantity.
   */
  public static int parseInt(final CharSequence string, final int radix)
    throws NumberFormatException {
    return parseInt(string, 0, string.length(), radix);
  }

  /**
   * Parses the {@code string} argument as if it was an {@code int} value and returns the
   * result. Throws {@linkplain NumberFormatException} if the string does not represent an
   * {@code int} quantity.
   *
   * @param string a string representation of an int quantity.
   * @param offset
   * @param end
   * @return int the value represented by the argument
   * @throws NumberFormatException
   *                if the argument could not be parsed as an int quantity.
   */
  public static int parseInt(final CharSequence string, int offset,
      final int end) {
    return parseInt(string, offset, end, 10);
  }

  public static int parseInt(final CharSequence string, int offset,
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
    if (! negative) {
      result = -result;
      if (result < 0) {
        throw new NumberFormatException(toString(string, start, end));
      }
    }
    return result;
  }

  /**
   * Parses the {@code string} argument as if it was a {@code long} value and returns the
   * result. Throws {@linkplain NumberFormatException} if the string does not represent a
   * {@code long} quantity.
   *
   * @param string a string representation of an {@code long} quantity.
   * @param radix the base to use for conversion.
   * @return {@code long} the value represented by the argument
   * @throws NumberFormatException
   *                if the argument could not be parsed as a long quantity.
   */
  public static long parseLong(final CharSequence string, final int radix)
    throws NumberFormatException {
      return parseLong(string, 0, string.length(), radix);
    }

  /**
   * Parses the {@code string} argument as if it was a {@code long} value and returns the
   * result. Throws {@linkplain NumberFormatException} if the string does not represent a
   * {@code long} quantity.
   * 
   * @param string a string representation of an {@code long} quantity.
   * @param offset
   * @param end
   * @param radix the base to use for conversion.
   * @return {@code long} the value represented by the argument
   * @throws NumberFormatException
   *                if the argument could not be parsed as a long quantity.
   */
  public static long parseLong(final CharSequence string, int offset,
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
    if (! negative) {
      result = -result;
      if (result < 0) {
        throw new NumberFormatException(toString(string, start, end));
      }
    }
    return result;
  }

  public static String toString(final CharSequence charSequence,
      final int offset, final int end) {
    return charSequence.subSequence(offset, end).toString();
  }
}
