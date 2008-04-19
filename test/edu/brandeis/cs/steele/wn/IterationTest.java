package edu.brandeis.cs.steele.wn;

import junit.framework.JUnit4TestAdapter;
import org.junit.*;
import static org.junit.Assert.*;
import java.util.*;
import java.util.concurrent.*;
import java.lang.management.*;

/**
 * Goal: verify various iteration methods of dictiony behave as expected
 */
public class IterationTest {
  // test searching iterators
  //   searchIndexBeginning()
  //   searchIndexWords()
  /** <b>Parts of this test uses hard coded values for WordNet 3.0 </b> */
  @Test
  public void wordIterationBoundaryTests() {
    // check if iteration returns first AND last item (boundary cases) 
    // - look at data files manually ($WNHOME/dict/index.<pos>)
    // TODO check this for all POS
    final DictionaryDatabase dictionary = FileBackedDictionary.getInstance();
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
    for(final Word word : nounIndexWords) {
      last = word;
    }
    //System.err.println("last: "+last);
    assertEquals(4786625, last.getOffset());
    assertEquals("zyrian", last.getLemma());

    assertEquals(first, nounIndexWords.iterator().next()); 
    // IF iteration used caching, this might or might not be the case
    assertTrue("not distinct objects?", first != nounIndexWords.iterator().next()); 
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
      for(int i=0; i < n; ++i) {
        int iterationWordsVisited = 0;
        int iterationIndexWordsVisited = 0;
        int iterationGlossLetters = 0;
        int iteration_total_p_cnt = 0;
        for(final POS pos : POS.CATS) {
          for(final Word word : dictionary.words(pos)) {
            for(final Synset synset : word.getSynsets()) {
              iterationGlossLetters += synset.getGloss().length();
            }
            ++iterationIndexWordsVisited;
            iteration_total_p_cnt += word.getPointerTypes().size();
            for(final WordSense wordSense : word.getSenses()) {
              //final String lemma = word.getLemma();
              final Synset synset = wordSense.getSynset();
              //String msg = i+" "+word;
              //System.err.println(msg);
              String longMsg = wordSense.getLongDescription();
              //System.err.println(longMsg);
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
    for(final MemoryPoolMXBean memPool : ManagementFactory.getMemoryPoolMXBeans()) {
      if(memPool.getType() != MemoryType.HEAP) {
        continue;
      }
      System.err.println("  "+memPool.getName()+/*" "+memPool.getType()+*/" peak: "+memPool.getPeakUsage());//+" "+memPool);
    }
  }

  @Test
  public void parallelIterationTest() {
    // TODO implement parallelIterationTest
    // start 2 iterators and increment each in "lock step"
  }
  
  public static junit.framework.Test suite() {
    return new JUnit4TestAdapter(IterationTest.class);
  }
}
