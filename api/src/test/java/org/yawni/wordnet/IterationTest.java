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

import org.junit.*;
import static org.junit.Assert.*;
import static org.yawni.wordnet.HasLemma.*;

import java.util.*;
import java.lang.management.*;
import java.util.regex.PatternSyntaxException;

import org.yawni.util.Utils;
import static org.yawni.util.Utils.first;
import static org.yawni.util.Utils.isEmpty;
import org.yawni.util.MergedIterable;
import org.yawni.wordnet.WordSense.AdjPosition;

/**
 * Goal: verify various iteration methods of dictionary behave as expected.
 */
public class IterationTest {
  private WordNetInterface dictionary;
  private Random rand;
  
  @Before
  public void init() {
    dictionary = WordNet.getInstance();
    // keep the test deterministic
    rand = new Random(0);
  }

  private static boolean contains(final List<Relation> relations, final RelationType queryType) {
    for (final Relation relation : relations) {
      if (relation.getType() == queryType) {
        return true;
      }
    }
    return false;
  }

  @Test
  @Ignore // slow and not generally informative
  public void multiHypernyms() {
    logTest("searchMultiHypernyms");
    int numWithMultiParents = 0;
    int numWithInstanceHypernymParents = 0;
    for (final Synset synset : dictionary.synsets(POS.ALL)) {
      final List<Relation> parents = synset.getRelations(RelationType.HYPERNYM);
      numWithMultiParents += parents.size() > 1 ? 1 : 0;
      if (parents.size() > 1) {
        // note quite a few of these are INSTANCE_HYPERNYM (791 of 2,244)
        //System.err.printf("  %20s\n", synset);
        if (contains(parents, RelationType.INSTANCE_HYPERNYM)) {
          numWithInstanceHypernymParents++;
        }
      }
    }
    System.err.printf("  %20s %d\n", "numWithMultiParents:", numWithMultiParents); // WN 3.0: 2244
    System.err.printf("  %20s %d\n", "numWithInstanceHypernymParents:", numWithInstanceHypernymParents); // WN 3.0: 791
  }

  @Test
  @Ignore // not generally informative
  public void lexnames() {
    logTest("lexnames");
    for (final Lexname lexname : Lexname.values()) {
      final long cnt = Utils.size(dictionary.synsets("?lexname=" + lexname.name()));
//      System.err.printf("  %30s %d\n", lexname, cnt);
    }
  }

  @Test
  @Ignore // not generally informative
  public void adjPositions() {
    logTest("adjPositions");
    for (final AdjPosition adjPosition : AdjPosition.values()) {
      final long cnt = Utils.size(dictionary.wordSenses("?adj_position=" + adjPosition.name()));
//      System.err.printf("  %30s %d\n", adjPosition, cnt);
    }
  }

  /** 
   * test searching iterators
   * - searchByPrefix()
   * - searchBySubstring()
   * <strong>Parts of this test uses hard coded values for WordNet 3.0</strong>
   */
  @Test
  public void searchIteratorBoundaryCases() {
    logTest("searchIteratorBoundaryCases");
    // test plan
    // + search for first word
    // + search for first word's prefix
    // + search for last word
    // + search for last word's prefix
    // + search after last word (non-existant)
    // + search before first word (non-existant)
    // + iterate over words, iterate over prefixes
    //   + assert containsLemma(Iterable<Word> matches)
    final Iterable<Word> firstWord = dictionary.searchByPrefix("'hood", POS.NOUN);
    final Word first = first(firstWord);
    assertThat("'hood", isLemmaOf(first));
    final Iterable<Word> firstWordPrefix = dictionary.searchByPrefix("'hoo", POS.NOUN);
    final Word firstPrefix = first(firstWordPrefix);
    assertThat("'hood", isLemmaOf(firstPrefix));
    final Iterable<Word> preFirstWord = dictionary.searchByPrefix("''", POS.NOUN);
    assertTrue(isEmpty(preFirstWord));

    final Iterable<Word> lastWord = dictionary.searchByPrefix("zyrian", POS.NOUN);
    final Word last = first(lastWord);
    assertThat("zyrian", isLemmaOf(last));
    final Iterable<Word> lastWordPrefix = dictionary.searchByPrefix("zyria", POS.NOUN);
    final Word lastPrefix = first(lastWordPrefix);
    assertThat("zyrian", isLemmaOf(lastPrefix));
    final Iterable<Word> postLastWordPrefix = dictionary.searchByPrefix("zz", POS.NOUN);
    assertTrue(isEmpty(postLastWordPrefix));

    for (final POS pos : POS.CATS) {
      final Iterable<Word> leadingDashPrefix = dictionary.searchByPrefix("-", pos);
      assertTrue(isEmpty(leadingDashPrefix));
      final Iterable<String> leadingDashPrefixLemma = new WordToLowercasedLemma(dictionary.searchByPrefix("-", pos));
      assertTrue(isEmpty(leadingDashPrefixLemma));
      final Iterable<Word> leadingSpacePrefix = dictionary.searchByPrefix(" ", pos);
      assertTrue(isEmpty(leadingSpacePrefix));
      final Iterable<Word> emptyPrefix = dictionary.searchByPrefix("", pos);
      assertTrue(isEmpty(emptyPrefix));
    }

    // TODO use POS.ALL ?
    final Iterable<Word> anyEmptyPrefix = MergedIterable.merge(true,
        dictionary.searchByPrefix("-", POS.NOUN),
        dictionary.searchByPrefix("-", POS.VERB),
        dictionary.searchByPrefix("-", POS.ADJ),
        dictionary.searchByPrefix("-", POS.ADV));
    assertTrue(isEmpty(anyEmptyPrefix));

    // TODO use POS.ALL ?
    final Iterable<Word> anyNonExistantPrefix = MergedIterable.merge(true,
        dictionary.searchByPrefix("lllllll", POS.NOUN),
        dictionary.searchByPrefix("lllllll", POS.VERB),
        dictionary.searchByPrefix("lllllll", POS.ADJ),
        dictionary.searchByPrefix("lllllll", POS.ADV));
    assertTrue(isEmpty(anyNonExistantPrefix));

    // TODO use POS.ALL ?
    final Iterable<Word> anyLeadingDashPrefix = MergedIterable.merge(true,
        dictionary.searchByPrefix("", POS.NOUN),
        dictionary.searchByPrefix("", POS.VERB),
        dictionary.searchByPrefix("", POS.ADJ),
        dictionary.searchByPrefix("", POS.ADV));
    assertTrue(isEmpty(anyLeadingDashPrefix));

    final Iterable<String> runs = Utils.uniq(
        new WordToLowercasedLemma(MergedIterable.merge(true,
            dictionary.searchByPrefix("run", POS.NOUN),
            dictionary.searchByPrefix("run", POS.VERB))));
    assertTrue(Utils.isUnique(runs, true));

    final Iterable<Word> spaceWords = dictionary.searchBySubstring(" ", POS.NOUN);
    assertFalse(isEmpty(spaceWords));

    final Iterable<Word> emptyString = dictionary.searchBySubstring("", POS.NOUN);
    assertTrue(isEmpty(emptyString));

    // TODO use POS.ALL ?
    final Iterable<Word> anyNonExistantSubstring = MergedIterable.merge(true,
        dictionary.searchBySubstring("lllllll", POS.NOUN),
        dictionary.searchBySubstring("lllllll", POS.VERB),
        dictionary.searchBySubstring("lllllll", POS.ADJ),
        dictionary.searchBySubstring("lllllll", POS.ADV));
    assertTrue(isEmpty(anyNonExistantSubstring));

    // expose problem where not skipping initial lines
    final Iterable<Word> everything = dictionary.searchBySubstring(".*", POS.ALL);
    assertFalse(isEmpty(everything));
  }

  @Test (expected=PatternSyntaxException.class)
  public void invalidRegexPatterns() {
    logTest("invalidRegexPatterns");
    final Iterable<Word> openCharClass = dictionary.searchBySubstring("[", POS.ALL);
    // note, exception is not triggered until we inspect the result
    assertTrue(isEmpty(openCharClass));
    // should never get here
    //final Iterable<Word> danglingMetaChar = dictionary.searchBySubstring("*", POS.ALL);
    //assertTrue(isEmpty(openCharClass));
  }

  // speed this test up by only searching for prefixes of
  // random sample of terms
  private final float prefixDitchProportion = 0.9995f; // 0.9999f

  @Test
  public void prefixSearch() {
    logTest("prefixSearch");
    int numPrefixTests = 0;
    final POS pos = POS.NOUN;
    for (final Word word : dictionary.words(pos)) {
      if (rand.nextFloat() < prefixDitchProportion) {
        continue;
      }
      final String lemma = word.getLowercasedLemma();
      for (int i = 1, n = lemma.length(); i < n; i++) {
        final String prefix = lemma.substring(0, i);
        final Iterable<Word> matches = dictionary.searchByPrefix(prefix, pos);
        numPrefixTests++;
        assertTrue(containsLemma(matches, lemma));
      }
    }
    System.err.printf("  %20s %d\n", "numPrefixTests:", numPrefixTests);
  }

  // speed this test up by only searching for substrings of
  // random sample of terms
  final float substringDitchProportion = 0.9999f; // 0.9999f

  @Test
  public void substringSearch() {
    logTest("substringSearch");
    int numSubstringTests = 0;
    final POS pos = POS.NOUN;
    for (final Word word : dictionary.words(pos)) {
      if (rand.nextFloat() < substringDitchProportion) {
        continue;
      }
      final String lemma = word.getLowercasedLemma();
      for (int i = 1, n = lemma.length(); i < n; i++) {
        //FIXME since substring, should test infixes too (will slow down test)
        final String prefix = lemma.substring(0, i);
        final Iterable<Word> matches = dictionary.searchBySubstring(prefix, pos);
        numSubstringTests++;
        assertTrue("lemma: "+lemma+" prefix: "+prefix, containsLemma(matches, lemma));
        //System.err.println("numSubstringTests: "+numSubstringTests);
      }
    }
    System.err.printf("  %20s %d\n", "numSubstringTests:", numSubstringTests);
  }

  /** <strong>Parts of this test uses hard coded values for WordNet 3.0</strong> */
  @Test
  public void wordIterationBoundary() {
    logTest("wordIterationBoundary");
    // check if iteration returns first AND last item (boundary cases) 
    // - look at data files manually ($WNHOME/dict/index.<pos>)
    // TODO check this for all POS
    final Iterable<Word> nounIndexWords = dictionary.words(POS.NOUN);
    final Word first = first(nounIndexWords);
    //System.err.println("first: "+first);
    // to get these offsets with gvim, open the data file, put the cursor on
    // the first char of the line you expect (e.g., first content line, last
    // line), and g CTRL-g will report "Byte n of m" -- n is a 1 based file
    // offset - Word offsets are zero based so you would expect n-1
    assertEquals(1740, first.getOffset());
    assertThat("'hood", isLemmaOf(first));
    Word last = null;
    for (final Word word : nounIndexWords) {
      last = word;
    }
    //System.err.println("last: "+last);
    assertEquals(4786625, last.getOffset());
    assertThat("zyrian", isLemmaOf(last));

    assertEquals(first, first(nounIndexWords));
    // IF iteration used caching, this might or might not be the case
    assertTrue("not distinct objects?", first != first(nounIndexWords));
  }

  @Ignore // coverd by other tests
  @Test
  public void exoticPOS() {
    logTest("exoticPOS");
    //for(final Word word : dictionary.words(POS.SAT_ADJ)) {
    //  System.err.println(word);
    //}
    // SAT_ADJ this doesn't seem to have any members ??
    for (final POS pos : POS.values()) {
      if (pos == POS.ALL) {
        continue;
      }
      final long num = Utils.distance(dictionary.words(pos));
      //System.err.printf("%s num: %,d\n", pos, num);
    }
  }

  //@Ignore
  @Test
  public void sortUnique() {
    logTest("sortUnique");
    for (final POS pos : POS.CATS) {
      //System.err.println(pos+" words isSorted");
      assertTrue(pos+" words not sorted?", Utils.isSorted(dictionary.words(pos)));
    }
    for (final POS pos : POS.CATS) {
      //System.err.println(pos+" words isUnique");
      assertTrue(pos+" words not unique?", Utils.isUnique(dictionary.words(pos), false));
    }
    for (final POS pos : POS.CATS) {
      //System.err.println(pos+" synsets isSorted");
      assertTrue(pos+" synsets not sorted?", Utils.isSorted(dictionary.synsets(pos)));
    }
    for (final POS pos : POS.CATS) {
      //System.err.println(pos+" synsets isUnique");
      assertTrue(pos+" synsets not unique?", Utils.isUnique(dictionary.synsets(pos), false));
    }
    for (final POS pos : POS.CATS) {
      //System.err.println(pos+" wordSenses isSorted");
      assertTrue(pos+" wordSenses not sorted?", Utils.isSorted(dictionary.wordSenses(pos)));
    }
    for (final POS pos : POS.CATS) {
      //System.err.println(pos+" wordSenses isUnique");
      assertTrue(pos+" wordSenses not unique?", Utils.isUnique(dictionary.wordSenses(pos), false));
    }
    for (final POS pos : POS.CATS) {
      //System.err.println(pos+" relations isSorted");
      assertTrue(pos+" relations not sorted?", Utils.isSorted(dictionary.relations(pos)));
    }
    for (final POS pos : POS.CATS) {
      //System.err.println(pos+" relations isUnique");
      assertTrue(pos+" relations not unique?", Utils.isUnique(dictionary.relations(pos), false));
    }
    //System.err.println("allPOSAllIterationsSortUniqueTests() passed");
  }

  //@Ignore // this test is kinda slow
  @Test
  public void sequentialIterations() {
    logTest("sequentialIterations");
    // iterate through Synset's of dictionary
    int n = 2;
    System.err.println("starting "+n+" full iterations...");
    int totalWordsVisited = 0;
    try {
      for (int i = 0; i < n; i++) {
        int iterationWordsVisited = 0;
        int iterationIndexWordsVisited = 0;
        int iterationGlossLetters = 0;
        int iteration_total_p_cnt = 0;
        int numCore = 0;
        int numNounCore = 0;
        int numVerbCore = 0;
        int numAdjCore = 0;
        for (final POS pos : POS.CATS) {
          for (final Word word : dictionary.words(pos)) {
            for (final Synset synset : word.getSynsets()) {
              iterationGlossLetters += synset.getGloss().length();
            }
            iterationIndexWordsVisited++;
            final Set<RelationType> relationTypes = word.getRelationTypes();
            if (relationTypes.contains(RelationType.ATTRIBUTE)) {
              //System.err.println("found ATTRIBUTE for word: "+word);
            }
            iteration_total_p_cnt += relationTypes.size();
            for (final WordSense wordSense : word.getWordSenses()) {
              //final String lemma = word.getLowercasedLemma();
              final Synset synset = wordSense.getSynset();
              final int taggedFreq = wordSense.getSensesTaggedFrequency();
              final int coreRank = wordSense.getCoreRank();
              if (coreRank > 0) {
//                System.out.println(wordSense.getSenseKey());
                numCore++;
                switch (pos) {
                  case NOUN: numNounCore++; break;
                  case VERB: numVerbCore++; break;
                  case ADJ: numAdjCore++; break;
                  default: throw new IllegalStateException();
                }
              }
              //String msg = i+" "+word+" taggedFreq: "+taggedFreq;
              //System.err.println(msg);
              String longMsg = wordSense.getLongDescription();
              //System.err.println(longMsg);
              if (pos == POS.VERB) {
                for (final String frame : wordSense.getVerbFrames()) {
                  String vframe = "  VFRAME: "+frame;
                  //System.err.println(vframe);
                }
              }
              final WordSense.AdjPosition adjPosFlag = wordSense.getAdjPosition();
              if (adjPosFlag != WordSense.AdjPosition.NONE) {
                assertTrue(pos == POS.ADJ);
                //System.err.println(longMsg);
                //System.err.println("AdjPositionFlags: "+adjPosFlag);
              }
              totalWordsVisited++;
              iterationWordsVisited++;
            }
          }
        }
        // numCore: 4997 (getting -- missing )
        // numNounCore: 3299 (getting 3289 -- missing 10!?)
        // numVerbCore: 1000 (getting 998 -- missing 2)
        // numAdjCore: 698 (getting 674 -- missing 24)
        System.err.printf("numCore: %d numNounCore: %d numVerbCore: %d numAdjCore: %d\n",
          numCore, numNounCore, numVerbCore, numAdjCore);
//        assertEquals(4997, numCore);
        // apparently 36 of the sensekeys in this data are invalid with respect to WordNet 3.0
        assertEquals(4961, numCore);
        printMemoryUsage();
        System.err.println("iterationIndexWordsVisited: " + iterationIndexWordsVisited+
            " iteration_total_p_cnt: " + iteration_total_p_cnt+
            " avg p_cnt: " + (((double)iteration_total_p_cnt)/iterationIndexWordsVisited));
        System.err.println("iterationGlossLetters: " + iterationGlossLetters);
        assertEquals((i + 1) * iterationWordsVisited, totalWordsVisited);
      }
    } finally {
      System.err.println("done with "+n+" full iterations.  totalWordsVisited: "+totalWordsVisited);
    }
  }

  /** @see ThreadSafetyTest */
  @Ignore
  @Test
  public void parallelIterationTest() {
    //TODO implement parallelIterationTest
    // start 2 iterators and increment each in "lock step"
    // and verify their equivalence
  }

  //@Ignore
  @Test
  public void words() {
    logTest("words");
    for (final Word word : dictionary.words(POS.ALL)) {
      final String s = word.toString();
      //System.err.println(s);
    }
  }
  //@Ignore
  @Test
  public void synsets() {
    logTest("synsets");
    for (final Synset synset : dictionary.synsets(POS.ALL)) {
      String s = synset.toString();
      //System.err.println(s);
    }
  }
  //@Ignore
  @Test
  public void wordSenses() {
    logTest("wordSenses");
    for (final WordSense wordSense : dictionary.wordSenses(POS.ALL)) {
      final String s = wordSense.toString();
      //System.err.println(s);
    }
  }
  //@Ignore
  @Test
  public void relations() {
    logTest("relations");
    for (final Relation relation : dictionary.relations(POS.ALL)) {
      final String s = relation.toString();
      //System.err.println(s);
    }
  }

  /**
   * Look for warning issues with lookupSynsets()
   */
  @Test
  public void lookupSynsets() {
    logTest("lookupSynsets");
    for (final Word word : dictionary.words(POS.ALL)) {
      final String str = word.getLowercasedLemma();
      // exhaustive -- all POS
      for (final POS pos : POS.CATS) {
        final List<Synset> syns = dictionary.lookupSynsets(str, pos);
        if (pos == word.getPOS()) {
          assertTrue("loopback failure", ! syns.isEmpty());
        }
      }
    }
  }

  void logTest(final String methodName) {
    //System.err.printf("%s %tT\n", methodName, System.currentTimeMillis());
    //System.err.printf("%s %tT\n", methodName, Calendar.getInstance());
    //System.err.printf("%s %1$tT%1$tT\n", methodName, Calendar.getInstance());
    final String time = String.format("%1$tH:%1$tM:%1$tS:%1$tL", System.currentTimeMillis());
    System.err.printf("%-30s %s\n", methodName, time);
  }

  static void printMemoryUsage() {
    System.err.println("heap: "+ManagementFactory.getMemoryMXBean().getHeapMemoryUsage());
    //System.err.println("non-heap: "+ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage());
    for (final MemoryPoolMXBean memPool : ManagementFactory.getMemoryPoolMXBeans()) {
      if (memPool.getType() != MemoryType.HEAP) {
        continue;
      }
      System.err.println("  "+memPool.getName()+/*" "+memPool.getType()+*/" peak: "+memPool.getPeakUsage());//+" "+memPool);
    }
  }

  private static boolean containsLemma(final Iterable<Word> words, final String lemma) {
    for (final Word word : words) {
      if (word.getLowercasedLemma().equals(lemma)) {
        return true;
      }
    }
    return false;
  }
}