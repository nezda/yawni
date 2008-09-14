package edu.brandeis.cs.steele.wn;

import junit.framework.JUnit4TestAdapter;
import org.junit.*;
import static org.junit.Assert.*;
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

    private int wordsVisited;
    private int wordSensesVisited;
    private int pointersVisited;

    Antagonizer(final int id, final Semaphore semaphore) { 
      super("Antagonizer "+id);
      this.id = id;
      this.semaphore = semaphore;
    }

    boolean sameCounts(final Antagonizer that) {
      return 
        this.wordsVisited == that.wordsVisited &&
        this.wordSensesVisited == that.wordSensesVisited &&
        this.pointersVisited == that.pointersVisited;
    }
    @Override public String toString() {
      return "[Antagonizer: " + id +
        " wordsVisited: " + wordsVisited +
        " wordSensesVisited: " + wordSensesVisited +
        " pointersVisited: " + pointersVisited + "]";
    }

    protected void antagonize() {
      System.err.println(Thread.currentThread()+" starting...");
      final DictionaryDatabase dictionary = FileBackedDictionary.getInstance();
      try {
        for (final POS pos : POS.CATS) {
          for (final Word word : dictionary.words(pos)) {
            ++wordsVisited;
            for (final WordSense wordSense : word.getSenses()) {
              final String lemma = wordSense.getLemma();
              final Synset synset = wordSense.getSynset();
              String msg = id+" "+wordSense;
              //System.err.println(msg);
              ++wordSensesVisited;
            }
            for (final Synset synset : word.getSynsets()) {
              for (final Pointer pointer : synset.getPointers()) {
                pointer.getTarget();
                // note these are not unique - they are visited from both sides
                ++pointersVisited;
              }
            }
          }
        }
      } finally {
        System.err.println(this);
      }
    }

    @Override public void run() {
      try {
        antagonize();
      } finally {
        semaphore.release();
      }
    }
  } // end class Antagonizer

  @Test
  public void test1() {
    // synchronization strategy: CountDownLatch implemented with Semaphore
    // acquire 1 - n permits
    // start n threads
    //   release permit (n times: implies 1 permit remains after last completion)
    // acquire 1 permit
    // 3 Antagonizer's takes a while to run
    final int numAntagonizers = 3;
    final Semaphore finisher = new Semaphore(1 - numAntagonizers, true /* fair */);
    final Antagonizer[] antagonizers = new Antagonizer[numAntagonizers];
    for (int i = 0; i < numAntagonizers; i++) {
      antagonizers[i] = new Antagonizer(i, finisher);
    }
    for (final Antagonizer antagonizer : antagonizers) {
      antagonizer.start();
    }
    // re-acquire all permits and exit
    try {
      finisher.acquire();
    } catch(InterruptedException ie) {
      throw new RuntimeException(ie);
    }
    for (int i = 1; i < numAntagonizers; i++) {
      assertTrue(antagonizers[0].sameCounts(antagonizers[i]));
    }
    System.err.println("done");
  }

  public static junit.framework.Test suite() {
    return new JUnit4TestAdapter(ThreadSafetyTest.class);
  }
}
