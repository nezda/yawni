package org.yawni.wn;

import java.util.Comparator;
import org.yawni.util.cache.Hasher;

/**
 * {@link Comparator} for {@code CharSequence}s which considers {@code ' '}
 * and {@code '_'} the same and optionally lowercases the characters of both
 * arguments to {@link #compare(java.lang.CharSequence, java.lang.CharSequence) compare()}
 * ({@link #TO_LOWERCASE_INSTANCE}).  This encodes the natural sort order of
 * the {@link Word}s of WordNet <code>index.<em>pos</em></code> files.
 */
public final class WordNetLexicalComparator implements Comparator<CharSequence>, Hasher<CharSequence> {
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
    int result, o1, o2;
    o1 = o2 = 0;
    final int end = s1Len < s2Len ? s1Len : s2Len;
    char c1, c2;
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

  public int hashCode(final Object obj) {
    if (!(obj instanceof CharSequence)) {
      return obj.hashCode();
    }
    final CharSequence seq = (CharSequence) obj;
    int hash = 0, multiplier = 1;
    for (int i = seq.length() - 1; i >= 0; i--) {
      char c = Character.toLowerCase(seq.charAt(i));
      c = c == ' ' ? '_' : c;
      hash += c * multiplier;
      int shifted = multiplier << 5;
      multiplier = shifted - multiplier;
    }
    return hash;
  }

  @Override
  public boolean equals(Object that) {
    // this is a singleton
    return this == that;
  }
}