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
import org.yawni.wn.DictionaryDatabase;
import org.yawni.wn.FileBackedDictionary;
import org.yawni.wn.POS;
import org.yawni.wn.Word;

/**
 * Utility class to generate and serialized BloomFilters representing the
 * content of a given WordNet version.  These are optionally
 * packaged in the data jar.
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
      final BloomFilter<CharSequence> filter = new BloomFilter<CharSequence>(count, fpProb);
      //FIXME need to provide customizable hashCode() since CharSequence's hashCode()
      // is not well defined

//      System.err.println(pos+" "+filter);
      for (final Word word : dictionary.words(pos)) {
        filter.add(word.getLemma());
        assert filter.contains(word.getLemma());
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
        assert filter.contains(word.getLemma());
      }
      System.err.println("XXX "+pos+" "+filter);
      final String fname = pos.name()+".bloom";
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
      BloomFilter<CharSequence> resurrected = (BloomFilter<CharSequence>)ois.readObject();
      System.err.println("equal?: "+resurrected.equals(filter));
//      assert resurrected.equals(filter);
    }
  }
}