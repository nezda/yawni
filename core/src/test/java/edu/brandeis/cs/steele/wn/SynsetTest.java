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

import java.util.List;
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
    System.err.println("testDescriptions");
    int count = 0;
    final int expectedCount = 117659;
    for (final Synset synset : dictionary.synsets(POS.ALL)) {
      ++count;
      //if(++count > 10) break;
      // exercise toString() and getGloss()
      final String msg = count+" "+synset+"\n  "+synset.getGloss();
      //System.err.println(msg);
      // exercise normal and long description with and without verbose
      //TODO assert something here, don't just exercise
      final String msg2 = count+" "+synset+"\n  "+synset.getDescription();
      final String msg3 = count+" "+synset+"\n  "+synset.getDescription(true);
      final String msg4 = count+" "+synset+"\n  "+synset.getLongDescription();
      final String msg5 = count+" "+synset+"\n  "+synset.getLongDescription(true);
    }
    assertEquals(count, expectedCount);
    System.err.printf("tested %,d descriptions.\n", count);
  }

  @Test
  public void testAntonym() {
    System.err.println("testAntonym");
    // adj up#1 has ANTONYM adj down#1
    final Word upWord = dictionary.lookupWord("up", POS.ADJ);
    assertTrue(upWord.getPointerTypes().contains(PointerType.ANTONYM));
    final WordSense up1 = upWord.getSense(1);
    final Word downWord = dictionary.lookupWord("down", POS.ADJ);
    final WordSense down1 = downWord.getSense(1);
    assertTrue(up1.getTargets(PointerType.ANTONYM).contains(down1));
    assertTrue(down1.getTargets(PointerType.ANTONYM).contains(up1));

    // adj beutifulu#1 has ANTONYM adj ugly#1
    // https://sourceforge.net/tracker/index.php?func=detail&aid=1226746&group_id=33824&atid=409470
    final Word beautifulWord = dictionary.lookupWord("beautiful", POS.ADJ);
    assertTrue(beautifulWord.getPointerTypes().contains(PointerType.ANTONYM));
    final WordSense beautiful1 = beautifulWord.getSense(1);
    final Word uglyWord = dictionary.lookupWord("ugly", POS.ADJ);
    final WordSense ugly1 = uglyWord.getSense(1);
    assertTrue(beautiful1.getTargets(PointerType.ANTONYM).contains(ugly1));
    assertTrue(ugly1.getTargets(PointerType.ANTONYM).contains(beautiful1));
  }

   @Test
  public void testPertainym() {
    System.err.println("testPertainym");
    // adj presidential#1 has PERTAINYM noun president#3
    final Word presidentialWord = dictionary.lookupWord("presidential", POS.ADJ);
    assertTrue(presidentialWord.getPointerTypes().contains(PointerType.PERTAINYM));
    final WordSense presidential1 = presidentialWord.getSense(1);
    final Word presidentWord = dictionary.lookupWord("president", POS.NOUN);
    final WordSense president3 = presidentWord.getSense(3);
    assertTrue(presidential1.getTargets(PointerType.PERTAINYM).contains(president3));
    // https://sourceforge.net/tracker/index.php?func=detail&aid=1372493&group_id=33824&atid=409470
    assertTrue(presidential1.getTargets(PointerType.DERIVED).isEmpty());
  }

  @Test
  public void testDomainTypes() {
    System.err.println("testDomainTypes");
    // adj up#7 member of noun TOPIC computer#1
    final Word word = dictionary.lookupWord("up", POS.ADJ);
    System.err.println("word: "+word+" pointerTypes: "+word.getPointerTypes());
    System.err.println("  "+word.getSense(7).getLongDescription());
    final PointerType[] pointerTypes = new PointerType[] {
      //PointerType.DOMAIN,
      //PointerType.MEMBER_OF_TOPIC_DOMAIN,
      PointerType.DOMAIN_OF_TOPIC,
      //PointerType.MEMBER_OF_THIS_DOMAIN_TOPIC,
    };
    //FIXME assert something here, don't just print
    for (final PointerType pointerType : pointerTypes) {
      for (final PointerTarget target : word.getSense(7).getSynset().getTargets(pointerType)) {
        System.err.println(pointerType + " target: " + target);
      }
    }
  }

  @Test
  public void testAttributeType() {
    System.err.println("testAttributeType");
    // adj low-pitch#1 is attribute of "pitch"#1
    final Word word = dictionary.lookupWord("low-pitched", POS.ADJ);
    System.err.println("word: "+word+" pointerTypes: "+word.getPointerTypes());
    System.err.println("  "+word.getSense(1).getLongDescription());
    final PointerType[] pointerTypes = new PointerType[] { 
      PointerType.ATTRIBUTE, 
    };
    //FIXME assert something here, don't just print
    for (final PointerType pointerType : pointerTypes) {
      for (final PointerTarget target : word.getSense(1).getSynset().getTargets(pointerType)) {
        System.err.println("  "+pointerType+" target: "+target);
      }
    }
  }

  @Test
  public void testVerbFrames() {
    System.err.println("testVerbFrames");
    // verb "complete"#1 _synset_ has 4 generic verb frames
    // 1. Somebody ----s
    // 2. Somebody ----s something
    // 3. Something ----s something
    // 4. Somebody ----s VERB-ing
    // and 1 specific verb frame
    // 1. They won't %s the story
    // https://sourceforge.net/tracker/index.php?func=detail&aid=1749797&group_id=33824&atid=409471
    final Word complete = dictionary.lookupWord("complete", POS.VERB);
    final WordSense complete1 = complete.getSense(1);
    //TODO compare with wnb and what its actually supposed to do
    assertEquals(5, complete1.getVerbFrames().size());
    final Word finish = dictionary.lookupWord("finish", POS.VERB);
    final WordSense finish1 = finish.getSense(1);
  }

  @Test
  public void testInstances() {
    System.err.println("testInstances");
    // noun "George Bush"#1 has 
    final Word georgeBush = dictionary.lookupWord("George Bush", POS.NOUN);
    System.err.println("word: "+georgeBush+" pointerTypes: "+georgeBush.getPointerTypes());
    System.err.println("  "+georgeBush.getSense(1).getLongDescription());
    final PointerType[] pointerTypes = new PointerType[] { 
      //FIXME make HYPONYM a superset of INSTANCE_HYPONYM ?
      PointerType.HYPONYM,
      PointerType.INSTANCE_HYPONYM,
      PointerType.HYPERNYM,
      PointerType.INSTANCE_HYPERNYM,
    };
    for (final PointerType pointerType : pointerTypes) {
      final List<PointerTarget> targets = georgeBush.getSense(1).getSynset().getTargets(pointerType);
      //assertTrue("type: "+pointerType, targets.isEmpty() == false);
      // woah - WordSense targets are different than Synset targets ??
      // at a minimum this needs to be documented
      final List<PointerTarget> targetsAlt = georgeBush.getSense(1).getTargets(pointerType);
//      assertEquals("pointerType: "+pointerType, targets, targetsAlt);
      //assertTrue(targets == targetsAlt);
      for (final PointerTarget target : targets) {
        System.err.println("  " + pointerType + " target: " + target);
      }
    }
  }
}
