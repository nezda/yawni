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
 * Goal: verify various iteration methods of dictiony behave as expected
 */
public class IterationTest {

  private static DictionaryDatabase dictionary;
  @BeforeClass
  public static void init() {
    dictionary = FileBackedDictionary.getInstance();
  }

  // test searching iterators
  //   searchIndexBeginning()
  //   searchIndexWords()
  /** <b>Parts of this test uses hard coded values for WordNet 3.0 </b> */
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

  @Ignore // this test is kinda slow
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
              //String msg = i+" "+word;
              //System.err.println(msg);
              String longMsg = wordSense.getLongDescription();
              //System.err.println(longMsg);
              if (pos == POS.VERB) {
                for (final String frame : wordSense.getVerbFrames()) {
                  String vframe = "  VFRAME: "+frame;
                  //System.err.println(vframe);
                }
              }
              final Set<WordSense.AdjPosition> adjPosFlags = wordSense.getAdjPositions();
              if (adjPosFlags.isEmpty() == false) {
                //System.err.println(longMsg);
                //System.err.println("AdjPositionFlags: "+adjPosFlags);
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
  
  public static junit.framework.Test suite() {
    return new JUnit4TestAdapter(IterationTest.class);
  }
}
