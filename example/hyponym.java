import edu.brandeis.cs.steele.wn.*;

/** 
	Displays the hyponyms for the given noun.
	A simple example of using JWordNet.
	For a more comprehensive example, take a look at the browser.

	Kurt Hayes
        
*/

public class hyponym {

	private static int iIndent = 0;
        private static boolean bExtended = true;

        private static void traverse(PointerTarget sense, PointerType pointerType)  {

		String sIndent = "";
		for (int i=0; i<iIndent; i++)
			sIndent += " ";

		sIndent += " =>";
            System.out.println(sIndent+sense.getDescription());

            PointerTarget[] parents = sense.getTargets(pointerType);


           for (int i = 0; i < parents.length; ++i) {
		iIndent++;
               traverse(parents[i], pointerType);
		iIndent--;
            }

        }



	static void main(String[] args) {

		if (args.length < 1) {
			System.out.println("Usage: java hyponym word");
			return;
		}

		// Open the database from its default location (as specified
		// by the WNHOME and WNSEARCHDIR properties).
		// To specify a pathname for the database directory, use
		//   new FileBackedDictionary(searchDir);
		// To use a remote server via RMI, use
		//   new FileBackedDictionary(RemoteFileManager.lookup(hostname));
		DictionaryDatabase dictionary = FileBackedDictionary.getInstance();
		// For this example, we use POS.NOUN. However, POS.VERB, POS.ADJ, POS.ADV are also valid.
		IndexWord word = dictionary.lookupIndexWord(POS.NOUN, args[0]);
		Synset[] senses = word.getSynsets();
		int taggedCount = word.getTaggedSenseCount();
		System.out.print("The " + word.getPOS().getLabel() + " " + word.getLemma() + " has " + senses.length + " sense" + (senses.length == 1 ? "" : "s") + " ");
		System.out.print("(");
		if (taggedCount == 0) {
			System.out.print("no senses from tagged texts");
		} else {
			System.out.print("first " + taggedCount + " from tagged texts");
		}
		System.out.print(")\n\n");


		for (int i=0; i<senses.length; i++) {

			iIndent = 0;
	                Synset sense = senses[i];
	                System.out.println("" + (i + 1) + ". " + sense.getDescription());
	
	                System.out.println("");
	
			// Change next line to HYPERNYM to get hypernyms.
	                traverse(senses[i], PointerType.HYPONYM);
			System.out.print("\n\n");
		}

	}
}

