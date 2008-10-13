package edu.brandeis.cs.steele.wn;

import junit.framework.JUnit4TestAdapter;
import org.junit.*;
import static org.junit.Assert.*;

import java.util.*;


// main things to test for:
// - produces all combinations
// - produces no duplicates

@Ignore // feature not working
public class GetIndexTest {
  private static DictionaryDatabase dictionary;
  private static Morphy morphy;
  @BeforeClass
  public static void init() {
    dictionary = FileBackedDictionary.getInstance();
    morphy = ((FileBackedDictionary)dictionary).morphy;
  }
  @Test
  public void test2() {
    assertEquals(2, GetIndex.alternate("a_b"));
  }
  @Test
  public void test3() {
    assertEquals(4, GetIndex.alternate("x_y_z"));
  }
  //@Test
  //public void test4() {
  //  assertEquals(16, GetIndex.alternate("p_q_r_s"));
  //}
  @Test
  public void testExplore() {
    //XXX NOTE THE UNDERSCORES
    //System.err.println("GetIndexTest test1() "+morphy);
    //new GetIndex("internal-combustion_engine", POS.NOUN, morphy);
    //GetIndex.alternate("internal-combustion_engine");
    //GetIndex.alternate("I_ran");
    //GetIndex.alternate("be_an");
    
    //GetIndex.alternate("m_n_o_p");
  }
}
