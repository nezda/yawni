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

    // Semantic (i.e., non-lexical) relations of a WordSense's Synset are not currently returned
    // by getRelation*(*) methods which is confusing and error prone (burned me, the designer!).
    //
    //   public List<? extends Relation> getRelations()
    //   public List<? extends Relation> getRelations(RelationType type);
    //   public List<RelationTarget> getRelationTargets();
    //   public List<RelationTarget> getRelationTargets(RelationType type);
    //
    // - Returning non-lexical relations from WordSense implementations of getRelation*(*) methods would
    //   introduce a runtime asymmetry between Synset and WordSense;
    // - WordSense versions would always return at least what Synset versions return, plus any defined
    //   lexical relations.  WordSense implementation signatures would change* from:
    //     public List<LexicalRelation> getRelation*(*)
    //     - to -
    //     public List<Relation> getRelation*(*)
    //   - Synset implementation signatures are already :
    //       public List<Relation> getRelation*(*),
    //     because ALL relations are <em>stored</em> in Synset's relations field (implementation detail).
    //   - Synset does however provide the addtional accessor
    //       public List<SemanticRelation> getSemanticRelations(final RelationType type)
    //   - Could easily add complementary Synset method:
    //       public List<SemanticRelation> getSemanticRelations()
    //   - and complemenatary WordSense methods:
    //       public List<LexicalRelation> getLexicalRelations(RelationType type)
    //       public List<LexicalRelation> getLexicalRelations()
    //
    // * Current <? extends Relation> generic parameterization of RelationTarget prohibits mixing Lexical
    //   and Semantic -Relations UNLESS we return base, Relation, rather than either subclass.
    //
    // RelationTarget's purpose(s):
    // - Acts as common interface to Synset and WordSense, which form a composite pair, as evidenced by both
    //   being Iterable<WordSense> and having getSynset()
    // - Original, primary purpose is to provide class for getRelationTarget(*) methods.
    // - Both are indepently Iterable<WordSense> without this interface.
    // - Defines common getDescription() / getLongDescription() methods.
    //   ? maybe this is better implemented by visitor API ?
    //     visit(WordSenseVisitor), visit(SynsetVisitor)
    //   ? maybe a collection of interfaces would be better ?
    //     (Iterable<WordSense>)
    //     GetSynset
    //     GetDescription
    //     GetLongDescription
    //
    // Note related issues manifest themselves in Word.getRelationTypes() which causes bad decisions to be
    // made in BrowserPanel.RelationTypeComboBox.void updateFor(POS pos, Word word)
    //
    // This will need to be carefuly documented and exhaustively unit tested.
    //   RelationTarget getRelation*(*) methods return maximal set of ...
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