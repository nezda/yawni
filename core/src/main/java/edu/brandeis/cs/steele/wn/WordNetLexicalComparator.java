package edu.brandeis.cs.steele.wn;

import java.util.Comparator;

/**
 * {@link Comparator} for {@code CharSequence}s which considers {@code ' '}
 * and {@code '_'} the same and optionally lowercases the characters of both
 * arguments to {@link #compare(java.lang.CharSequence, java.lang.CharSequence) compare()}
 * ({@link #TO_LOWERCASE_INSTANCE}).  This encodes the natural sort order of
 * the {@link Word}s of WordNet <code>index.<em>pos</em></code> files.
 */
public final class WordNetLexicalComparator implements Comparator<CharSequence> {
  /**
   * {@code WordNetLexicalComparator} for use with {@code Word} lemmas which are all already lowercased.
   */
  public static final WordNetLexicalComparator GIVEN_CASE_INSTANCE = new WordNetLexicalComparator(false);
  /**
   * {@code WordNetLexicalComparator} for use with {@code WordSense} lemmas which include natural case information which should
   * be normalized as part of the comparison.
   */
  public static final WordNetLexicalComparator TO_LOWERCASE_INSTANCE = new WordNetLexicalComparator(true);
  private final boolean lowerCase;

  private WordNetLexicalComparator(final boolean lowerCase) {
    super();
    this.lowerCase = lowerCase;
  }

  /**
   * {@inheritDoc}
   */
  public int compare(final CharSequence s1, final CharSequence s2) {
    final int s1Len = s1.length();
    final int s2Len = s2.length();
    int o1 = 0;
    int o2 = 0;
    int result;
    final int end = s1Len < s2Len ? s1Len : s2Len;
    char c1;
    char c2;
    while (o1 < end) {
      c1 = s1.charAt(o1++);
      c2 = s2.charAt(o2++);
      c1 = c1 == ' ' ? '_' : c1;
      c2 = c2 == ' ' ? '_' : c2;
      if (c1 == c2) {
        continue;
      }
      if (lowerCase) {
        c1 = Character.toLowerCase(c1);
        c2 = Character.toLowerCase(c2);
      }
      if ((result = c1 - c2) != 0) {
        return result;
      }
    }
    return s1Len - s2Len;
  }

  @Override
  public boolean equals(Object that) {
    // this is a singleton
    return this == that;
  }
}
