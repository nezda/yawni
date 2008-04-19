package edu.brandeis.cs.steele.wn;

import junit.framework.JUnit4TestAdapter;
import org.junit.*;
import static org.junit.Assert.*;
import java.util.*;

public class SynsetTest {
  private static DictionaryDatabase dictionary;
  @BeforeClass
  public static void init() {
    dictionary = FileBackedDictionary.getInstance();
  }

  @Test
  public void test1() {
    for(final POS pos : POS.CATS) {
      int count = 0;
      for(final Synset synset : dictionary.synsets(pos)) {
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
    final Word word = dictionary.lookupWord(POS.ADJ, "up");
    System.err.println("word: "+word+" pointerTypes: "+word.getPointerTypes());
    System.err.println("  "+word.getSense(7).getLongDescription());
    //assertTrue(
    //PointerType pointerType = PointerType.DOMAIN;
    final PointerType[] pointerTypes = new PointerType[]{ 
      /*PointerType.DOMAIN, PointerType.MEMBER_OF_TOPIC_DOMAIN,*/ PointerType.DOMAIN_OF_TOPIC, //PointerType.MEMBER_OF_THIS_DOMAIN_TOPIC
    };
    for(final PointerType pointerType : pointerTypes) {
      for(final PointerTarget target : word.getSense(7).getSynset().getTargets(pointerType)) {
        System.err.println(pointerType+" target: "+target);
      }
    }
  }

  @Test
  public void test3() {
    // adj low-pitch#1 is attribute of "pitch"
    final Word word = dictionary.lookupWord(POS.ADJ, "low-pitched");
    System.err.println("word: "+word+" pointerTypes: "+word.getPointerTypes());
    System.err.println("  "+word.getSense(1).getLongDescription());
    //assertTrue(
    //PointerType pointerType = PointerType.DOMAIN;
    final PointerType[] pointerTypes = new PointerType[] { 
      PointerType.ATTRIBUTE, 
    };
    for(final PointerType pointerType : pointerTypes) {
      for(final PointerTarget target : word.getSense(1).getSynset().getTargets(pointerType)) {
        System.err.println("  "+pointerType+" target: "+target);
      }
    }
  }

  @Test
  public void testInstances() {
    // adj low-pitch#1 is attribute of "pitch"
    final Word word = dictionary.lookupWord(POS.NOUN, "George Bush");
    System.err.println("word: "+word+" pointerTypes: "+word.getPointerTypes());
    System.err.println("  "+word.getSense(1).getLongDescription());
    final PointerType[] pointerTypes = new PointerType[] { 
      PointerType.HYPONYM, 
      PointerType.INSTANCE_HYPONYM,
      PointerType.HYPERNYM, 
      PointerType.INSTANCE_HYPERNYM,
    };
    for(final PointerType pointerType : pointerTypes) {
      for(final PointerTarget target : word.getSense(1).getSynset().getTargets(pointerType)) {
        System.err.println("  "+pointerType+" target: "+target);
      }
    }
  }

  public static junit.framework.Test suite() {
    return new JUnit4TestAdapter(SynsetTest.class);
  }
}
