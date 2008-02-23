package edu.brandeis.cs.steele.wn;

import junit.framework.JUnit4TestAdapter;
import org.junit.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Goal: run multiple parallel iterations over the dictionary
 * and ensure isolation which will be detected by absence of
 * exceptions and expected input for given output.  Also stresses
 * memory usage demanded by the system.
 */
public class ThreadSafetyTest {
  static class Antagonizer extends Thread {
    private final int id;
    private final Semaphore semaphore;

    Antagonizer(final int id, final Semaphore semaphore) { 
      this.id = id;
      this.semaphore = semaphore;
    }

    protected void antagonize1() {
      System.err.println(id+" antagonizer antagonize()");
    }

    protected void antagonize() {
      System.err.println(id+" Antagonizer starting... "+Thread.currentThread());
      final DictionaryDatabase dictionary = FileBackedDictionary.getInstance();
      // iterate through Synset's of dictionary
      // iterate through XXX
      int wordsVisited = 0;
      int indexWordsVisited = 0;
      try {
        for(final POS pos : POS.CATS) {
          for(final IndexWord indexWord : dictionary.indexWords(pos)) {
            ++indexWordsVisited;
            for(final Word word : indexWord.getSenses()) {
              final String lemma = word.getLemma();
              final Synset synset = word.getSynset();
              String msg = id+" "+word;
              //System.err.println(msg);
              ++wordsVisited;
            }
          }
        }
      } finally {
        System.err.println("Antagonizer: "+id+
            " wordsVisited: "+wordsVisited+" indexWordsVisited: "+indexWordsVisited);
      }
    }

    @Override public void run() {
      try {
        antagonize();
      } finally {
        semaphore.release();
      }
    }
  }

  @Test
  public void test1() {
    // synchronization strategy: CountDownLatch implemented with Semaphore
    // acquire 1 - n permits
    // start n threads
    //   release permit (n times: implies 1 permit remains after last completion)
    // acquire 1 permit
    // 3 Antagonizer's takes a while to run
    final int numAntagonizers = 2;
    final Semaphore finisher = new Semaphore(1 - numAntagonizers, true /* fair */);
    final Antagonizer[] antagonizers = new Antagonizer[numAntagonizers];
    for(int i=0; i<numAntagonizers; ++i) {
      antagonizers[i] = new Antagonizer(i, finisher);
    }
    for(final Antagonizer antagonizer : antagonizers) {
      antagonizer.start();
    }
    // re-acquire all permits and exit
    try {
      finisher.acquire();
    } catch(InterruptedException ie) {
      throw new RuntimeException(ie);
    }
    System.err.println("done");
  }
  // possible bugs:
  // - 2 threads trying to use CharStream at once - insufficient synchronization
  // - NextLineCache ?

  public static junit.framework.Test suite() {
    return new JUnit4TestAdapter(ThreadSafetyTest.class);
  }
}
