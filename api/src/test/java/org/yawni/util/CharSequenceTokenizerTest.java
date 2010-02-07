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

import java.nio.CharBuffer;
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
    return (int) Utils.distance(new CharSequenceTokenizer(string));
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
}