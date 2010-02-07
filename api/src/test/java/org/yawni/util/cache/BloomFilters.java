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
package org.yawni.util.cache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import org.yawni.wordnet.DictionaryDatabase;
import org.yawni.wordnet.FileBackedDictionary;
import org.yawni.wordnet.POS;
import org.yawni.wordnet.Word;
import org.yawni.wordnet.WordNetLexicalComparator;

/**
 * Utility class to generate and serialize {@link BloomFilter}s representing the
 * content of a given WordNet version; these filters are typically packaged in the 
 * {@code yawni-data} jar artifact.
 */
class BloomFilters {
  public static void main(String[] args) throws Exception {
    final double fpProb = 0.001;
    final DictionaryDatabase dictionary = FileBackedDictionary.getInstance();
    for (final POS pos : POS.CATS) {
      int count = 0;
      for (final Word word : dictionary.words(pos)) {
        count++;
      }
      final BloomFilter<CharSequence> filter = new BloomFilter<CharSequence>(count, fpProb, WordNetLexicalComparator.TO_LOWERCASE_INSTANCE);

//      System.err.println(pos+" "+filter);
      for (final Word word : dictionary.words(pos)) {
        filter.add(word.getLowercasedLemma());
        assert filter.contains(word.getLowercasedLemma());
      }
//      int numExceptions = 0;
//      int numExceptionInstances = 0;
//      for (final List<String> exceptions : dictionary.exceptions(pos)) {
//        for (final String exception : exceptions) {
//          filter.add(exception);
//          assert filter.contains(exception);
//          if (null == dictionary.lookupWord(exception, pos)) {
//            numExceptions++;
//            numExceptionInstances += (exceptions.size() - 1);
//          }
//        }
//      }
//      System.err.printf("numExceptions: %,d numExceptionInstances: %,d\n",
//        numExceptions, numExceptionInstances);
      for (final Word word : dictionary.words(pos)) {
        assert filter.contains(word.getLowercasedLemma());
      }
      final String fname = pos.name()+".bloom";
      System.err.println(fname+" "+filter);
      serialize(filter, fname);
    }
    for (final POS pos : POS.CATS) {
      int count = 0;
      for (final List<String> exceptions : dictionary.exceptions(pos)) {
        //count += exceptions.size() - 1;
        count++;
      }
      final BloomFilter<CharSequence> filter = new BloomFilter<CharSequence>(count, fpProb, WordNetLexicalComparator.TO_LOWERCASE_INSTANCE);
      for (final List<String> exceptions : dictionary.exceptions(pos)) {
//        for (final String exception : exceptions.subList(1, exceptions.size())) {
        final String exception = exceptions.get(0);
//          assert exception.indexOf('_') < 0 : "exception: "+exception;
          assert exception.indexOf(' ') < 0 : "exception: "+exception;
          filter.add(exception);
          assert filter.contains(exception);
//        }
      }
      final String fname = pos.name()+".exc.bloom";
      System.err.println(fname+" "+filter);
      serialize(filter, fname);
    }
  }

  private static void serialize(final BloomFilter filter, final String fname) throws Exception {
      final ObjectOutputStream oos =
        new ObjectOutputStream(
          new BufferedOutputStream(
            new FileOutputStream(fname)));
      oos.writeObject(filter);
      oos.close();
      final ObjectInputStream ois =
        new ObjectInputStream(
          new BufferedInputStream(
            new FileInputStream(fname)));
      @SuppressWarnings("unchecked")
      final BloomFilter<CharSequence> resurrected = (BloomFilter<CharSequence>)ois.readObject();
      assert resurrected.equals(filter);
  }
}