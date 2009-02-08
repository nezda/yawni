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
package edu.brandeis.cs.steele.wn;

import org.junit.*;
import static org.junit.Assert.*;

public class SynsetTest {
  private static DictionaryDatabase dictionary;
  @BeforeClass
  public static void init() {
    dictionary = FileBackedDictionary.getInstance();
  }

  @Test
  public void testDescriptions() {
    for (final POS pos : POS.CATS) {
      int count = 0;
      for (final Synset synset : dictionary.synsets(pos)) {
        ++count;
        //if(++count > 10) break;
        // exercise toString() and getGloss()
        final String msg = count+" "+synset+"\n  "+synset.getGloss();
        //System.err.println(msg);
        // exercise normal and long description with and without verbose
        final String msg2 = count+" "+synset+"\n  "+synset.getDescription();
        final String msg3 = count+" "+synset+"\n  "+synset.getDescription(true);
        final String msg4 = count+" "+synset+"\n  "+synset.getLongDescription();
        final String msg5 = count+" "+synset+"\n  "+synset.getLongDescription(true);
      }
    }
  }

  @Test
  public void test2() {
    // adj up#7 member of noun TOPIC computer#1
    final Word word = dictionary.lookupWord("up", POS.ADJ);
    System.err.println("word: "+word+" pointerTypes: "+word.getPointerTypes());
    System.err.println("  "+word.getSense(7).getLongDescription());
    //assertTrue(
    //PointerType pointerType = PointerType.DOMAIN;
    final PointerType[] pointerTypes = new PointerType[]{ 
      /*PointerType.DOMAIN, PointerType.MEMBER_OF_TOPIC_DOMAIN,*/ PointerType.DOMAIN_OF_TOPIC, //PointerType.MEMBER_OF_THIS_DOMAIN_TOPIC
    };
    for (final PointerType pointerType : pointerTypes) {
      for (final PointerTarget target : word.getSense(7).getSynset().getTargets(pointerType)) {
        System.err.println(pointerType + " target: " + target);
      }
    }
  }

  @Test
  public void test3() {
    // adj low-pitch#1 is attribute of "pitch"
    final Word word = dictionary.lookupWord("low-pitched", POS.ADJ);
    System.err.println("word: "+word+" pointerTypes: "+word.getPointerTypes());
    System.err.println("  "+word.getSense(1).getLongDescription());
    //assertTrue(
    //PointerType pointerType = PointerType.DOMAIN;
    final PointerType[] pointerTypes = new PointerType[] { 
      PointerType.ATTRIBUTE, 
    };
    for (final PointerType pointerType : pointerTypes) {
      for (final PointerTarget target : word.getSense(1).getSynset().getTargets(pointerType)) {
        System.err.println("  "+pointerType+" target: "+target);
      }
    }
  }

  @Test
  public void testInstances() {
    // adj low-pitch#1 is attribute of "pitch"
    final Word word = dictionary.lookupWord("George Bush", POS.NOUN);
    System.err.println("word: "+word+" pointerTypes: "+word.getPointerTypes());
    System.err.println("  "+word.getSense(1).getLongDescription());
    final PointerType[] pointerTypes = new PointerType[] { 
      PointerType.HYPONYM, 
      PointerType.INSTANCE_HYPONYM,
      PointerType.HYPERNYM, 
      PointerType.INSTANCE_HYPERNYM,
    };
    for (final PointerType pointerType : pointerTypes) {
      for (final PointerTarget target : word.getSense(1).getSynset().getTargets(pointerType)) {
        System.err.println("  " + pointerType + " target: " + target);
      }
    }
  }
}
