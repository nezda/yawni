package example;

import java.util.*;
import edu.brandeis.cs.steele.wn.*;

/** 
 * Displays the hyponyms for the given noun.
 * A simple example of using JWordNet.
 * For a more comprehensive example, take a look at the browser.
 *
 * @author Kurt Hayes
 */
public class Example {
  private static int iIndent = 0;

  private static void traverse(PointerTarget sense, PointerType pointerType)  {
    String sIndent = "";
    for (int i=0; i<iIndent; i++) {
      sIndent += " ";
    }

    sIndent += " =>";
    System.err.println(sIndent+sense.getDescription());

    PointerTarget[] parents = sense.getTargets(pointerType);

    for (int i = 0; i < parents.length; ++i) {
      iIndent++;
      traverse(parents[i], pointerType);
      iIndent--;
    }
  }

  private static void original(String[] args) {
    if (args.length < 1) {
      System.err.println("Usage: java hyponym word");
      return;
    }

    // Open the database from its default location (as specified
    // by the WNHOME and WNSEARCHDIR properties).
    // To specify a pathname for the database directory, use
    //   FileBackedDictionary.getInstance(searchDir);
    // To use a remote server via RMI, use
    //   FileBackedDictionary.getInstance(RemoteFileManager.lookup(hostname));
    DictionaryDatabase dictionary = FileBackedDictionary.getInstance();
    // For this example, we use POS.NOUN. However, POS.VERB, POS.ADJ, POS.ADV are also valid.
    Word word = dictionary.lookupWord(POS.NOUN, args[0]);
    Synset[] senses = word.getSynsets();
    int taggedCount = word.getTaggedSenseCount();
    System.err.print("The " + word.getPOS().getLabel() + " " + word.getLemma() + 
        " has " + senses.length + " sense" + (senses.length == 1 ? "" : "s") + " ");
    System.err.print("(");
    if (taggedCount == 0) {
      System.err.print("no senses from tagged texts");
    } else {
      System.err.print("first " + taggedCount + " from tagged texts");
    }
    System.err.print(")\n\n");

    for (int i=0; i<senses.length; i++) {
      iIndent = 0;
      final Synset sense = senses[i];
      System.err.println("" + (i + 1) + ". " + sense.getDescription());

      System.err.println("");

      // Change next line to HYPERNYM to get hypernyms.
      traverse(senses[i], PointerType.HYPONYM);
      System.err.print("\n\n");
    }
  }

  private static void iterate(final String[] args)  throws Exception {
    final DictionaryDatabase dictionary = FileBackedDictionary.getInstance();
    // NOUN
    // VERB
    // ADJ
    // ADV
    for(final Synset synset : dictionary.synsets(POS.valueOf(args[0]))) {
      System.err.println(synset);
      //e.next();
    }
    System.err.println("finished "+args[0]);
  }

  private static void lookupPossibleSynsets(final String[] args)  throws Exception {
    final DictionaryDatabase dictionary = FileBackedDictionary.getInstance();
    final Synset[] syns = dictionary.lookupSynsets(POS.valueOf(args[0]), args[1]);
    if(syns != null) {
      for(final Synset syn : syns) {
        System.err.println(syn);
      }
    }
    System.err.println("finished "+args[0]+" for "+args[1]);
  }

  static class FindAmbiguity {
    static void iterate(final String[] args)  throws Exception {
      final DictionaryDatabase dictionary = FileBackedDictionary.getInstance();
      for(final POS pos : POS.CATS) {
        for(final Word word : dictionary.words(pos)) {
          //System.err.println(indexWord);
          //for(final Synset synset : indexWord.getSynsets()) {
          //  for(final WordSense wordSense : synset.getWords()) {
          //    System.err.println("  "+wordSense);
          //  }
          //}
          final String[] stems = dictionary.lookupBaseForms(pos, word.getLemma());
          final boolean hasLemma = 0 <= indexOf(word.getLemma(), stems);
          //System.err.println(indexWord+" stems.length: "+stems.length+" hasLemma: "+hasLemma+" stems: "+Arrays.toString(stems));
          if(hasLemma == false) {
            System.err.println(word+" stems.length: "+stems.length+" hasLemma: "+hasLemma+" stems: "+Arrays.toString(stems));
          }
          if(stems.length != 1) {
            System.err.println(stems.length+" "+word+" "+Arrays.toString(stems)+" lemma: \""+word.getLemma()+"\"");
          }
        }
      }
      System.err.println("finished.");
    }
  }

  private static int indexOf(final String key, final String[] strings) {
    for(int i=0; i<strings.length; ++i) {
      if(key.equalsIgnoreCase(strings[i])) {
        return i;
      }
    }
    return -1;
  }

  static class ShowDerivations {
    // starting with a given Word, find all derivationally related Words (if any)
    // some are in the same POS, others are not
    static void iterate(final String[] args)  throws Exception {
      final DictionaryDatabase dictionary = FileBackedDictionary.getInstance();
      for(final POS pos : POS.CATS) {
        for(final Word word : dictionary.words(pos)) {
          //System.err.println(indexWord);
          //for(final Synset synset : indexWord.getSynsets()) {
          //  for(final WordSense wordSense : synset.getWords()) {
          //    System.err.println("  "+wordSense);
          //  }
          //}
        }
      }
      System.err.println("finished.");
    }
  }

  // TODO
  // add JUnit tests
  // - multi-threaded tests (crash first, then correctness)

  public static void main(String[] args) throws Exception {
    FindAmbiguity.iterate(args);
    //lookupPossibleSynsets(args);
    //iterate(args);
    //original(args);
  }
}

