package edu.brandeis.cs.steele.wn;

import junit.framework.JUnit4TestAdapter;
import org.junit.*;
import static org.junit.Assert.*;
import java.util.*;

/**
 * 
 */
public class DictionaryDatabaseTest {
  private static DictionaryDatabase dictionary;
  @BeforeClass
  public static void init() {
    dictionary = FileBackedDictionary.getInstance();
  }

  /** 
   * test POS.ALL support
   * + String[] lookupBaseForms(POS pos, String someString)
   * - Synset[] lookupSynsets(POS pos, String someString)
   * - Iterator<Pointer> pointers(POS pos)
   * - Iterator<Pointer> pointers(POS pos, PointerType pointerType)
   * - Iterator<Word> words(POS pos)
   * - Iterator<Synset> synsets(POS pos)
   * - Iterator<WordSense> wordSenses(POS pos)
   */
  //@Ignore
  @Test
  public void test() {
    final String[] tanks = dictionary.lookupBaseForms("tank", POS.ALL);
    System.err.println("tanks: "+Arrays.toString(tanks));
    final String[] geese = dictionary.lookupBaseForms("geese", POS.ALL);
    System.err.println("geese: "+Arrays.toString(geese));
  }

  /**
   * Expects exception because POS.ALL does not make sense for method 
   * lookupWord(POS, lemma) which returns at most 1 result.
   * TODO subclass IllegalArgumentException (a RuntimeException) 
   * indicating this.
   */
  @Test(expected=IllegalArgumentException.class)
  public void test_lookupWord() {
    dictionary.lookupWord("tank", POS.ALL);
  }

  /**
   * Look for warning issues with lookupSynsets()
   */
  @Test
  public void lookupSynsetsTest() {
    //String str = "allow for";
    //String str = "allowing for";
    String str = "allows for";
    POS pos = POS.VERB;

    // exhaustive -- all POS
    //for(POS pos : POS.CATS) {
    //  Synset[] syns = dictionary.lookupSynsets(pos, str);
    //}
    // just our source POS
    Synset[] syns = dictionary.lookupSynsets(str, pos);
    if (syns.length == 0) {
      System.err.println("XXX PROBLEM: "+str+" no syns found (loopback failure)");
    }
    //System.err.println(str+": "+Arrays.toString(syns));
  }
}
