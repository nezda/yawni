
import edu.brandeis.cs.steele.wn.*;

/**
  Displays the domain relationships for the given noun.
  A simple example of using JWordNet.
  For a more comprehensive example, take a look at the browser.

  Kurt Hayes
 */
public class domain {

  private static int iIndent = 0;

  private static void traverse(PointerTarget sense, PointerType pointerType) {
    String sIndent = "";
    for (int i = 0; i < iIndent; i++)
      sIndent += " ";

    sIndent += " =>";
    //System.out.println(sIndent + sense.getDescription());

    PointerTarget[] children = sense.getTargets(pointerType);

    for (int i = 0; i < children.length; ++i) {
      iIndent++;
      System.out.println(sIndent + pointerType.getLabel()+":"+children[i].getDescription());
      iIndent--;
    }
  }

  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.println("Usage: java domain word");
      return;
    }
    // Open the database from its default location (as specified
    // by the WNHOME and WNSEARCHDIR properties).
    // To specify a pathname for the database directory, use
    //   FileBackedDictionary.getInstance(searchDir);
    // To use a remote server via RMI, use
    //   FileBackedDictionary.getInstance(RemoteFileManager.lookup(hostname));
    final DictionaryDatabase dictionary = FileBackedDictionary.getInstance();
    // For this example, we use POS.NOUN. However, POS.VERB, POS.ADJ, POS.ADV are also valid.
    IndexWord word = dictionary.lookupIndexWord(POS.NOUN, args[0]);
    Synset[] senses = word.getSynsets();
    int taggedCount = word.getTaggedSenseCount();
    System.out.print("The " + word.getPOS().getLabel() + " " + word.getLemma() + " has " +
        senses.length + " sense" + (senses.length == 1 ? "" : "s") + " ");
    System.out.print("(");
    if (taggedCount == 0) {
      System.out.print("no senses from tagged texts");
    } else {
      System.out.print("first " + taggedCount + " from tagged texts");
    }
    System.out.print(")\n\n");

    for (int i = 0; i < senses.length; i++) {
      iIndent = 0;
      Synset sense = senses[i];
      System.out.println("" + (i + 1) + ". " + sense.getDescription());

      System.out.println("");

      traverse(senses[i], PointerType.DOMAIN_OF_TOPIC);
      traverse(senses[i], PointerType.DOMAIN_OF_REGION);
      traverse(senses[i], PointerType.DOMAIN_OF_USAGE);
      traverse(senses[i], PointerType.MEMBER_OF_TOPIC_DOMAIN);
      traverse(senses[i], PointerType.MEMBER_OF_REGION_DOMAIN);
      traverse(senses[i], PointerType.MEMBER_OF_USAGE_DOMAIN);
      System.out.print("\n\n");
    }
  }
}

