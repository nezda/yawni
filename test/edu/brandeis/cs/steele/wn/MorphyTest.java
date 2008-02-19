package edu.brandeis.cs.steele.wn;

import junit.framework.TestCase;
import java.util.*;

public class MorphyTest extends TestCase {
  public void test1() {
    final DictionaryDatabase dictionary = FileBackedDictionary.getInstance();
    String[][] unstemmedStemmedCases = new String[][] {
      { POS.NOUN.name(), "dogs", "dog" },
      { POS.NOUN.name(), "geese", "goose" },
      { POS.NOUN.name(), "handfuls", "handful" },
      //"true case" bug { POS.NOUN.name(), "villas", "villa" },
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
      { POS.VERB.name(), "dogs", "dog" },
      { POS.ADJ.name(), "onliner" /* no idea */, "online" },
      // should both variants be returned ? { POS.ADJ.name(), "onliner" /* no idea */, "on-line" },
      { POS.ADJ.name(), "redder" /* no idea */, "red" },
    };
    for(final String[] unstemmedStemmed : unstemmedStemmedCases) {
      final POS pos = POS.valueOf(unstemmedStemmed[0]);
      final String unstemmed = unstemmedStemmed[1];
      final String stemmed = unstemmedStemmed[2];
      final List<String> baseForms = Arrays.asList(dictionary.lookupBaseForms(pos, unstemmed));
      assertTrue("unstemmed: \""+unstemmed+"\" "+pos+" gold: \""+stemmed+"\" output: "+baseForms,
          baseForms.contains(stemmed) || (stemmed == null && baseForms.isEmpty()));
    }
  }
  // TODO
  // - upgrade to junit4 (@Test, @Ignore, paramterized tests) - don't forget adapter
  // - test plan
  //   - Morphy
  //     - make sure caching strategies are not harming correctness (uses DatabaseKey(pos, someString))
  //   - specific synsets
  //     - using offets will require changes as WN is improved
  //     - could use lemmma, pos, and (sense number OR gloss)
  //   - other relations including derivationally related
  //   * DictionaryDatabase iteration (require Xmx)
  //     - check if iteration returns first AND last item (boundary cases) - look at data files manually
  //   * multi-threaded correctness and no exceptions 
  //   - sense numbers
  //   - gloss
  //   - compare to parsed output of 'wn' binary (optional - @Ignore and/or boolean flag)
}
