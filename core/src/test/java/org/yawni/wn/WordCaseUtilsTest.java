package org.yawni.wn;

import org.junit.Before;
import org.junit.Test;
import static org.fest.assertions.Assertions.assertThat;

public class WordCaseUtilsTest {
  private DictionaryDatabase dictionary;
  @Before
  public void init() {
    dictionary = FileBackedDictionary.getInstance();
  }

  @Test
  public void test() {
    String lemma;
    Word word;
    lemma = "CD";
    word = getWord(lemma, POS.NOUN);
    System.err.println("case variants: "+WordCaseUtils.getUniqueLemmaCaseVariants(word));
    System.err.println("dominant case: "+WordCaseUtils.getDominantCasedLemma(word));
    assertThat(WordCaseUtils.getUniqueLemmaCaseVariants(word)).containsExactly("Cd", "cd", "CD");
    assertThat(WordCaseUtils.getDominantCasedLemma(word)).isEqualTo("cd");

    lemma = "roma";
    word = getWord(lemma, POS.NOUN);
    System.err.println("case variants: "+WordCaseUtils.getUniqueLemmaCaseVariants(word));
    System.err.println("dominant case: "+WordCaseUtils.getDominantCasedLemma(word));
    assertThat(WordCaseUtils.getUniqueLemmaCaseVariants(word)).containsExactly("Roma");
    assertThat(WordCaseUtils.getDominantCasedLemma(word)).isEqualTo("Roma");
    
    lemma = "rom";
    word = getWord(lemma, POS.NOUN);
    System.err.println("case variants: "+WordCaseUtils.getUniqueLemmaCaseVariants(word));
    System.err.println("dominant case: "+WordCaseUtils.getDominantCasedLemma(word));
    assertThat(WordCaseUtils.getUniqueLemmaCaseVariants(word)).containsExactly("ROM");
    assertThat(WordCaseUtils.getDominantCasedLemma(word)).isEqualTo("ROM");
  }

  private Word getWord(final String lemma, final POS pos) {
    return dictionary.lookupWord(lemma, pos);
  }
}