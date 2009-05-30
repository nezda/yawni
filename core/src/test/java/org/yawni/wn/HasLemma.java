package org.yawni.wn;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.BaseMatcher;

//"item", isIn(collection<T>)
//- matches("item") => collection.contains("item")

//different T's for factory method and matches()
//"lemma" isLemmaOf(word)
//- matches("lemma") => word.getLemma().equals("lemma")
//"lemma" isLemmaOf(wordSense)

// inverted order (hasLemma("lemma")
//word, hasLemma("lemma")
//- matches(word) => "lemma".equals(word.getLemma())

// maybe would be more properly implemented as IsLemmaOf ?
// assertThat("'hood", isLemmaOf(wordSenseX))
//
// hack to support Word and WordSense which currently have no
// common interface (e.g., GetLemma/HaveLemma/Lemma)
public class HasLemma<T> extends BaseMatcher<T> {
  private final Object wordOrWordSense;
  
  private HasLemma(final Word word) {
    this.wordOrWordSense = word;
  }
  private HasLemma(final WordSense wordSense) {
    this.wordOrWordSense = wordSense;
  }
  public boolean matches(Object operand) {
    String lemma = (String) operand;
    if (wordOrWordSense instanceof Word) {
      return lemma.equals(((Word)wordOrWordSense).getLemma());
    } else if (wordOrWordSense instanceof WordSense) {
      return lemma.equals(((WordSense)wordOrWordSense).getLemma());
    } else {
      throw new IllegalArgumentException("wordOrWordSense: "+wordOrWordSense+
        " unsupported class: " + wordOrWordSense.getClass());
    }
  }
  public void describeTo(final Description description) {
    description.
      appendText("is lemma of ").
      appendValue(wordOrWordSense);
  }
  @Factory
  public static <T> Matcher<T> isLemmaOf(final Word operand) {
    return new HasLemma<T>(operand);
  }
  @Factory
  public static <T> Matcher<T> isLemmaOf(final WordSense operand) {
    return new HasLemma<T>(operand);
  }
}
