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
package org.yawni.wordnet;

import java.util.ArrayList;
import java.util.List;
import org.yawni.wordnet.POS;
import org.yawni.wordnet.POS;
import org.yawni.wordnet.RelationArgument;
import org.yawni.wordnet.RelationArgument;
import org.yawni.wordnet.RelationType;
import org.yawni.wordnet.RelationType;
import org.yawni.wordnet.WordNet;
import org.yawni.wordnet.WordNet;
import org.yawni.wordnet.WordSense;
import org.yawni.wordnet.WordSense;

/**
 * Run easily with this Maven incantation:
 * {@code mvn exec:java -Dexec.mainClass="org.yawni.wordnet.MorphDerivator" -Dexec.classpathScope=test -Dexec.args="employ" }
 */
public class MorphDerivator {
  public static void main(String[] args) {
    final WordNet wordnet = WordNet.getInstance();
    for (final String arg : args) {
      final List<String> morphDerivs = new ArrayList<String>();
      for (final WordSense wordSense : wordnet.lookupWordSenses(arg, POS.ALL)) {
        for (final RelationArgument morphDerivSenseArg : wordSense.getRelationTargets(RelationType.DERIVATIONALLY_RELATED)) {
          final WordSense morphDerivSense = (WordSense) morphDerivSenseArg;
          morphDerivs.add(morphDerivSense.getLemma());
        }
      }
      System.err.println("input: "+arg+" morphDerivs: "+morphDerivs);
    }
  }
}
