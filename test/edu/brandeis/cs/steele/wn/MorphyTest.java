package edu.brandeis.cs.steele.wn;

import junit.framework.JUnit4TestAdapter;
import org.junit.*;
import static org.junit.Assert.*;
import java.util.*;

// By far most complex features involve multi-words, esp those containing
// prepositions and "-"
// TODO add tests with prepositions
public class MorphyTest {
  //TODO consider proper Parameterized tests

  private static DictionaryDatabase dictionary;
  @BeforeClass
  public static void init() {
    dictionary = FileBackedDictionary.getInstance();
  }

  private static List<String> stem(final String someString, final POS pos) {
    return Arrays.asList(dictionary.lookupBaseForms(pos, someString));
  }

  private static boolean containsIgnoreCase(final String needle, final List<String> haystack) {
    for(final String item : haystack) {
      if(item.equalsIgnoreCase(needle)) {
        return true;
      }
    }
    return false;
  }

  @Test
  public void test1() {
    String[][] unstemmedStemmedCases = new String[][] {
      { POS.NOUN.name(), "dogs", "dog" },
      { POS.NOUN.name(), "geese", "goose" },
      { POS.NOUN.name(), "handfuls", "handful" },
      { POS.NOUN.name(), "villas", "villa" }, // exposed true case bug
      { POS.NOUN.name(), "heiresses", "heiress" }, 
        //WN missing derivationally relation to neuter form "heir" - intentional?
      { POS.NOUN.name(), "heiress", "heiress" },
      { POS.NOUN.name(), "George W. \t\tBush", "George W. Bush" }, //WN doesn't get this (extra internal space) (WN online does - probably string preprocessing)
      { POS.NOUN.name(), "george w. bush", "George W. Bush" },
      //WN doesn't get this either (missing ".") { POS.NOUN.name(), "george w bush", "George W. Bush" },
      { POS.NOUN.name(), "mice", "mouse" },
      { POS.NOUN.name(), "internal-combustion engine", "internal-combustion engine" },
      //WN 3 doesn't get this? { POS.NOUN.name(), "internal combustion engine", "internal-combustion engine" },
      { POS.NOUN.name(), "hangers-on", "hanger-on" },
      // needs " " -> "-" { POS.NOUN.name(), "hangers on", "hanger-on" },
      { POS.NOUN.name(), "letter bombs", "letter bomb" },
      // needs "-" -> "" { POS.NOUN.name(), "fire-bomb", "firebomb" },
      // needs "-" -> " " { POS.NOUN.name(), "letter-bomb", "letter bomb" },
      { POS.NOUN.name(), "I ran", null }, // WN gets this as "Iran" - " " -> "" seems bad unless a variant has "-" in same position (WN online doesn't do this)
      { POS.NOUN.name(), "be an", null }, // WN gets this as "bean" (WN online doesn't do this)
      { POS.NOUN.name(), "are a", null }, // WN gets this as "area" (WN online doesn't do this)
      { POS.NOUN.name(), "_slovaks_", "Slovak" },
      { POS.NOUN.name(), "superheroes", "superhero" }, // NOTE: this isn't in WordNet (Brett Spell noted this)
      { POS.NOUN.name(), "_", null },
      { POS.NOUN.name(), "armful", "armful" },
      { POS.NOUN.name(), "attorneys general", "attorney general" },
      { POS.NOUN.name(), "axes", "ax", "axis", "Axis" }, //XXX currently fails! NOTE: noun "axe" is only derivationally related to "ax"
      { POS.NOUN.name(), "boxesful", "boxful" },
      //{ POS.NOUN.name(), "bachelor of art", "Bachelor of Arts" }, //currently fails - known morpphy algorihm bug (http://wordnet.princeton.edu/man/morphy.7WN.html#toc8)
      { POS.NOUN.name(), "Bachelor of Sciences in Engineering", "Bachelor of Science in Engineering" }, //currently fails - known morpphy algorihm bug (http://wordnet.princeton.edu/man/morphy.7WN.html#toc8)
      { POS.NOUN.name(), "lines of business", "line of business" },
      { POS.NOUN.name(), "SS", "SS" },
      { POS.VERB.name(), "dogs", "dog" },
      { POS.VERB.name(), "abided by", "abide by" },
      { POS.VERB.name(), "gave a damn", "give a damn" },
      { POS.VERB.name(), "asking for it", "ask for it" },
      { POS.VERB.name(), "accounting for", "account for" },
      { POS.VERB.name(), "was", "be" },
      //{ POS.VERB.name(), "finesses", "finess" }, not in WordNet 3.0
      { POS.VERB.name(), "accesses", "access" },
      { POS.ADJ.name(), "onliner" /* no idea */, "online" },
      // should both variants be returned ? { POS.ADJ.name(), "onliner" /* no idea */, "on-line" },
      { POS.ADJ.name(), "redder" /* no idea */, "red" },
    };
    for(final String[] unstemmedStemmed : unstemmedStemmedCases) {
      final POS pos = POS.valueOf(unstemmedStemmed[0]);
      final String unstemmed = unstemmedStemmed[1];
      final String stemmed = unstemmedStemmed[2];
      final List<String> baseForms = stem(unstemmed, pos);
      assertTrue("unstemmed: \""+unstemmed+"\" "+pos+" gold: \""+stemmed+"\" output: "+baseForms,
          baseForms.contains(stemmed) || (stemmed == null && baseForms.isEmpty()));
      if(baseForms.size() > 2) {
        //TODO tighten up this test - don't allow any extra unspecified variants
        // note this considers case variants distinct
        System.err.println("extra variants for \""+unstemmed+"\": "+baseForms);
      }
      assertTrue(isUnique(baseForms));
    }
  }

  @Test
  public void testMorphyUtils() {
    // odd empty string is considered a word
    assertEquals(1, Morphy.countWords("", ' '));
    assertEquals(1, Morphy.countWords("dog", ' '));
    // odd that countWords uses passed in separator AND ' ' and '_'
    assertEquals(2, Morphy.countWords("dog_gone", ' '));
    assertEquals(1, Morphy.countWords("dog-gone", ' '));
    assertEquals(2, Morphy.countWords("dog-gone", '-'));
  }

  @Test
  public void detectLostVariants() {
    final DictionaryDatabase dictionary = FileBackedDictionary.getInstance();
    int issues = 0;
    int nonCaseIssues = 0;
    for(final POS pos : POS.CATS) {
      for(final IndexWord indexWord : dictionary.indexWords(pos)) {
        for(final Word word : indexWord.getSenses()) {
          final String lemma = word.getLemma();
          final List<String> restems = stem(lemma, pos);
          String msg = "ok";
          if(false == restems.contains(lemma)) {
            msg = "restems: "+restems+" doesn't contain lemma: "+lemma;
            ++issues;
            boolean nonCaseIssue = false == containsIgnoreCase(lemma, restems);
            if(nonCaseIssue) {
              ++nonCaseIssues;
            }
            System.err.println(
                "issues: "+issues+" nonCases: "+nonCaseIssues+
                (nonCaseIssue ? "*" : " ")+
                " "+msg);
          }
          if(restems.size() > 1) {
            //System.err.println(pos+" lemma: "+lemma+" restems: "+restems);
          }
          assertTrue(msg, restems.contains(lemma));
          // note this considers case variants distinct
          assertTrue(isUnique(restems));
        }
      }
    }
  }

  private static boolean isUnique(final List<String> items) {
    return items.size() == new HashSet<String>(items).size();
  }

  // TODO
  // - test plan
  //   - Morphy
  //     - make sure caching strategies are not harming correctness (uses DatabaseKey(pos, someString))
  //   - specific synsets
  //     - using offets will require changes as WN is improved
  //     - could use lemmma, pos, and (sense number OR gloss)
  //   - other relations including derivationally related
  //   - add speed tests
  //     - task-based: count unique Word's in all DBs
  //     - get stems of every lemma in all DBs ("wounds" -> "wound" -> "wind") 
  //     - compare speed with various CharStream impls (add some package private methods)
  //   - sense numbers
  //   - gloss
  //   - compare to parsed output of 'wn' binary (optional - @Ignore and/or boolean flag)

  public static junit.framework.Test suite() {
    return new JUnit4TestAdapter(MorphyTest.class);
  }
}
