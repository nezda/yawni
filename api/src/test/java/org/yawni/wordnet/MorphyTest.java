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
package org.yawni.wordnet;

import org.junit.*;
import static org.junit.Assert.*;

import java.util.*;

import static org.fest.assertions.Assertions.assertThat;

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
 *     - get stems of every lemma in all DBs ("wounds" → "wound" → "wind")
 *     - compare speed with various CharStream impls (add some package private methods)
 *   - sense numbers
 *   - gloss
 *   - compare to parsed output of 'wn' binary (optional - @Ignore and/or boolean flag)
 *
 * TODO add tests with prepositions
 * TODO consider proper Parameterized tests
 */
public class MorphyTest {
  private WordNetInterface wn;
  @Before
  public void init() {
    wn = WordNet.getInstance();
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
  public void coreTest() {
    String[][] unstemmedStemmedCases = new String[][] {
      // { POS, <unstemmed>, <stemmed> }
      { POS.NOUN.name(), "dogs", "dog" },
      { POS.NOUN.name(), "geese", "goose", "geese" },
      { POS.NOUN.name(), "handfuls", "handful" },
      { POS.NOUN.name(), "villas", "Villa", "villa" }, // exposed true case bug
      { POS.NOUN.name(), "Villa", "Villa", "villa" }, // exposed true case bug
      { POS.NOUN.name(), "br", "Br", "BR" }, // exposed true case bug
      { POS.NOUN.name(), "heiresses", "heiress" },
        //WN missing derivationally relation to neuter form "heir" - intentional?
      { POS.NOUN.name(), "heiress", "heiress" },
      { POS.NOUN.name(), "George W. \t\tBush", "George W. Bush" }, // WN doesn't get this (extra internal space) (WN online does - probably better input string preprocessing)
      { POS.NOUN.name(), "george w. bush", "George W. Bush" },
      //{ POS.NOUN.name(), "f.d. roosevelt", "F. D. Roosevelt" }, // WN doesn't get this (weird entry HAS extra internal space)
      //{ POS.NOUN.name(), "u.s", "u.s."}, // WN gets this via "US"
      //WN doesn't get this either (missing ".") { POS.NOUN.name(), "george w bush", "George W. Bush" },
      { POS.NOUN.name(), "mice", "mouse", "mice" },
      { POS.NOUN.name(), "internal-combustion engine", "internal-combustion engine" }, // simple reflexive test
      { POS.NOUN.name(), "internal combustion engine", "internal-combustion engine" }, // exercise getindex() logic; WN doesn't get this
      { POS.NOUN.name(), "internal combustion engines", "internal-combustion engine" }, // exercise getindex() logic; WN doesn't get this
      { POS.NOUN.name(), "hangers-on", "hanger-on", "hangers-on" },
      { POS.NOUN.name(), "hangers on", "hanger-on" }, // needs " " → "-"
      { POS.NOUN.name(), "letter bombs", "letter bomb" },
      // { POS.NOUN.name(), "fire-bomb", "firebomb" }, // needs "-" → ""
      { POS.NOUN.name(), "letter-bomb", "letter bomb" }, // needs "-" → " "
      // harder one:
      // - needs to either not require getIndexedLinePointer() to operate on words OR
      //   + return the nearest hit (maybe negated to indicate no normal match)
      // - could be really fast with a suffix index (reverse words)
      // { POS.NOUN.name(), "letterbomb", "letter bomb" }, // needs "" → " " ; WN doesn't get this
      //{ POS.NOUN.name(), "hyper-active", "hyperactive" }, // WN doesn't get this
      { POS.NOUN.name(), "I ran", null }, // WN gets this as "Iran" - " " → "" seems bad unless a variant has "-" in same position (WN online doesn't do this)
      { POS.NOUN.name(), "be an", null }, // WN gets this as "bean" (WN online doesn't do this)
      { POS.NOUN.name(), "are a", null }, // WN gets this as "area" (WN online doesn't do this)
      { POS.NOUN.name(), " Americans", "American" }, // WN doesn't get this
      { POS.NOUN.name(), "_slovaks_", "Slovak" }, // WN doesn't get this
      { POS.NOUN.name(), "superheroes", "superhero", "superheroes" }, // NOTE: this isn't in WordNet (Brett Spell noted this)
      { POS.NOUN.name(), "businessmen", "businessman", "businessmen" },
      { POS.NOUN.name(), "_", null },
      { POS.NOUN.name(), "\n", null },
      { POS.NOUN.name(), "\ndog", "dog" },
      { POS.NOUN.name(), "dog\n", "dog" },
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
			// from http://mail-archives.apache.org/mod_mbox/opennlp-users/201208.mbox/%3C70649AB6-40E1-47FA-9B40-E18691627F36%40yahoo.com%3E
			// yawni tokenizes this on underscores
			{ POS.NOUN.name(), "found_r_n_rnhttpsttlc_blablacompost_show_post_full_view_dw_w_b_ar_dxxcfazbay_ppid_rnrn", null },
      { POS.NOUN.name(), "armful", "armful" },
      { POS.NOUN.name(), "attorneys general", "attorney general", "Attorney General" },
      { POS.NOUN.name(), "axes", "axis", "ax", "axes", "Axis" }, // NOTE: noun "axe" is only derivationally related to "ax"
      { POS.NOUN.name(), "bases", "basis", "base", "bases" }, // NOTE: noun "basis" ?
      { POS.NOUN.name(), "boxesful", "boxful" },
      //{ POS.NOUN.name(), "bachelor of art", "Bachelor of Arts" }, //currently fails - known morpphy algorihm bug (http://wordnet.princeton.edu/man/morphy.7WN.html#toc8)
      { POS.NOUN.name(), "Bachelor of Sciences in Engineering", "Bachelor of Science in Engineering" },
      { POS.NOUN.name(), "cd", "Cd", "cd", "CD" },
      { POS.NOUN.name(), "lines of business", "line of business" },
      { POS.NOUN.name(), "SS", "SS" },
      { POS.NOUN.name(), "mamma's boy", "mamma's boy" },
      { POS.NOUN.name(), "15_minutes", "15 minutes" },
      { POS.NOUN.name(), "talks", "talk", "talks" },
      { POS.NOUN.name(), "talk", "talk" }, // note asymmetry: "talk" → {"talk"}, "talks" → {"talk", "talks"}
      { POS.NOUN.name(), "values", "value", "values" },
      { POS.NOUN.name(), "value", "value" }, // note asymmetry: "value" → {"value"}, "values" → {"value", "values"}
      { POS.NOUN.name(), "wounded", "wounded" },
      { POS.NOUN.name(), "yourselves", "yourself", "yourselves" }, // no Word for this pronoun
      { POS.NOUN.name(), "wounding", "wounding" },
      { POS.NOUN.name(), "'s Gravenhage", "'s Gravenhage" },
      { POS.NOUN.name(), "parts of speech", "part of speech" },
      { POS.NOUN.name(), "read/write memory", "read/write memory"},
      //{ POS.NOUN.name(), "read write memory", "read/write memory"}, // WN doesn't get this
      { POS.NOUN.name(), "roma", "rom", "roma", "Roma"}, // "Rom" is the singular form of "Roma": this is reflected in the exceptions file, but missing elsewhere; "roma" (is artifact of exceptions file)
      { POS.NOUN.name(), "rom", "ROM"}, // only "read-only memory"
      { POS.ADJ.name(), "KO'd", "KO'd" },
      { POS.VERB.name(), "KO'd", "ko", "ko'd" }, // no Word for the verb form of this ∴ no true case support
      { POS.VERB.name(), "booby-trapped", "booby-trap", "booby-trapped" }, // no Word for the verb form of this
      { POS.VERB.name(), "bird-dogged", "bird-dog", "bird-dogged" }, // no Word for this exceptional verb
      { POS.VERB.name(), "wounded", "wound" },
      { POS.VERB.name(), "wound", "wind", "wound" },
      { POS.ADJ.name(), "wounded", "wounded" },
      { POS.VERB.name(), "dogs", "dog" },
      { POS.VERB.name(), "abided by", "abide by" },
      { POS.VERB.name(), "gave a damn", "give a damn" },
      { POS.VERB.name(), "asking for it", "ask for it" },
      { POS.VERB.name(), "asked for it", "ask for it" },
      { POS.VERB.name(), "accounting for", "account for" },
      { POS.VERB.name(), "was", "be", "was" }, // 2 "stems", 1 baseform
      { POS.VERB.name(), "founded", "found" },
      { POS.VERB.name(), "founder", "founder" }, // note asymmetries: "founder" → {"founder"}, "founded" → {"found"}, "found" → {"find", "found"}
      { POS.VERB.name(), "found", "find", "found"},
      { POS.VERB.name(), "names of", null},
      { POS.VERB.name(), "names of association football", null},
      { POS.ADJ.name(), "founder", "founder" },
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
      { POS.VERB.name(), "let the cats out of the bag", "let the cat out of the bag" },
      { POS.ADJ.name(), "onliner" /* no idea */, "online" },
      // should both variants be returned ? { POS.ADJ.name(), "onliner" /* no idea */, "on-line" },
      { POS.ADJ.name(), "redder" /* no idea */, "red" },
      { POS.ADJ.name(), "Middle Eastern", "Middle Eastern" }, // capitalized adj - grep "a [0-9]+ [A-Z]" data.adj
      { POS.ADJ.name(), "Latin-American", "Latin-American" }, // capitalized adj - grep "a [0-9]+ [A-Z]" data.adj
      { POS.ADJ.name(), "low-pitched", "low-pitched" },
      //{ POS.ADJ.name(), "low-pitch", "low-pitched" }, // WN doesn't get this
    };
    for (final String[] testElements : unstemmedStemmedCases) {
      final POS pos = POS.valueOf(testElements[0]);
      final String unstemmed = testElements[1];
      final String stemmed = testElements[2];
      final List<String> goldStems = new ArrayList<String>();
      for (int i = 2; i < testElements.length; ++i) {
        goldStems.add(testElements[i]);
      }
      assertTrue("goldStems: "+goldStems, areUnique(goldStems));
      final List<String> baseForms = stem(unstemmed, pos);
      String msg = "unstemmed: \""+unstemmed+"\" "+pos+" gold: \""+stemmed+"\" output: "+baseForms;
      assertTrue(msg, baseForms.contains(stemmed) || (stemmed == null && baseForms.isEmpty()));
      //System.err.println(msg);
      assertFalse("baseForms: "+baseForms, baseFormContainsUnderScore(baseForms));
      //TODO on failure, could try other POS
      if (baseForms.size() >= 2 && ! goldStems.equals(baseForms)) {
        //TODO tighten up this test - don't allow any extra unspecified variants
        // note this considers case variants distinct
        System.err.println("extra variants for \""+unstemmed+"\": "+baseForms+" goldStems: "+goldStems);
      }
      assertTrue(areUnique(baseForms));

      final List<String> upperBaseForms = stem(unstemmed.toUpperCase(), pos);
      msg = "UPPER unstemmed: \""+unstemmed+"\" "+pos+" gold: \""+stemmed+"\" output: "+upperBaseForms;
      assertTrue(msg, upperBaseForms.contains(stemmed) || (stemmed == null && upperBaseForms.isEmpty()));
    }
  }

  @Test
  public void testLookupWord() {
    String lemma;
    lemma = "";
    assertNull("lemma: "+lemma, wn.lookupWord(lemma, POS.NOUN));
    lemma = "dog";
    assertNotNull("lemma: "+lemma, wn.lookupWord(lemma, POS.NOUN));
    lemma = "DOG";
    assertNotNull("lemma: "+lemma, wn.lookupWord(lemma, POS.NOUN));
    lemma = "ad blitz";
    assertNotNull("lemma: "+lemma, wn.lookupWord(lemma, POS.NOUN));
    lemma = "ad_blitz";
    assertNotNull("lemma: "+lemma, wn.lookupWord(lemma, POS.NOUN));
    lemma = "AD BLITZ";
    assertNotNull("lemma: "+lemma, wn.lookupWord(lemma, POS.NOUN));
    lemma = "wild-goose chase";
    assertNotNull("lemma: "+lemma, wn.lookupWord(lemma, POS.NOUN));
    lemma = "wild-goose_chase";
    assertNotNull("lemma: "+lemma, wn.lookupWord(lemma, POS.NOUN));
  }

  @Test
  public void testGetExceptions() {
    final WordNet wordNet = (WordNet) wn;
    String lemma;
    lemma = "";
    assertThat(wordNet.getExceptions(lemma, POS.NOUN)).isEmpty();
    lemma = "dog";
    assertThat(wordNet.getExceptions(lemma, POS.NOUN)).isEmpty();
    lemma = "geese";
    assertThat(wordNet.getExceptions(lemma, POS.NOUN)).contains("goose");
    lemma = "geese";
    assertThat(wordNet.getExceptions(lemma, POS.NOUN).get(0)).isEqualTo("geese");
    lemma = "goose";
    assertThat(wordNet.getExceptions(lemma, POS.NOUN)).isEmpty();
  }

  // could add explicit checks for this in API methods but that's pretty tedious
  @Test(expected=NullPointerException.class)
  public void testNullLookupWord() {
    assertNull(wn.lookupWord(null, POS.NOUN));
  }

  @Test
  public void testWordSense() {
    assertEquals(42, wn.lookupWord("dog", POS.NOUN).getSense(1).getSensesTaggedFrequency());
    assertEquals(2, wn.lookupWord("dog", POS.VERB).getSense(1).getSensesTaggedFrequency());
    assertEquals(3, wn.lookupWord("cardinal", POS.ADJ).getSense(1).getSensesTaggedFrequency());
    assertEquals(0, wn.lookupWord("cardinal", POS.ADJ).getSense(2).getSensesTaggedFrequency());
    assertEquals(9, wn.lookupWord("concrete", POS.ADJ).getSense(1).getSensesTaggedFrequency());
    assertEquals(1, wn.lookupWord("dogmatic", POS.ADJ).getSense(1).getSensesTaggedFrequency());
  }

  @Test
  public void detectLostVariants() {
    int issues = 0;
    int nonCaseIssues = 0;
    for (final POS pos : POS.CATS) {
      for (final Word word : wn.words(pos)) {
        for (final WordSense wordSense : word.getWordSenses()) {
          final String lemma = wordSense.getLemma();
          final List<String> restems = stem(lemma, pos);
          String msg = "ok";
          if (! restems.contains(lemma)) {
            msg = "restems: " + restems + " doesn't contain lemma: " + lemma;
            issues++;
            boolean nonCaseIssue = ! containsIgnoreCase(lemma, restems);
            if (nonCaseIssue) {
              nonCaseIssues++;
            }
            System.err.println(
              "issues: " + issues + " nonCases: " + nonCaseIssues +
              (nonCaseIssue ? "*" : " ") +
              " " + msg);
          }
          if (restems.size() > 1) {
            //System.err.println(pos+" lemma: "+lemma+" restems: "+restems);
          }
          assertTrue(msg, restems.contains(lemma));
          // note that this considers case variants distinct
          assertTrue(areUnique(restems));
          // case variants are present
          //assertFalse(word+" "+wordSense+" restems: "+restems, baseFormContainsUpperCase(restems));
        }
      }
    }
    IterationTest.printMemoryUsage();
  }

  @Test
  public void findCollocationAmbiguity() {
    int spaceAndDash = 0;
    for (final POS pos : POS.CATS) {
      for (final Word word : wn.words(pos)) {
        final String lemma = word.getLowercasedLemma();
        if (lemma.indexOf('-') > 0 && lemma.indexOf(' ') > 0) {
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
      for (final Word word : wn.words(pos)) {
        final String lemma = word.getLowercasedLemma();
        if (lemma.indexOf('-') > 0) {
          dash++;
          final String noDash = lemma.replace("-", "");
          if (null != wn.lookupWord(noDash, pos)) {
            dashNoDash++;
            //System.err.println("lemma: "+lemma+" dashNoDash "+dashNoDash);
          }
          final String dashToSpace = lemma.replace('-', ' ');
          if (null != wn.lookupWord(dashToSpace, pos)) {
            dashSpace++;
            //System.err.println("lemma: "+lemma+" dashSpace "+dashSpace);
          } else {
            dashNotSpace++;
            //System.err.println("lemma: "+lemma+" dashNotSpace "+dashNotSpace);
          }
        }
        if (lemma.indexOf(' ') > 0) {
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
  //  int issues = 0;
  //  int nonCaseIssues = 0;
  //  for(final POS pos : POS.CATS) {
  //    for(final Word word : dictionary.words(pos)) {
  //      for(final WordSense wordSense : word.getWordSenses()) {
  //        final String lemma = wordSense.getLowercasedLemma();
  //        for(final POS otherPOS : POS.CATS) {
  //          if(otherPOS == pos) {
  //            continue;
  //          }
  //          // TODO implement getindex() and then activate this test
  //          // search for this lemma in other POS
  //          // see if we can find lexical ambiguity
  //          // e.g., NOUN("long time")
  //          // ADJ("longtime)
  //        }
  //        //XXX final List<String> restems = stem(lemma, pos);
  //        //XXX String msg = "ok";
  //        //XXX if(! restems.contains(lemma)) {
  //        //XXX   msg = "restems: "+restems+" doesn't contain lemma: "+lemma;
  //        //XXX   ++issues;
  //        //XXX   boolean nonCaseIssue = ! containsIgnoreCase(lemma, restems);
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

  private List<String> stem(final String someString, final POS pos) {
    return wn.lookupBaseForms(someString, pos);
  }

  // note this relies on equals() and hashCode()
  private static <T> boolean areUnique(final Collection<T> items) {
    return items.size() == new HashSet<T>(items).size();
  }

  //TODO consider moving to Utils
  private static boolean containsIgnoreCase(final String needle, final Iterable<String> haystack) {
    for (final String item : haystack) {
      if (item.equalsIgnoreCase(needle)) {
        return true;
      }
    }
    return false;
  }

  private static boolean baseFormContainsUnderScore(final Iterable<String> baseForms) {
    for (final String baseForm : baseForms) {
      if (baseForm.indexOf('_') >= 0) {
        return true;
      }
    }
    return false;
  }

  private static boolean baseFormContainsUpperCase(final Iterable<String> baseForms) {
    for (final String baseForm : baseForms) {
      for (int i = 0, n = baseForm.length(); i < n; i++) {
        final char c = baseForm.charAt(i);
        if (Character.isUpperCase(c)) {
          return true;
        }
      }
    }
    return false;
  }
}