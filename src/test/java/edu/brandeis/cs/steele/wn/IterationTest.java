package edu.brandeis.cs.steele.wn;

import junit.framework.JUnit4TestAdapter;
import org.junit.*;
import static org.junit.Assert.*;
import java.util.*;
import java.util.concurrent.*;
import java.lang.management.*;

import edu.brandeis.cs.steele.util.Utils;
import edu.brandeis.cs.steele.util.MergedIterable;

/**
 * Goal: verify various iteration methods of dictionary behave as expected.
 */
public class IterationTest {
  private static DictionaryDatabase dictionary;
  private static Random rand;
  @BeforeClass
  public static void init() {
    dictionary = FileBackedDictionary.getInstance();
    rand = new Random(0);
  }

  /** 
   * test searching iterators
   * - searchByPrefix()
   * - searchBySubstring()
   * <b>Parts of this test uses hard coded values for WordNet 3.0</b> 
   */
  @Test
  public void testSearchIterators() {
    // plan
    // + search for first word
    // + search for first word's prefix
    // + search for last word
    // + search for last word's prefix
    // + search after last word (non-existant)
    // + search before first word (non-existant)
    // + iterate over words, iterate over prefixes
    //   + assert containsLemma(Iterable<Word> matches)
    final Iterable<Word> firstWord = dictionary.searchByPrefix(POS.NOUN, "'hood");
    final Word first = firstWord.iterator().next();
    assertEquals("'hood", first.getLemma());
    final Iterable<Word> firstWordPrefix = dictionary.searchByPrefix(POS.NOUN, "'hoo");
    final Word firstPrefix = firstWordPrefix.iterator().next();
    assertEquals("'hood", firstPrefix.getLemma());
    final Iterable<Word> preFirstWord = dictionary.searchByPrefix(POS.NOUN, "''");
    assertFalse(preFirstWord.iterator().hasNext());

    final Iterable<Word> lastWord = dictionary.searchByPrefix(POS.NOUN, "zyrian");
    final Word last = lastWord.iterator().next();
    assertEquals("zyrian", last.getLemma());
    final Iterable<Word> lastWordPrefix = dictionary.searchByPrefix(POS.NOUN, "zyria");
    final Word lastPrefix = lastWordPrefix.iterator().next();
    assertEquals("zyrian", lastPrefix.getLemma());
    final Iterable<Word> postLastWordPrefix = dictionary.searchByPrefix(POS.NOUN, "zz");
    assertFalse(postLastWordPrefix.iterator().hasNext());

    for (final POS pos : POS.CATS) {
      final Iterable<Word> leadingDashPrefix = dictionary.searchByPrefix(pos, "-");
      assertFalse(leadingDashPrefix.iterator().hasNext());
      final Iterable<String> leadingDashPrefixLemma = new WordToLemma(dictionary.searchByPrefix(pos, "-"));
      assertFalse(leadingDashPrefixLemma.iterator().hasNext());
      final Iterable<Word> leadingSpacePrefix = dictionary.searchByPrefix(pos, " ");
      assertFalse(leadingSpacePrefix.iterator().hasNext());
      final Iterable<Word> emptyPrefix = dictionary.searchByPrefix(pos, "");
      assertFalse(leadingSpacePrefix.iterator().hasNext());
    }

    final Iterable<Word> anyEmptyPrefix = MergedIterable.merge(true,
        dictionary.searchByPrefix(POS.NOUN, "-"),
        dictionary.searchByPrefix(POS.VERB, "-"),
        dictionary.searchByPrefix(POS.ADJ, "-"),
        dictionary.searchByPrefix(POS.ADV, "-"));
    assertFalse(anyEmptyPrefix.iterator().hasNext());
    
    final Iterable<Word> anyNonExistantPrefix = MergedIterable.merge(true,
        dictionary.searchByPrefix(POS.NOUN, "lllllll"),
        dictionary.searchByPrefix(POS.VERB, "lllllll"),
        dictionary.searchByPrefix(POS.ADJ, "lllllll"),
        dictionary.searchByPrefix(POS.ADV, "lllllll"));
    assertFalse(anyNonExistantPrefix.iterator().hasNext());

    final Iterable<Word> anyLeadingDashPrefix = MergedIterable.merge(true,
        dictionary.searchByPrefix(POS.NOUN, ""),
        dictionary.searchByPrefix(POS.VERB, ""),
        dictionary.searchByPrefix(POS.ADJ, ""),
        dictionary.searchByPrefix(POS.ADV, ""));
    assertFalse(anyEmptyPrefix.iterator().hasNext());

    final Iterable<String> runs = Utils.uniq(
        new WordToLemma(MergedIterable.merge(true,
            dictionary.searchByPrefix(POS.NOUN, "run"),
            dictionary.searchByPrefix(POS.VERB, "run"))));
    assertTrue(Utils.isUnique(runs, true));

    // this is sped up by only searching for randomized prefixes
    // only for randomized terms, but keep it deterministic
    // with a fixed randomizer seed
    final float prefixDitchProportion = 0.9f;
    int numPrefixTests = 0;
    final POS pos = POS.NOUN;
    for (final Word word : dictionary.words(pos)) {
      if (rand.nextFloat() < prefixDitchProportion) {
        continue;
      }
      final String lemma = word.getLemma();
      for (int i = 1, n = lemma.length(); i < n; i++) {
        final String prefix = lemma.substring(0, i);
        final Iterable<Word> matches = dictionary.searchByPrefix(pos, lemma);
        numPrefixTests++;
        assertTrue(containsLemma(matches, lemma));
      }
    }
    //System.err.println("numPrefixTests: "+numPrefixTests);

    final Iterable<Word> spaceWords = dictionary.searchBySubstring(POS.NOUN, " ");
    assertTrue(spaceWords.iterator().hasNext());

    final Iterable<Word> anyNonExistantSubstring = MergedIterable.merge(true,
        dictionary.searchBySubstring(POS.NOUN, "lllllll"),
        dictionary.searchBySubstring(POS.VERB, "lllllll"),
        dictionary.searchBySubstring(POS.ADJ, "lllllll"),
        dictionary.searchBySubstring(POS.ADV, "lllllll"));
    assertFalse(anyNonExistantSubstring.iterator().hasNext());

    final float substringDitchProportion = 0.9999f;
    int numSubstringTests = 0;
    //final POS pos = POS.NOUN;
    for (final Word word : dictionary.words(pos)) {
      if (rand.nextFloat() < substringDitchProportion) {
        continue;
      }
      final String lemma = word.getLemma();
      for (int i = 1, n = lemma.length(); i < n; i++) {
        final String prefix = lemma.substring(0, i);
        final Iterable<Word> matches = dictionary.searchBySubstring(pos, lemma);
        numSubstringTests++;
        assertTrue(containsLemma(matches, lemma));
        //System.err.println("numSubstringTests: "+numSubstringTests);
      }
    }
    //System.err.println("numSubstringTests: "+numSubstringTests);
  }

  private static boolean containsLemma(final Iterable<Word> words, final String lemma) {
    for (final Word word : words) {
      if (word.getLemma().equals(lemma)) {
        return true;
      }
    }
    return false;
  }

  /** <b>Parts of this test uses hard coded values for WordNet 3.0</b> */
  @Test
  public void wordIterationBoundaryTests() {
    // check if iteration returns first AND last item (boundary cases) 
    // - look at data files manually ($WNHOME/dict/index.<pos>)
    // TODO check this for all POS
    final Iterable<Word> nounIndexWords = dictionary.words(POS.NOUN);
    final Word first = nounIndexWords.iterator().next();
    //System.err.println("first: "+first);
    // to get these offsets with gvim, open the data file, put the cursor on
    // the first char of the line you expect (e.g. first content line, last
    // line), and g CTRL-g will report "Byte n of m" -- n is a 1 based file
    // offset - Word offsets are zero based so you would expect n-1
    assertEquals(1740, first.getOffset());
    assertEquals("'hood", first.getLemma());
    Word last = null;
    for (final Word word : nounIndexWords) {
      last = word;
    }
    //System.err.println("last: "+last);
    assertEquals(4786625, last.getOffset());
    assertEquals("zyrian", last.getLemma());

    assertEquals(first, nounIndexWords.iterator().next()); 
    // IF iteration used caching, this might or might not be the case
    assertTrue("not distinct objects?", first != nounIndexWords.iterator().next()); 
  }

  //@Ignore
  @Test
  public void exoticPOS() {
    System.err.println("exoticPOS()");
    //for(final Word word : dictionary.words(POS.SAT_ADJ)) {
    //  System.err.println(word);
    //}
    // SAT_ADJ this doesn't seem to have any members ??
    for (final POS pos : POS.values()) {
      if (pos == POS.ALL) {
        continue;
      }
      final long num = Utils.distance(dictionary.words(pos));
      System.err.printf("%s num: %,d\n", pos, num);
    }
  }

  //@Ignore
  @Test
  public void allPOSAllIterationsSortUniqueTests() {
    System.err.println("allPOSAllIterationsSortUniqueTests()");
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
      //System.err.println(pos+" pointers isSorted");
      assertTrue(pos+" pointers not sorted?", Utils.isSorted(dictionary.pointers(pos)));
    }
    for (final POS pos : POS.CATS) {
      //System.err.println(pos+" pointers isUnique");
      assertTrue(pos+" pointers not unique?", Utils.isUnique(dictionary.pointers(pos), false));
    }
    //System.err.println("allPOSAllIterationsSortUniqueTests() passed");
  }

  //@Ignore // this test is kinda slow
  @Test
  public void sequentialIterationTest() {
    final DictionaryDatabase dictionary = FileBackedDictionary.getInstance();
    // iterate through Synset's of dictionary
    int n = 2;
    System.err.println("starting "+n+" full iterations...");
    int totalWordsVisited = 0;
    try {
      for (int i=0; i < n; ++i) {
        int iterationWordsVisited = 0;
        int iterationIndexWordsVisited = 0;
        int iterationGlossLetters = 0;
        int iteration_total_p_cnt = 0;
        for (final POS pos : POS.CATS) {
          for (final Word word : dictionary.words(pos)) {
            for (final Synset synset : word.getSynsets()) {
              iterationGlossLetters += synset.getGloss().length();
            }
            ++iterationIndexWordsVisited;
            final EnumSet<PointerType> pointerTypes = word.getPointerTypes();
            if (pointerTypes.contains(PointerType.ATTRIBUTE)) {
              //System.err.println("found ATTRIBUTE for word: "+word);
            }
            iteration_total_p_cnt += pointerTypes.size();
            for (final WordSense wordSense : word.getSenses()) {
              //final String lemma = word.getLemma();
              final Synset synset = wordSense.getSynset();
              final int taggedFreq = wordSense.getSensesTaggedFrequency();
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
                //System.err.println(longMsg);
                //System.err.println("AdjPositionFlags: "+adjPosFlag);
              }
              ++totalWordsVisited;
              ++iterationWordsVisited;
            }
          }
        }
        printMemoryUsage();
        System.err.println("iterationIndexWordsVisited: "+iterationIndexWordsVisited+
            " iteration_total_p_cnt: "+iteration_total_p_cnt+
            " avg p_cnt: "+(((double)iteration_total_p_cnt)/iterationIndexWordsVisited));
        System.err.println("iterationGlossLetters: "+iterationGlossLetters);
        assertEquals((i+1) * iterationWordsVisited, totalWordsVisited);
      }
    } finally {
      System.err.println("done with "+n+" full iterations.  totalWordsVisited: "+totalWordsVisited);
    }
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

  @Test
  public void parallelIterationTest() {
    //TODO implement parallelIterationTest
    // start 2 iterators and increment each in "lock step"
    // and verify their equivalence
  }

  //@Ignore
  @Test
  public void words() {
    System.err.println("allPOSWordsTest()");
    for (final Word word : MergedIterable.merge(true,
          dictionary.words(POS.NOUN),
          dictionary.words(POS.VERB),
          dictionary.words(POS.ADJ),
          dictionary.words(POS.ADV))) {
      final String s = word.toString();
      //System.err.println(s);
    }
  }
  //@Ignore
  @Test
  public void synsets() {
    System.err.println("allPOSSynsetsTest()");
    for (final Synset synset : MergedIterable.merge(true,
          dictionary.synsets(POS.NOUN),
          dictionary.synsets(POS.VERB),
          dictionary.synsets(POS.ADJ),
          dictionary.synsets(POS.ADV))) {
      String s = synset.toString();
      //System.err.println(s);
    }
  }
  //@Ignore
  @Test
  public void wordSenses() {
    System.err.println("allPOSWordSensesTest()");
    for (final WordSense wordSense : MergedIterable.merge(true,
          dictionary.wordSenses(POS.NOUN),
          dictionary.wordSenses(POS.VERB),
          dictionary.wordSenses(POS.ADJ),
          dictionary.wordSenses(POS.ADV))) {
      final String s = wordSense.toString();
      //System.err.println(s);
    }
  }
  //@Ignore
  @Test
  public void pointers() {
    System.err.println("allPOSPointersTest()");
    for (final Pointer pointer : MergedIterable.merge(true,
          dictionary.pointers(POS.NOUN),
          dictionary.pointers(POS.VERB),
          dictionary.pointers(POS.ADJ),
          dictionary.pointers(POS.ADV))) {
      final String s = pointer.toString();
      //System.err.println(s);
    }
  }

  /**
   * Look for warning issues with lookupSynsets()
   */
  @Test
  public void lookupSynsetsTest() {
    for (final Word word : dictionary.words(POS.ALL)) {
      String str = word.getLemma();
      // exhaustive -- all POS
      for(POS pos : POS.CATS) {
        Synset[] syns = dictionary.lookupSynsets(pos, str);
        if(pos == word.getPOS()) {
          assertTrue("loopback failure", syns.length > 0);
        }
      }
      // just our source POS
      //Synset[] syns = dictionary.lookupSynsets(word.getPOS(), str);
      //if (syns.length == 0) {
      //  System.err.println("XXX PROBLEM: "+str+" no syns found (loopback failure)");
      //}
      //System.err.println(str+": "+Arrays.toString(syns));
    }
  }
  
  public static junit.framework.Test suite() {
    return new JUnit4TestAdapter(IterationTest.class);
  }
}
