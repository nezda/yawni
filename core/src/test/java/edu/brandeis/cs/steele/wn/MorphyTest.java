package edu.brandeis.cs.steele.wn;

import junit.framework.JUnit4TestAdapter;
import org.junit.*;
import static org.junit.Assert.*;
import java.util.*;

/** 
 * By far most complex features involve multi-words, esp those containing
 * prepositions and "-".
 *
 * - Test Plan
 *   - Morphy
 *     - make sure caching strategies are not harming correctness (uses DatabaseKey(pos, someString))
 *   - specific synsets
 *     - using offets will require changes as WN is improved
 *     - could use lemmma, pos, and (sense number OR gloss)
 *   - other relations including derivationally related
 *   - add speed tests
 *     - task-based: count unique WordSense's in all DBs
 *     - get stems of every lemma in all DBs ("wounds" -> "wound" -> "wind") 
 *     - compare speed with various CharStream impls (add some package private methods)
 *   - sense numbers
 *   - gloss
 *   - compare to parsed output of 'wn' binary (optional - @Ignore and/or boolean flag)
 * 
 * TODO add tests with prepositions
 * TODO consider proper Parameterized tests
 */
public class MorphyTest {
  private static DictionaryDatabase dictionary;
  @BeforeClass
  public static void init() {
    dictionary = FileBackedDictionary.getInstance();
  }

  private static List<String> stem(final String someString, final POS pos) {
    return Arrays.asList(dictionary.lookupBaseForms(someString, pos));
  }

  //TODO consider moving to Utils
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
  public void testMorphyUtils() {
    assertEquals(1, Morphy.countWords("dog", ' '));
    // odd that countWords uses passed in separator AND ' ' and '_'
    assertEquals(2, Morphy.countWords("dog_gone", ' '));
    assertEquals(2, Morphy.countWords("dog _ gone", ' '));
    assertEquals(2, Morphy.countWords("dog__gone", ' '));
    assertEquals(2, Morphy.countWords("dog_ gone", ' '));
    assertEquals(2, Morphy.countWords("_dog_gone_", ' '));
    assertEquals(1, Morphy.countWords("dog-gone", ' '));
    assertEquals(2, Morphy.countWords("dog-gone", '-'));
    assertEquals(3, Morphy.countWords("internal-combustion engine", '-'));
    assertEquals(2, Morphy.countWords("internal-combustion engine", '_'));
    assertEquals(3, Morphy.countWords("a-b-c", '-'));
    assertEquals(3, Morphy.countWords("a-b-c-", '-'));
    assertEquals(3, Morphy.countWords("-a-b-c", '-'));
    
    // odd empty string is considered 1 word (not 0)
    assertEquals(0, Morphy.countWords("", ' '));
    assertEquals(0, Morphy.countWords(" ", ' '));
    assertEquals(1, Morphy.countWords("-", ' '));
    assertEquals(1, Morphy.countWords("--", ' '));
    assertEquals(0, Morphy.countWords("__", ' '));
    assertEquals(0, Morphy.countWords("  ", ' '));
    assertEquals(1, Morphy.countWords("- ", ' '));
  }

  @Test
  public void test1() {
    String[][] unstemmedStemmedCases = new String[][] {
      // { POS, <unstemmed>, <stemmed> }
      { POS.NOUN.name(), "dogs", "dog" },
      { POS.NOUN.name(), "geese", "goose", "geese" },
      { POS.NOUN.name(), "handfuls", "handful" },
      { POS.NOUN.name(), "villas", "villa" }, // exposed true case bug
      { POS.NOUN.name(), "Villa", "Villa" }, // exposed true case bug
      { POS.NOUN.name(), "br", "Br", "BR" }, // exposed true case bug
      { POS.NOUN.name(), "heiresses", "heiress" }, 
        //WN missing derivationally relation to neuter form "heir" - intentional?
      { POS.NOUN.name(), "heiress", "heiress" },
      { POS.NOUN.name(), "George W. \t\tBush", "George W. Bush" }, // WN doesn't get this (extra internal space) (WN online does - probably better input string preprocessing)
      { POS.NOUN.name(), "george w. bush", "George W. Bush" },
      //{ POS.NOUN.name(), "f.d. roosevelt", "F. D. Roosevelt" }, // WN doesn't get this
      //{ POS.NOUN.name(), "u.s", "u.s."}, // WN gets this, though probably via "US"
      //WN doesn't get this either (missing ".") { POS.NOUN.name(), "george w bush", "George W. Bush" },
      { POS.NOUN.name(), "mice", "mouse", "mice" },
      { POS.NOUN.name(), "internal-combustion engine", "internal-combustion engine" },
      //WN 3 doesn't get this? { POS.NOUN.name(), "internal combustion engine", "internal-combustion engine" },
      { POS.NOUN.name(), "hangers-on", "hanger-on" },
      // needs " " -> "-" { POS.NOUN.name(), "hangers on", "hanger-on" },
      { POS.NOUN.name(), "letter bombs", "letter bomb" },
      // needs "-" -> "" { POS.NOUN.name(), "fire-bomb", "firebomb" },
      // needs "-" -> " " { POS.NOUN.name(), "letter-bomb", "letter bomb" },
      // harder one: 
      // - needs to either not require getIndexedLinePointer() to operate on words OR 
      //   + return the nearest hit (maybe negated to indicate no normal match)
      // - could be really fast with a suffix index (reverse words)
      // needs "" -> " " { POS.NOUN.name(), "letterbomb", "letter bomb" }, // WN doesn't get this
      //{ POS.NOUN.name(), "hyper-active", "hyperactive" }, // WN doesn't get this
      { POS.NOUN.name(), "I ran", null }, // WN gets this as "Iran" - " " -> "" seems bad unless a variant has "-" in same position (WN online doesn't do this)
      { POS.NOUN.name(), "be an", null }, // WN gets this as "bean" (WN online doesn't do this)
      { POS.NOUN.name(), "are a", null }, // WN gets this as "area" (WN online doesn't do this)
      { POS.NOUN.name(), "_slovaks_", "Slovak" },
      { POS.NOUN.name(), "superheroes", "superhero" }, // NOTE: this isn't in WordNet (Brett Spell noted this)
      { POS.NOUN.name(), "businessmen", "businessmen", "businessman" },
      { POS.NOUN.name(), "_", null },
      { POS.NOUN.name(), "\n", null },
      { POS.NOUN.name(), "\ndog", null },
      { POS.NOUN.name(), "dog\n", null },
      { POS.NOUN.name(), "\n''", null },
      { POS.NOUN.name(), "''\n", null },
      { POS.NOUN.name(), "-", null },
      { POS.NOUN.name(), "--", null },
      { POS.NOUN.name(), "__", null },
      { POS.NOUN.name(), "  ", null },
      { POS.NOUN.name(), " ", null },
      { POS.NOUN.name(), "-_", null },
      { POS.NOUN.name(), "_-", null },
      { POS.NOUN.name(), " -", null },
      { POS.NOUN.name(), "- ", null },
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
      { POS.NOUN.name(), "was", "WA" }, // weird- de-pluralizing Washington state abbr
      { POS.VERB.name(), "cannonball along", "cannonball along" },
      //{ POS.VERB.name(), "cannonballing along", "cannonball along" }, // WN doesn't get this
      //{ POS.VERB.name(), "finesses", "finesse" }, //not in WordNet 3.0 as a Verb
      { POS.VERB.name(), "accesses", "access" },
      { POS.VERB.name(), "went", "go", "went" },
      { POS.VERB.name(), "bloging" /* spelled wrong */, "blog" },
      //{ POS.VERB.name(), "blogging" /* spelled _correctly_, not in exceptions file */, "blog" },
      { POS.VERB.name(), "shook hands", "shake hands", "shook hands" },
      { POS.VERB.name(), "Americanize", "Americanize" }, // capitalized verb - grep "v [0-9]+ [A-Z]" data.verb
      { POS.VERB.name(), "saw", "see", "saw" },
      { POS.ADJ.name(), "onliner" /* no idea */, "online" },
      // should both variants be returned ? { POS.ADJ.name(), "onliner" /* no idea */, "on-line" },
      { POS.ADJ.name(), "redder" /* no idea */, "red" },
      { POS.ADJ.name(), "Middle Eastern", "Middle Eastern" }, // capitalized adj - grep "a [0-9]+ [A-Z]" data.adj
      { POS.ADJ.name(), "Latin-American", "Latin-American" }, // capitalized adj - grep "a [0-9]+ [A-Z]" data.adj
      { POS.ADJ.name(), "low-pitched", "low-pitched" },
      //{ POS.ADJ.name(), "low-pitch", "low-pitched" }, // WN doesn't get this
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

      final List<String> upperBaseForms = stem(unstemmed.toUpperCase(), pos);
      msg = "UPPER unstemmed: \""+unstemmed+"\" "+pos+" gold: \""+stemmed+"\" output: "+upperBaseForms;
      assertTrue(msg, upperBaseForms.contains(stemmed) || (stemmed == null && upperBaseForms.isEmpty()));
    }
  }

  @Test
  public void testLookupWord() {
    assertEquals(null, dictionary.lookupWord("", POS.NOUN));
    assertTrue(null != dictionary.lookupWord("dog", POS.NOUN));
    assertTrue(null != dictionary.lookupWord("DOG", POS.NOUN));
    assertTrue(null != dictionary.lookupWord("ad blitz", POS.NOUN));
    assertTrue(null != dictionary.lookupWord("ad_blitz", POS.NOUN));
    assertTrue(null != dictionary.lookupWord("AD BLITZ", POS.NOUN));
    assertTrue(null != dictionary.lookupWord("wild-goose chase", POS.NOUN));
    assertTrue(null != dictionary.lookupWord("wild-goose_chase", POS.NOUN));
  }

  // could add explicit checks for this in API methods but that's pretty tedious
  @Test(expected=NullPointerException.class)
  public void testNullLookupWord() {
    assertEquals(null, dictionary.lookupWord(null, POS.NOUN));
  }

  @Test
  public void testWordSense() {
    assertEquals(42, dictionary.lookupWord("dog", POS.NOUN).getSenses()[0].getSensesTaggedFrequency());
    assertEquals(2, dictionary.lookupWord("dog", POS.VERB).getSenses()[0].getSensesTaggedFrequency());
    assertEquals(3, dictionary.lookupWord("cardinal", POS.ADJ).getSenses()[0].getSensesTaggedFrequency());
    assertEquals(0, dictionary.lookupWord("cardinal", POS.ADJ).getSenses()[1].getSensesTaggedFrequency());
    assertEquals(9, dictionary.lookupWord("concrete", POS.ADJ).getSenses()[0].getSensesTaggedFrequency());
    assertEquals(1, dictionary.lookupWord("dogmatic", POS.ADJ).getSenses()[0].getSensesTaggedFrequency());
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

  //TODO consider moving to Utils
  private static <T> boolean isUnique(final List<T> items) {
    return items.size() == new HashSet<T>(items).size();
  }

  @Test
  public void findCollocationAmbiguity() {
    final DictionaryDatabase dictionary = FileBackedDictionary.getInstance();
    int spaceAndDash = 0;
    for(final POS pos : POS.CATS) {
      for(final Word word : dictionary.words(pos)) {
        final String lemma = word.getLemma();
        if(lemma.indexOf("-") > 0 && lemma.indexOf(" ") > 0) {
          spaceAndDash++;
          //System.err.println("lemma: "+lemma+" spaceAndDash: "+spaceAndDash);
        }
      }
    }
    
    int dash = 0;
    int space = 0;
    int dashNoDash = 0;
    int dashSpace = 0;
    int dashNotSpace = 0;
    for (final POS pos : POS.CATS) {
      for (final Word word : dictionary.words(pos)) {
        final String lemma = word.getLemma();
        if (lemma.indexOf("-") > 0) {
          dash++;
          final String noDash = lemma.replace("-", "");
          if (null != dictionary.lookupWord(noDash, pos)) {
            dashNoDash++;
            //System.err.println("lemma: "+lemma+" dashNoDash "+dashNoDash);
          }
          final String dashToSpace = lemma.replace("-", " ");
          if (null != dictionary.lookupWord(dashToSpace, pos)) {
            dashSpace++;
            //System.err.println("lemma: "+lemma+" dashSpace "+dashSpace);
          } else {
            dashNotSpace++;
            //System.err.println("lemma: "+lemma+" dashNotSpace "+dashNotSpace);
          }
        }
        if (lemma.indexOf(" ") > 0) {
          space++;
        }
      }
    }
    System.err.println("dash: "+dash);
    System.err.println("space: "+space);
    System.err.println("dashNotSpace: "+dashNotSpace);
    System.err.println("spaceAndDash: "+spaceAndDash);
    System.err.println("dashNoDash: "+dashNoDash);
    System.err.println("dashSpace: "+dashSpace);
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