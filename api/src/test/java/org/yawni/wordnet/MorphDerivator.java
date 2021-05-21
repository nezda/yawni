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

import com.google.common.collect.ImmutableSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.yawni.util.WordSenseToLemma;
import static com.google.common.collect.Iterables.transform;

/**
 * Run easily with this Maven incantation: {@code
 *   mvn exec:java -Dexec.mainClass="org.yawni.wordnet.MorphDerivator" -Dexec.classpathScope=test -Dexec.args="employ"
 * }
 * NOTE: you <em>must</em> compile test!
 */
public class MorphDerivator {
  // recursively follow derivational relations
  // note: WordNet 3.0 is missing the deriavational relation between "recursion"#n and "recursive"#adj, although it
  // spells it out in "recursive"'s defintion ("of or relating to a recursion")
  private static void gather(final WordSense wordSense, final Set<WordSense> derivs) {
    for (final RelationArgument morphDerivSenseArg : wordSense.getRelationTargets(RelationType.DERIVATIONALLY_RELATED)) {
      final WordSense morphDerivSense = (WordSense) morphDerivSenseArg;
      // avoid cycles (infinite recursion)
      if (! derivs.contains(morphDerivSense)) {
        derivs.add(morphDerivSense);
        gather(morphDerivSense, derivs);
      }
    }
  }

  public static void main(String[] args) {
    final WordNet wordnet = WordNet.getInstance();
    for (final String arg : args) {
      final Set<WordSense> seen = new LinkedHashSet<>();
      for (final WordSense wordSense : wordnet.lookupWordSenses(arg, POS.ALL)) {
        gather(wordSense, seen);
      }
      final ImmutableSet<String> morphDerivs = ImmutableSet.copyOf(transform(seen, new WordSenseToLemma()));
      System.err.format("input: %20s morphDerivs: %s\n", arg, morphDerivs);
    }
  }
}
