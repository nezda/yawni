/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.yawni.util;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import java.nio.CharBuffer;
import java.util.List;
import java.util.NoSuchElementException;
import static org.yawni.util.AbstractCharSequenceTokenizer.*;
import org.junit.Test;
import static org.fest.assertions.Assertions.assertThat;

public class CharSequenceTokenizerTest {
  @Test
  public void testEqualsHashCode() {
    final String s = "someString";
    final CharBuffer cb = CharBuffer.wrap(s);
    final CharSequence scs = s;
    final CharSequence cbcs = cb;
    // FEST Assert API oddity; works for ObjectAssert, not StringAssert
//    assertThat(s).isInstanceOf(CharSequence.class);
    assertThat(cb).isInstanceOf(CharSequence.class);
    // String, StringBuilder/StringBuffer, and CharBuffer are all incompatible with
    // respect to equals() AND hashCode()
    // FEST Assert API is type safe, but Object.equals() isn't
    //assertThat(s).isNotEqualTo(cb);
    assertThat(s.equals(cb)).isFalse();
    assertThat(s.hashCode()).isNotEqualTo(cb.hashCode());
  }

  @Test
  public void testCountTokens() {
    int tokenCount;
    String string;

    tokenCount = 0;
    string = "";
    assertThat(countTokens(string)).isEqualTo(tokenCount);
    assertThat(altCountTokens(string)).isEqualTo(tokenCount);

    tokenCount = 0;
    string = " ";
    assertThat(countTokens(string)).isEqualTo(tokenCount);
    assertThat(altCountTokens(string)).isEqualTo(tokenCount);

    tokenCount = 1;
    string = " abc";
    assertThat(countTokens(string)).isEqualTo(tokenCount);
    assertThat(altCountTokens(string)).isEqualTo(tokenCount);

    tokenCount = 1;
    string = "abc ";
    assertThat(countTokens(string)).isEqualTo(tokenCount);
    assertThat(altCountTokens(string)).isEqualTo(tokenCount);

    tokenCount = 2;
    string = "abc \t def";
    assertThat(countTokens(string)).isEqualTo(tokenCount);
    assertThat(altCountTokens(string)).isEqualTo(tokenCount);

    tokenCount = 2;
    string = "\tabc \t def\t ";
    assertThat(countTokens(string)).isEqualTo(tokenCount);
    assertThat(altCountTokens(string)).isEqualTo(tokenCount);
  }

  private static int altCountTokens(String string) {
    // use default delimiters
    return Iterators.size(new CharSequenceTokenizer(string));
  }

  @Test
  public void testCharSequenceTokenizer() {
    //CharSequenceTokenizer tokens = new CharSequenceTokenizer("1 2 3");
    //CharSequenceTokenizer tokens = new CharSequenceTokenizer(" 1 2 3");
    //CharSequenceTokenizer tokens = new CharSequenceTokenizer(" 1");
    //while(tokens.hasNext()) {
    //  System.err.printf("next: \"%s\"\n", tokens.nextInt());
    //}

    String s = "0";
    assertThat(new CharSequenceTokenizer(s).nextInt()).isEqualTo(0);
    s = " 0";
    assertThat(new CharSequenceTokenizer(s).nextInt()).isEqualTo(0);
    s = "1";
    assertThat(new CharSequenceTokenizer(s).nextInt()).isEqualTo(1);
    s = " 1";
    assertThat(new CharSequenceTokenizer(s).nextInt()).isEqualTo(1);
    s = " 1 ";
    assertThat(new CharSequenceTokenizer(s).nextInt()).isEqualTo(1);
    s = "-1";
    assertThat(new CharSequenceTokenizer(s).nextInt()).isEqualTo(-1);
    s = " -1";
    assertThat(new CharSequenceTokenizer(s).nextInt()).isEqualTo(-1);
    //System.err.println("testCharSequenceTokenizer passed");
  }

  @Test
  public void testBasics() {
    String string;
    StringTokenizer tokenizer;

    string = "?name=value";
    tokenizer = new StringTokenizer(string, 1, "=");
    assertThat(tokenizer.next()).isEqualTo("name");
    assertThat(tokenizer.next()).isEqualTo("value");
  }

  @Test(expected = NoSuchElementException.class)
  public void testBoundary() {
    final List<String> items = ImmutableList.of("A", "B", "C", "D");
    final String delim = "\r\n";
    final String c = Joiner.on(delim).join(items);
    CharSequenceTokenizer tok = new CharSequenceTokenizer(c, delim);
    for (int i = 0; i < items.size() + 1; i++) {
      tok.next();
    }
  }

  @Test(expected = NoSuchElementException.class)
  public void testBeginBoundary() {
//    System.err.println("testBeginBoundary");
    final List<String> items = ImmutableList.of("A", "B", "C", "D");
    final String delim = "\r\n";
    final String c = Joiner.on(delim).join(items);
    CharSequenceTokenizer tok = new CharSequenceTokenizer(c, delim);
    for (int i = 0; i < items.size(); i++) {
      final CharSequence t = tok.next();
//      System.err.println("next: "+t);
    }
    for (int i = 0; i < items.size() + 1; i++) {
      final CharSequence t = tok.previous();
//      System.err.println("prev: "+t);
    }
//    System.err.println("testBeginBoundary done");
  }

  @Test
  public void testLineBreaker() {
    final List<String> items = ImmutableList.of("A", "B", "C", "D");
    final String delim = "\r\n";
    final String c = Joiner.on(delim).join(items);
    CharSequenceTokenizer tok = new CharSequenceTokenizer(c, delim);
    assertThat(ImmutableList.copyOf(tok)).isEqualTo(items);

    tok = new CharSequenceTokenizer(c, delim);
    assertThat(tok.hasPrevious()).isFalse();
    assertThat(tok.next()).isEqualTo("A");
    assertThat(tok.next()).isEqualTo("B");
    assertThat(tok.next()).isEqualTo("C");
    assertThat(tok.next()).isEqualTo("D");
    assertThat(tok.previous()).isEqualTo("D");
    assertThat(tok.previous()).isEqualTo("C");
    assertThat(tok.previous()).isEqualTo("B");
    assertThat(tok.previous()).isEqualTo("A");
    assertThat(tok.next()).isEqualTo("A");
    tok.skipNextToken();
    assertThat(tok.next()).isEqualTo("C");
    tok.skipPreviousToken();
    assertThat(tok.previous()).isEqualTo("B");
  }

  // many more boundary cases could be tested
  // - edge delimiter content (esp. trailing)
  // - pure delimiter input
  // - attempting to use tokenizer again after it has thrown a NSEE
}