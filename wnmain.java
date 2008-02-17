import edu.brandeis.cs.steele.wn.*;

/* This prints the senses of the noun 'dog' to the console. */
public class wnmain {
  static void main(String[] args) {
    // Open the database from its default location (as specified
    // by the WNHOME and WNSEARCHDIR properties).
    // To specify a pathname for the database directory, use
    //   new FileBackedDictionary(searchDir);
    // To use a remote server via RMI, use
    //   new FileBackedDictionary(RemoteFileManager.lookup(hostname));
    DictionaryDatabase dictionary = new FileBackedDictionary();
    IndexWord word = dictionary.lookupIndexWord(POS.NOUN, "dog");
    Synset[] senses = word.getSynsets();
    int taggedCount = word.getTaggedSenseCount();
    System.out.print("The " + word.getPOS().getLabel() + " " + word.getLemma() + 
        " has " + senses.length + " sense" + (senses.length == 1 ? "" : "s") + " ");
    System.out.print("(");
    if (taggedCount == 0) {
      System.out.print("no senses from tagged texts");
    } else {
      System.out.print("first " + taggedCount + " from tagged texts");
    }
    System.out.print(")\n\n");
    for (int i = 0; i < senses.length; ++i) {
      Synset sense = senses[i];
      System.out.println("" + (i + 1) + ". " + sense.getLongDescription());

      System.out.println("");
    }
  }
}

