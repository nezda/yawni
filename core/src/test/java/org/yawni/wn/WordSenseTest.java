package org.yawni.wn;

import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.fest.assertions.Assertions.assertThat;

public class WordSenseTest {
  private static DictionaryDatabase dictionary;
  @BeforeClass
  public static void init() {
    dictionary = FileBackedDictionary.getInstance();
  }

  @Test
  public void testSpecificLexicalRelations() {
    System.err.println("testSpecificLexicalRelations");
    final WordSense viral = dictionary.lookupWord("viral", POS.ADJ).getSense(1);
    final WordSense virus = dictionary.lookupWord("virus", POS.NOUN).getSense(1);
    assertThat(viral.getRelationTargets(RelationType.DERIVATIONALLY_RELATED)).contains(virus);
    assertThat(virus.getRelationTargets(RelationType.DERIVATIONALLY_RELATED)).contains(viral);

    final WordSense hypocrite = dictionary.lookupWord("hypocrite", POS.NOUN).getSense(1);
    final WordSense hypocritical = dictionary.lookupWord("hypocritical", POS.ADJ).getSense(1);

    // relation missing from WordNet 3.0!
    // assertThat(hypocrite.getRelationTargets(RelationType.DERIVATIONALLY_RELATED)).contains(hypocritical);
    // relation missing from WordNet 3.0!
    // assertThat(hypocritical.getRelationTargets(RelationType.DERIVATIONALLY_RELATED)).contains(hypocrite);

    final WordSense hypocrisy = dictionary.lookupWord("hypocrisy", POS.NOUN).getSense(1);
    assertThat(hypocritical.getRelationTargets(RelationType.DERIVATIONALLY_RELATED)).contains(hypocrisy);
    assertThat(hypocrisy.getRelationTargets(RelationType.DERIVATIONALLY_RELATED)).contains(hypocritical);

    final WordSense palatine = dictionary.lookupWord("palatine", POS.NOUN).getSense(1);
    // roman2 is a hypernym of palatine
    final WordSense roman2 = dictionary.lookupWord("roman", POS.NOUN).getSense(2);
    // this will fail because the WordSense palatine has NO HYPERNYMs ? this is VERY confusing
//    assertThat(palatine.getRelationTargets(RelationType.HYPERNYM)).contains(roman2);
    assertThat(palatine.getRelationTargets(RelationType.HYPERNYM)).contains(roman2.getSynset());
    // this will fail because the WordSense palatine's Synset's HYPERNYM targets are Synsets, NOT WordSenses
//    assertThat(palatine.getSynset().getRelationTargets(RelationType.HYPERNYM)).contains(roman2);
    assertThat(palatine.getSynset().getRelationTargets(RelationType.HYPERNYM)).contains(roman2.getSynset());
  }

  @Test
  public void testLexicalRelations() {
    System.err.println("testRelations");
    for (final WordSense sense : dictionary.wordSenses(POS.ALL)) {
      for (final Relation relation : sense.getRelations()) {
        assertThat(relation.isLexical() ^ ! relation.getSource().equals(sense)).isTrue();
        //assertTrue("! type.isLexical(): "+relation, relation.getType().isLexical());
        if (! relation.getType().isLexical()) {
          //System.err.println("CONFUSED "+relation);
        }
      }
    }
  }

  /**
   * verifies optimized {@link WordSense#getSenseKey()} is equal to simple, slow form
   */
  @Test
  public void testSenseKey() {
    System.err.println("testSenseKey");
    for (final WordSense sense : dictionary.wordSenses(POS.ALL)) {
      // NOTE: String != StringBuilder ! (use .toString() or contentEquals())
      assertThat(sense.getSenseKey().toString()).isEqualTo(getSenseKey(sense).toString());
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
    final List<RelationTarget> adjsses = sense.getSynset().getRelationTargets(RelationType.SIMILAR_TO);
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