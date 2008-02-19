package edu.brandeis.cs.steele.wn;

import junit.framework.TestCase;
import java.util.*;
import java.util.concurrent.*;

/**
 * Goal: run multiple parallel iterations over the dictionary
 * and ensure isolation which will be detected by absence of
 * exceptions and expected input for given output.  Also stresses
 * memory usage demanded by the system.
 */
public class ThreadSafetyTest extends TestCase {
  private static <T> Iterable<T> iterablize(final Iterator<T> iterator) {
    return new Iterable<T>() {
      public Iterator<T> iterator() {
        return iterator;
      }
    };
  }

  static class Antagonizer extends Thread {
    private final int id;
    private final Semaphore semaphore;

    Antagonizer(final int id, final Semaphore semaphore) { 
      this.id = id;
      this.semaphore = semaphore;
      System.err.println(id+" Antagonizer created");
    }

    protected void antagonize1() {
      System.err.println(id+" antagonizer antagonize()");
    }

    protected void antagonize() {
      final DictionaryDatabase dictionary = FileBackedDictionary.getInstance();
      // iterate through Synset's of dictionary
      // iterate through XXX
      for(final POS pos : POS.CATS) {
        for(final IndexWord indexWord : iterablize(dictionary.indexWords(pos))) {
          for(final Word word : indexWord.getSenses()) {
            final String lemma = word.getLemma();
            final Synset synset = word.getSynset();
            String msg = id+" "+word;
            //System.err.println(msg);
          }
        }
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
  // 2 Antagonizers detected errors:
  // [junit] SEVERE: IndexWord parse error on line:
  // [junit]  n 1 3 @ #m %m 1 0 12301917
  // [junit] SEVERE: IndexWord parse error on line:
  // [junit]  2 \ + 1 0 02658412
  // [junit] SEVERE: IndexWord parse error on line:
  // [junit]  00457072
  // not reproducible with 1 Antagonizer
}
