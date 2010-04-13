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

import java.util.List;
import org.yawni.wordnet.Word;

public final class WordCaseUtils {
  private WordCaseUtils(){ }
  
  /**
   * note this only adds case variants within its POS
   * weird cases exist: NOUN "Romaic", ADJ "romaic"
   * @yawni.experimental
   */
  public static List<String> getUniqueLemmaCaseVariants(final Word word) {
    return ImmutableList.copyOf(Utils.uniq(new WordSenseToLemma(word.getWordSenses())));
  }

  /**
   * If single cased, return that case, else return lowercased version.
   * Note this only {@code Word}'s {@code POS}.
   */
  public static String getDominantCasedLemma(final Word word) {
    final List<String> lemmas = getUniqueLemmaCaseVariants(word);
    if (lemmas.size() == 1) {
      // single case
      return lemmas.get(0).toString();
    } else {
      return word.getLowercasedLemma();
    }
  }
}