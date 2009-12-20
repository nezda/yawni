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
package org.yawni.wn;

import org.yawni.util.Preconditions;
import org.yawni.util.Utils;

/**
 * Generates <a href="http://wordnet.princeton.edu/wordnet/man/wnstats.7WN.html">WNSTATS</a>
 */
public class WNSTATSGenerator {
  public static void main(String[] args) {
    final DictionaryDatabase dictionary = FileBackedDictionary.getInstance();
    System.out.println("Number of words, synsets, and senses");
    
    //POS   	Unique   	Synsets   	Total
	  //        Strings 	            Word-Sense Pairs
    long totalWordCount, totalSynsetCount, totalWordSenseCount;
    totalWordCount = totalSynsetCount = totalWordSenseCount = 0;
    for (final POS pos : POS.CATS) {
      final String posLabel = Utils.capitalize(pos.getLabel());
      final long wordCount = Utils.distance(dictionary.words(pos));
      totalWordCount += wordCount;
      final long synsetCount = Utils.distance(dictionary.synsets(pos));
      totalSynsetCount += synsetCount;
      final long wordSenseCount = Utils.distance(dictionary.wordSenses(pos));
      totalWordSenseCount += wordSenseCount;
      final String row = String.format("%-10s%20d%20d%20d\n",
        posLabel, wordCount, synsetCount, wordSenseCount);
      System.out.print(row);
    }
    Preconditions.checkState(totalWordCount == Utils.distance(dictionary.words(POS.ALL)));
    Preconditions.checkState(totalSynsetCount == Utils.distance(dictionary.synsets(POS.ALL)));
    Preconditions.checkState(totalWordSenseCount == Utils.distance(dictionary.wordSenses(POS.ALL)));
    
    final String sumary = String.format("%-10s%20d%20d%20d\n",
        "Totals", totalWordCount, totalSynsetCount, totalWordSenseCount);
    System.out.print(sumary);

    System.out.println();
    
    System.out.println("Polysemy information");
    
    //POS   	Monosemous   	    Polysemous   	Polysemous
    //        Words and Senses 	Words 	      Senses
    for (final POS pos : POS.CATS) {
      final String posLabel = Utils.capitalize(pos.getLabel());
      final long monosemousWordCount = monosemousWordCount(pos, dictionary);
      final long polysemousWordCount = polysemousWordCount(pos, dictionary);
      final long polysemousWordSensesCount = polysemousWordSensesCount(pos, dictionary);
      final String row = String.format("%-10s%20d%20d%20d\n",
        posLabel, monosemousWordCount, polysemousWordCount, polysemousWordSensesCount);
      System.out.print(row);
    }

    //POS   	Average Polysemy   	        Average Polysemy
    //        Including Monosemous Words 	Excluding Monosemous Words
  }

  private static long monosemousWordCount(final POS pos, final DictionaryDatabase dictionary) {
    long monosemousWordCount = 0;
    for (final Word word : dictionary.words(pos)) {
      if (word.getWordSenses().size() == 1) {
        monosemousWordCount++;
      }
    }
    return monosemousWordCount;
  }

  private static long polysemousWordCount(final POS pos, final DictionaryDatabase dictionary) {
    long polysemousWordCount = 0;
    for (final Word word : dictionary.words(pos)) {
      if (word.getWordSenses().size() > 1) {
        polysemousWordCount++;
      }
    }
    return polysemousWordCount;
  }

  // there are different interpretations of "Polysemous Senses"
  // Word can be in different Synsets
  // Synset can have multiple members
  private static long polysemousWordSensesCount(final POS pos, final DictionaryDatabase dictionary) {
    long polysemousWordSenseCount = 0;
//    for (final WordSense wordSense : dictionary.wordSenses(pos)) {
//      if (wordSense.getSynset().getWordSenses().size() > 1) {
//        polysemousWordSenseCount++;
//      }
//    }

//    for (final Synset synset : dictionary.synsets(pos)) {
//      final int synsetSize = synset.getWordSenses().size();
//      if (synsetSize > 1) {
//        polysemousWordSenseCount++;
////        polysemousWordSenseCount += synsetSize;
//      }
//    }

    for (final Word word : dictionary.words(pos)) {
      final int numSenses = word.getWordSenses().size();
      if (numSenses > 1) {
//        polysemousWordSenseCount++;
        polysemousWordSenseCount += numSenses;
      }
    }

    return polysemousWordSenseCount;
  }
}