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
   * - String[] lookupBaseForms(POS pos, String someString)
   * - Synset[] lookupSynsets(POS pos, String someString)
   * - Iterator<Pointer> pointers(POS pos)
   * - Iterator<Pointer> pointers(POS pos, PointerType pointerType)
   * - Iterator<Word> words(POS pos)
   * - Iterator<Synset> synsets(POS pos)
   * - Iterator<WordSense> wordSenses(POS pos)
   */
  @Ignore
  @Test
  public void test() {
    final String[] tanks = dictionary.lookupBaseForms(POS.ALL, "tank");
    System.err.println("tanks: "+Arrays.toString(tanks));
    final String[] gooses = dictionary.lookupBaseForms(POS.ALL, "goose");
    System.err.println("gooses: "+Arrays.toString(gooses));
  }

  @Test(expected=IllegalArgumentException.class)
  public void test_lookupWord() {
    dictionary.lookupWord(POS.ALL, "tank");
  }
}
