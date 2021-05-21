/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
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
    private int relationsVisited;

    Antagonizer(final int id, final Semaphore semaphore) { 
      super("Antagonizer "+id);
      this.id = id;
      this.semaphore = semaphore;
    }

    boolean sameCounts(final Antagonizer that) {
      return 
        this.wordsVisited == that.wordsVisited &&
        this.wordSensesVisited == that.wordSensesVisited &&
        this.relationsVisited == that.relationsVisited;
    }
    @Override
    public String toString() {
      return "[Antagonizer: " + id +
        " wordsVisited: " + wordsVisited +
        " wordSensesVisited: " + wordSensesVisited +
        " relationsVisited: " + relationsVisited + "]";
    }

    protected void antagonize() {
      System.err.println(Thread.currentThread()+" starting...");
      final WordNetInterface dictionary = WordNet.getInstance();
      try {
        for (final Word word : dictionary.words(POS.ALL)) {
          ++wordsVisited;
          for (final WordSense wordSense : word.getWordSenses()) {
            final String lemma = wordSense.getLemma();
            final Synset synset = wordSense.getSynset();
            String msg = id + " " + wordSense;
            //System.err.println(msg);
            ++wordSensesVisited;
          }
          for (final Synset synset : word.getSynsets()) {
            for (final Relation relation : synset.getRelations()) {
              relation.getTarget();
              // note these are not unique - they are visited from both sides
              ++relationsVisited;
            }
          }
        }
      } finally {
        System.err.println(this);
      }
    }

    @Override
    public void run() {
      try {
        antagonize();
      } finally {
        semaphore.release();
      }
    }
  } // end class Antagonizer

  @Test
  public void test1() {
    String time;
    time = String.format("%1$tH:%1$tM:%1$tS:%1$tL", System.currentTimeMillis());
    System.err.printf("%-30s %s\n", "threadSafe", time);
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
    time = String.format("%1$tH:%1$tM:%1$tS:%1$tL", System.currentTimeMillis());
    System.err.printf("%-30s %s\n", "done", time);
  }
}