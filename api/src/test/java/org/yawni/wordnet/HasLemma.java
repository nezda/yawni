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
package org.yawni.wordnet;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.BaseMatcher;

//"item", isIn(collection<T>)
//- matches("item") => collection.contains("item")

//different T's for factory method and matches()
//"lemma" isLemmaOf(word)
//- matches("lemma") => word.getLowercasedLemma().equals("lemma")
//"lemma" isLemmaOf(wordSense)

// inverted order (hasLemma("lemma")
//word, hasLemma("lemma")
//- matches(word) => "lemma".equals(word.getLowercasedLemma())

// maybe would be more properly implemented as IsLemmaOf ?
// assertThat("'hood", isLemmaOf(wordSenseX))
//
// hack to support Word and WordSense which currently have no
// common interface (e.g., GetLemma/HaveLemma/Lemma)
class HasLemma<T> extends BaseMatcher<T> {
  private final Object wordOrWordSense;

  private HasLemma(final Word word) {
    this.wordOrWordSense = word;
  }
  private HasLemma(final WordSense wordSense) {
    this.wordOrWordSense = wordSense;
  }
  @Override
  public boolean matches(Object operand) {
    String lemma = (String) operand;
    if (wordOrWordSense instanceof Word) {
      return lemma.equals(((Word)wordOrWordSense).getLowercasedLemma());
    } else if (wordOrWordSense instanceof WordSense) {
      return lemma.equals(((WordSense)wordOrWordSense).getLemma());
    } else {
      throw new IllegalArgumentException("wordOrWordSense: "+wordOrWordSense+
        " unsupported class: " + wordOrWordSense.getClass());
    }
  }
  @Override
  public void describeTo(final Description description) {
    description.
      appendText("is lemma of ").
      appendValue(wordOrWordSense);
  }
  @Factory
  public static <T> Matcher<T> isLemmaOf(final Word operand) {
    return new HasLemma<>(operand);
  }
  @Factory
  public static <T> Matcher<T> isLemmaOf(final WordSense operand) {
    return new HasLemma<>(operand);
  }
}