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

/**
 * {@code CharSequenceTokenizer} is used to break a string apart into tokens.  Lighter
 * than a {@link java.util.Scanner}, more features than {@link java.util.StringTokenizer java.util.StringTokenizer}, with full
 * support for {@link CharSequence}s.
 */
public final class CharSequenceTokenizer extends AbstractCharSequenceTokenizer implements Iterator<CharSequence> {
  /**
   * Constructs a new {@code CharSequenceTokenizer} for {@code string} using whitespace as
   * the delimiter.
   * @param string the CharSequence to be tokenized
   */
  public CharSequenceTokenizer(final CharSequence string) {
    super(string);
  }

  /**
   * Constructs a new {@code CharSequenceTokenizer} for {@code string} using the specified
   * {@code delimiters} and returning delimiters as tokens when specified.
   * @param string the string to be tokenized
   * @param delimiters the delimiters to use
   */
  public CharSequenceTokenizer(final CharSequence string, final String delimiters) {
    super(string, delimiters);
  }

  /**
   * Constructs a new {@code CharSequenceTokenizer} for {@code string} using the specified
   * {@code delimiters} and returning delimiters as tokens when specified.
   * @param string the string to be tokenized
   * @param position position in string
   * @param delimiters the delimiters to use
   */
  public CharSequenceTokenizer(final CharSequence string, final int position, final String delimiters) {
    super(string, position, delimiters);
  }
}