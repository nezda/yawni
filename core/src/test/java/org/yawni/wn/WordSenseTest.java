package org.yawni.wn;

import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class WordSenseTest {
  private static DictionaryDatabase dictionary;
  @BeforeClass
  public static void init() {
    dictionary = FileBackedDictionary.getInstance();
  }

  /**
   * verifies optimized {@link WordSense#getSenseKey()} is equal to simple, slow form
   */
  @Test
  public void testSenseKey() {
    System.err.println("testSenseKey");
    for (final WordSense sense : dictionary.wordSenses(POS.ALL)) {
      // NOTE: String != StringBuilder ! (use .toString() or contentEquals())
      assertEquals(sense.getSenseKey().toString(), getSenseKey(sense).toString());
    }
  }

  @Test
  public void testLexicalRelations() {
    System.err.println("testRelations");
    for (final WordSense sense : dictionary.wordSenses(POS.ALL)) {
      for (final LexicalRelation relation : sense.getRelations()) {
        assertTrue(relation.isLexical());
        //assertTrue("! type.isLexical(): "+relation, relation.getType().isLexical());
        if (relation.getType().isLexical() == false) {
          //System.err.println("CONFUSED "+relation);
        }
      }
    }
  }

  private String getSenseKey(final WordSense sense) {
    if (sense.getSynset().isAdjectiveCluster()) {
      return oldAdjClusterSenseKey(sense);
    } else {
      return oldNonAdjClusterSenseKey(sense);
    }
  }

  private String oldAdjClusterSenseKey(final WordSense sense) {
    final List<RelationTarget> adjsses = sense.getSynset().getTargets(RelationType.SIMILAR_TO);
    assert adjsses.size() == 1;
    final Synset adjss = (Synset) adjsses.get(0);
    // if satellite, key lemma in cntlist.rev
    // is adjss's first word (no case) and
    // adjss's lexid (aka lexfilenum) otherwise
    final String searchWord = adjss.getWordSenses().get(0).getLemma();
    final int headSense = adjss.getWordSenses().get(0).getLexid();
    return String.format("%s%%%d:%02d:%02d:%s:%02d",
        sense.getLemma().toLowerCase(),
        POS.SAT_ADJ.getWordNetCode(),
        sense.getSynset().lexfilenum(),
        sense.getLexid(),
        searchWord.toLowerCase(),
        headSense);
  }

  private String oldNonAdjClusterSenseKey(final WordSense sense) {
    return String.format("%s%%%d:%02d:%02d::",
        sense.getLemma().toLowerCase(),
        sense.getPOS().getWordNetCode(),
        sense.getSynset().lexfilenum(),
        sense.getLexid()
        );
  }
}