package edu.brandeis.cs.steele.wn;

import junit.framework.JUnit4TestAdapter;
import org.junit.*;
import static org.junit.Assert.*;
import java.util.*;

// By far most complex features involve multi-words, esp those containing
// prepositions and "-"
// TODO add tests with prepositions
//
// - test plan
//   - Morphy
//     - make sure caching strategies are not harming correctness (uses DatabaseKey(pos, someString))
//   - specific synsets
//     - using offets will require changes as WN is improved
//     - could use lemmma, pos, and (sense number OR gloss)
//   - other relations including derivationally related
//   - add speed tests
//     - task-based: count unique WordSense's in all DBs
//     - get stems of every lemma in all DBs ("wounds" -> "wound" -> "wind") 
//     - compare speed with various CharStream impls (add some package private methods)
//   - sense numbers
//   - gloss
//   - compare to parsed output of 'wn' binary (optional - @Ignore and/or boolean flag)

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

  private static boolean baseFormContainsUnderScore(final List<String> baseForms) {
    for(final String baseForm : baseForms) {
      if(baseForm.indexOf("_") >= 0) {
        return true;
      }
    }
    return false;
  }

  @Test
  public void testCharSequenceTokenizer() {
    //CharSequenceTokenizer tokens = new CharSequenceTokenizer("1 2 3");
    //CharSequenceTokenizer tokens = new CharSequenceTokenizer(" 1 2 3");
    //CharSequenceTokenizer tokens = new CharSequenceTokenizer(" 1");
    //while(tokens.hasNext()) {
    //  System.err.printf("next: \"%s\"\n", tokens.nextInt());
    //}

    String s = "0";
    assertEquals(0, new CharSequenceTokenizer(s).nextInt());
    s = " 0";
    assertEquals(0, new CharSequenceTokenizer(s).nextInt());
    s = "1";
    assertEquals(1, new CharSequenceTokenizer(s).nextInt());
    s = " 1";
    assertEquals(1, new CharSequenceTokenizer(s).nextInt());
    s = " 1 ";
    assertEquals(1, new CharSequenceTokenizer(s).nextInt());
    s = "-1";
    assertEquals(-1, new CharSequenceTokenizer(s).nextInt());
    s = " -1";
    assertEquals(-1, new CharSequenceTokenizer(s).nextInt());
    //System.err.println("testCharSequenceTokenizer passed");
  }

  @Test
  public void test1() {
    String[][] unstemmedStemmedCases = new String[][] {
      { POS.NOUN.name(), "dogs", "dog" },
      { POS.NOUN.name(), "geese", "goose", "geese" },
      { POS.NOUN.name(), "handfuls", "handful" },
      { POS.NOUN.name(), "villas", "villa" }, // exposed true case bug
      { POS.NOUN.name(), "Villa", "Villa" }, // exposed true case bug
      { POS.NOUN.name(), "br", "Br", "BR" }, // exposed true case bug
      { POS.NOUN.name(), "heiresses", "heiress" }, 
        //WN missing derivationally relation to neuter form "heir" - intentional?
      { POS.NOUN.name(), "heiress", "heiress" },
      { POS.NOUN.name(), "George W. \t\tBush", "George W. Bush" }, //WN doesn't get this (extra internal space) (WN online does - probably string preprocessing)
      { POS.NOUN.name(), "george w. bush", "George W. Bush" },
      //WN doesn't get this either (missing ".") { POS.NOUN.name(), "george w bush", "George W. Bush" },
      { POS.NOUN.name(), "mice", "mouse", "mice" },
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
      { POS.NOUN.name(), "businessmen", "businessmen", "businessman" },
      { POS.NOUN.name(), "_", null },
      { POS.NOUN.name(), "armful", "armful" },
      { POS.NOUN.name(), "attorneys general", "attorney general" },
      { POS.NOUN.name(), "axes", "ax", "axis", "Axis" }, // NOTE: noun "axe" is only derivationally related to "ax"
      { POS.NOUN.name(), "boxesful", "boxful" },
      //{ POS.NOUN.name(), "bachelor of art", "Bachelor of Arts" }, //currently fails - known morpphy algorihm bug (http://wordnet.princeton.edu/man/morphy.7WN.html#toc8)
      { POS.NOUN.name(), "Bachelor of Sciences in Engineering", "Bachelor of Science in Engineering" }, 
      { POS.NOUN.name(), "lines of business", "line of business" },
      { POS.NOUN.name(), "SS", "SS" },
      { POS.NOUN.name(), "mamma's boy", "mamma's boy" },
      { POS.NOUN.name(), "15_minutes", "15 minutes" },
      { POS.NOUN.name(), "talks", "talk", "talks" },
      { POS.NOUN.name(), "talk", "talk" }, // note asymmetric property: "talk" -> {"talk"}, "talks" -> {"talk", "talks"}
      { POS.NOUN.name(), "wounded", "wounded" },
      { POS.NOUN.name(), "wounding", "wounding" },
      { POS.VERB.name(), "wounded", "wound" },
      { POS.VERB.name(), "wound", "wind" },
      { POS.ADJ.name(), "wounded", "wounded" },
      { POS.VERB.name(), "dogs", "dog" },
      { POS.VERB.name(), "abided by", "abide by" },
      { POS.VERB.name(), "gave a damn", "give a damn" },
      { POS.VERB.name(), "asking for it", "ask for it" },
      { POS.VERB.name(), "asked for it", "ask for it" },
      { POS.VERB.name(), "accounting for", "account for" },
      { POS.VERB.name(), "was", "be", "was" },
      { POS.VERB.name(), "cannonball along", "cannonball along" },
      //{ POS.VERB.name(), "cannonballing along", "cannonball along" }, //XXX currently fails wnb too
      //{ POS.VERB.name(), "finesses", "finesse" }, //not in WordNet 3.0 as a Verb
      { POS.VERB.name(), "accesses", "access" },
      { POS.VERB.name(), "went", "go", "went" },
      { POS.VERB.name(), "bloging" /* spelled wrong */, "blog" },
      //{ POS.VERB.name(), "blogging" /* spelled correctly, not in exceptions file */, "blog" },
      { POS.VERB.name(), "shook hands", "shake hands", "shook hands" },
      { POS.VERB.name(), "Americanize", "Americanize" }, // capitalized verb - grep "v [0-9]+ [A-Z]" data.verb
      { POS.VERB.name(), "saw", "see", "saw" },
      { POS.ADJ.name(), "onliner" /* no idea */, "online" },
      // should both variants be returned ? { POS.ADJ.name(), "onliner" /* no idea */, "on-line" },
      { POS.ADJ.name(), "redder" /* no idea */, "red" },
      { POS.ADJ.name(), "Middle Eastern", "Middle Eastern" }, // capitalized adj - grep "a [0-9]+ [A-Z]" data.adj
      { POS.ADJ.name(), "Latin-American", "Latin-American" }, // capitalized adj - grep "a [0-9]+ [A-Z]" data.adj
      { POS.ADJ.name(), "low-pitched", "low-pitched" },
      //{ POS.ADJ.name(), "low-pitch", "low-pitched" }, // wnb misses
    };
    for(final String[] testElements : unstemmedStemmedCases) {
      final POS pos = POS.valueOf(testElements[0]);
      final String unstemmed = testElements[1];
      final String stemmed = testElements[2];
      final List<String> goldStems = new ArrayList<String>();
      for(int i = 2; i < testElements.length; ++i) {
        goldStems.add(testElements[i]);
      }
      final List<String> baseForms = stem(unstemmed, pos);
      String msg = "unstemmed: \""+unstemmed+"\" "+pos+" gold: \""+stemmed+"\" output: "+baseForms;
      assertTrue(msg, baseForms.contains(stemmed) || (stemmed == null && baseForms.isEmpty()));
      //System.err.println(msg);
      assertFalse("baseForms: "+baseForms, baseFormContainsUnderScore(baseForms));
      //TODO on failure, could try other POS
      if(baseForms.size() >= 2) {
        //TODO tighten up this test - don't allow any extra unspecified variants
        // note this considers case variants distinct
        System.err.println("extra variants for \""+unstemmed+"\": "+baseForms+" goldStems: "+goldStems);
      }
      assertTrue(isUnique(baseForms));
    }
  }

  @Test
  public void testMorphyUtils() {
    // odd empty string is considered 1 word (not 0)
    assertEquals(1, Morphy.countWords("", ' '));
    assertEquals(1, Morphy.countWords("dog", ' '));
    // odd that countWords uses passed in separator AND ' ' and '_'
    assertEquals(2, Morphy.countWords("dog_gone", ' '));
    assertEquals(1, Morphy.countWords("dog-gone", ' '));
    assertEquals(2, Morphy.countWords("dog-gone", '-'));
  }

  @Test
  public void testWordSense() {
    assertEquals(42, dictionary.lookupWord(POS.NOUN, "dog").getSenses()[0].getSensesTaggedFrequency());
    assertEquals(2, dictionary.lookupWord(POS.VERB, "dog").getSenses()[0].getSensesTaggedFrequency());
    assertEquals(3, dictionary.lookupWord(POS.ADJ, "cardinal").getSenses()[0].getSensesTaggedFrequency());
    assertEquals(0, dictionary.lookupWord(POS.ADJ, "cardinal").getSenses()[1].getSensesTaggedFrequency());
    assertEquals(9, dictionary.lookupWord(POS.ADJ, "concrete").getSenses()[0].getSensesTaggedFrequency());
    assertEquals(1, dictionary.lookupWord(POS.ADJ, "dogmatic").getSenses()[0].getSensesTaggedFrequency());
    System.err.println("testWordSense() passed");
  }

  @Test
  public void detectLostVariants() {
    int issues = 0;
    int nonCaseIssues = 0;
    for(final POS pos : POS.CATS) {
      for(final Word word : dictionary.words(pos)) {
        for(final WordSense wordSense : word.getSenses()) {
          final String lemma = wordSense.getLemma();
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
    IterationTest.printMemoryUsage();
  }

  private static boolean isUnique(final List<String> items) {
    return items.size() == new HashSet<String>(items).size();
  }

  //won't find anything without getindex() functionality?
  //@Test
  //public void findLexicalAmbiguity() {
  //  final DictionaryDatabase dictionary = FileBackedDictionary.getInstance();
  //  int issues = 0;
  //  int nonCaseIssues = 0;
  //  for(final POS pos : POS.CATS) {
  //    for(final Word word : dictionary.words(pos)) {
  //      for(final WordSense wordSense : word.getSenses()) {
  //        final String lemma = wordSense.getLemma();
  //        for(final POS otherPOS : POS.CATS) {
  //          if(otherPOS == pos) {
  //            continue;
  //          }
  //          // TODO implement getindex() and then activate this test
  //          // search for this lemma in other POS
  //          // see if we can find lexical ambiguity
  //          // e.g. NOUN("long time")
  //          // ADJ("longtime)
  //        }
  //        //XXX final List<String> restems = stem(lemma, pos);
  //        //XXX String msg = "ok";
  //        //XXX if(false == restems.contains(lemma)) {
  //        //XXX   msg = "restems: "+restems+" doesn't contain lemma: "+lemma;
  //        //XXX   ++issues;
  //        //XXX   boolean nonCaseIssue = false == containsIgnoreCase(lemma, restems);
  //        //XXX   if(nonCaseIssue) {
  //        //XXX     ++nonCaseIssues;
  //        //XXX   }
  //        //XXX   System.err.println(
  //        //XXX       "issues: "+issues+" nonCases: "+nonCaseIssues+
  //        //XXX       (nonCaseIssue ? "*" : " ")+
  //        //XXX       " "+msg);
  //        //XXX }
  //        //XXX if(restems.size() > 1) {
  //        //XXX   //System.err.println(pos+" lemma: "+lemma+" restems: "+restems);
  //        //XXX }
  //        //XXX assertTrue(msg, restems.contains(lemma));
  //        //XXX // note this considers case variants distinct
  //        //XXX assertTrue(isUnique(restems));
  //      }
  //    }
  //  }
  //}

  public static junit.framework.Test suite() {
    return new JUnit4TestAdapter(MorphyTest.class);
  }
}
