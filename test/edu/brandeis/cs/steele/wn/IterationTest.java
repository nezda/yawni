package edu.brandeis.cs.steele.wn;

import junit.framework.JUnit4TestAdapter;
import org.junit.*;
import static org.junit.Assert.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Goal: verify various iteration methods of dictiony behave as expected
 */
public class IterationTest {
  /** <b>Parts of this test uses hard coded values for WordNet 3.0 </b> */
  @Test
  public void indexWordIterationBoundaryTests() {
    // check if iteration returns first AND last item (boundary cases) 
    // - look at data files manually ($WNHOME/dict/index.<pos>)
    // TODO check this for all POS
    final DictionaryDatabase dictionary = FileBackedDictionary.getInstance();
    final Iterable<IndexWord> nounIndexWords = dictionary.indexWords(POS.NOUN);
    final IndexWord first = nounIndexWords.iterator().next();
    //System.err.println("first: "+first);
    // to get these offsets with gvim, open the data file, put the cursor on
    // the first char of the line you expect (e.g. first content line, last
    // line), and g CTRL-g will report "Byte n of m" -- n is a 1 based file
    // offset - IndexWord offsets are zero based so you would expect n-1
    assertEquals(1740, first.offset);
    assertEquals("'hood", first.lemma);
    IndexWord last = null;
    for(final IndexWord indexWord : nounIndexWords) {
      last = indexWord;
    }
    //System.err.println("last: "+last);
    assertEquals(4786625, last.offset);
    assertEquals("zyrian", last.lemma);

    assertEquals(first, nounIndexWords.iterator().next()); 
    // IF iteration used caching, this might or might not be the case
    assertTrue("not distinct objects?", first != nounIndexWords.iterator().next()); 
  }

  @Ignore // this test is kinda slow
  @Test
  public void sequentialIterationTest() {
    final DictionaryDatabase dictionary = FileBackedDictionary.getInstance();
    // iterate through Synset's of dictionary
    // iterate through XXX
    int n = 2;
    System.err.println("starting "+n+" full iterations...");
    int totalWordsVisited = 0;
    try {
      for(int i=0; i < n; ++i) {
        int iterationWordsVisited = 0;
        for(final POS pos : POS.CATS) {
          for(final IndexWord indexWord : dictionary.indexWords(pos)) {
            for(final Word word : indexWord.getSenses()) {
              //final String lemma = word.getLemma();
              final Synset synset = word.getSynset();
              //String msg = i+" "+word;
              //System.err.println(msg);
              ++totalWordsVisited;
              ++iterationWordsVisited;
            }
          }
        }
        assertEquals((i+1) * iterationWordsVisited, totalWordsVisited);
      }
    } finally {
      System.err.println("done with "+n+" full iterations.  totalWordsVisited: "+totalWordsVisited);
    }
  }

  @Test
  public void parallelIterationTest() {
    // TODO
    // start 2 iterators and increment each in "lock step"
  }
  
  public static junit.framework.Test suite() {
    return new JUnit4TestAdapter(IterationTest.class);
  }
}
