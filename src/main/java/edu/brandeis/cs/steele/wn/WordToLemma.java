package edu.brandeis.cs.steele.wn;

import edu.brandeis.cs.steele.util.MutatedIterable;

public class WordToLemma extends MutatedIterable<Word, String> {
  public WordToLemma(final Iterable<Word> iterable) {
    super(iterable);
  }
  @Override
  public String apply(final Word word) { 
    return word.getLemma(); 
  }
}
