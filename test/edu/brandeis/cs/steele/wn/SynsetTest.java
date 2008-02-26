package edu.brandeis.cs.steele.wn;

import junit.framework.JUnit4TestAdapter;
import org.junit.*;
import static org.junit.Assert.*;
import java.util.*;

public class SynsetTest {
  @Test
  public void test1() {
    final DictionaryDatabase dictionary = FileBackedDictionary.getInstance();
    for(final POS pos : POS.CATS) {
      int count = 0;
      for(final Synset synset : dictionary.synsets(pos)) {
        if(++count > 10) break;
        // exercise toString() and getGloss()
        final String msg = count+" "+synset+"\n  "+synset.getGloss();
        //System.err.println(msg);
      }
    }
  }

  public static junit.framework.Test suite() {
    return new JUnit4TestAdapter(SynsetTest.class);
  }
}
