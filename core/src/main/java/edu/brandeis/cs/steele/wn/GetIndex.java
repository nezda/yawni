package edu.brandeis.cs.steele.wn;

import java.util.*;

/**
 * Function object port of <code>getindex()</code> search variants function
 * in <code>search.c</code>.
 * TODO implement ListIterator<CharSequence>, ListIterator<Word>
 */
class GetIndex implements CharSequence, Iterable<CharSequence>, Iterator<CharSequence>, Comparable<CharSequence> {
  private List<Word> words;
  private int offset;
  private final String base;
  private final POS pos;
  private final Morphy morphy;

  GetIndex(final String base, final POS pos, final Morphy morphy) {
    this.base = base;
    this.pos = pos == POS.SAT_ADJ ? POS.ADJ : pos;
    this.morphy = morphy;
    this.words = new ArrayList<Word>();
    getindex(base, pos);
  }
  // general plan
  // * offset is the step we're in in the set of transformations to undergo
  // * method advance() will mutate the outward appearance of this object
  //   to the next state
  public char charAt(int i) {
    throw new UnsupportedOperationException();
  }
  public int length() {
    throw new UnsupportedOperationException();
  }
  public CharSequence subSequence(final int s, final int e) {
    throw new UnsupportedOperationException();
  }
  public Iterator<CharSequence> iterator() {
    return new GetIndex(base, pos, morphy);
  }
  public boolean hasNext() {
    throw new UnsupportedOperationException();
  }
  public CharSequence next() {
    throw new UnsupportedOperationException();
  }
  public void remove() {
    throw new UnsupportedOperationException();
  }
  public int compareTo(final CharSequence that) {
    throw new UnsupportedOperationException();
  }
  // note this is part of the CharSequence interface
  public String toString() {
    throw new UnsupportedOperationException();
  }
  public boolean equals() {
    throw new UnsupportedOperationException();
  }
  public int hashCode() {
    throw new UnsupportedOperationException();
  }

  static int alternate(String searchStr) {
    // http://en.wikipedia.org/wiki/Combination (number of cobinations with repetitions)
    // - count spaces & dashes (k)
    // - generate alternatives with minimal differences using
    //   Gray code single-change strategy
    //   - every combo of size k of space and dash (n=2) should be tried
    //   - (n + k - 1) choose (n - 1)
    // * these 32 sequences (limited by number of bits in int and how long a collocation we want to
    //   try exhaustive alternatives for) can be cached
    // Combinadic: ordered interger partition or composition
    //
    // number of comnbinations: 2^n - 1
    //
    // XXX new notes
    // - there will be 2^n total variants where -- 2 because |{'-','_'}| == 2
    // ? implementation should be straight-forward ?

    final StringBuilder buffer = new StringBuilder(searchStr);

    // a_b
    //  1 3
 
    // x_y_z
    //  1 3

    
    int numAlternations = 0;
    for(int i = next(searchStr, 0);
      i >= 0;
      i = next(searchStr, i + 1)) {
      for(int j = next(searchStr, i + 1);
          j >= 0;
          j = next(searchStr, j + 1)) {
        //System.err.println("i: "+i+" j: "+j);
        buffer.setCharAt(i, toggle(buffer, i));
        System.err.println("buffer: "+buffer);
        numAlternations++;
        buffer.setCharAt(j, toggle(buffer, j));
        System.err.println("buffer: "+buffer);
        numAlternations++;
        buffer.setCharAt(j, toggle(buffer, j));

        //if (i >= j) {
        //  break;
        //}

        buffer.setCharAt(i, toggle(buffer, i));
        System.err.println("buffer: "+buffer);
        numAlternations++;
        buffer.setCharAt(j, toggle(buffer, j));
        System.err.println("buffer: "+buffer);
        numAlternations++;
        buffer.setCharAt(j, toggle(buffer, j));
      }
    }
    return numAlternations;
  }

  static char toggle(CharSequence str, int i) {
    if(str.charAt(i) == '_') {
      return '-';
    } else if(str.charAt(i) == '-') {
      return '_';
    } else {
      throw new IllegalStateException("str: "+str+" str["+i+"]: "+str.charAt(i));
    }
  }

  static int next(String str, int i) {
    int s = str.indexOf("_", i);
    int h = str.indexOf("-", i);
    if(s < 0) {
      return h;
    } else if(h < 0) {
      return s;
    } else {
      return Math.min(s, h);
    }
  }

  /**
   * 'Smart' search of WordNet index file.  Attempts to find word in index file
   * by trying different transformations including:
   * <ul>
   *   <li> replace hyphens with underscores (spaces) </li>
   *   <li> replace underscores (spaces) with hyphens </li>
   *   <li> strip hyphens and underscores (spaces) XXX dangerous to just strip spaces </li>
   *   <li> strip periods </li>
   * </ul>
   * ??? Typically this operates on the output(s) of <code>morphstr()</code>.
   *
   * <p>Port of <code>search.c</code>'s function <code>getindex()</code> function.
   *
   * TODO
   * - add periods to all upper searchstr's
   * - ...
   * - see commented test cases in {@link MorphyTest}
   */
  private void getindex(String searchstr, POS pos) {
    // typical input:
    // needs "-" -> ""  { POS.NOUN, "fire-bomb",   "firebomb" }, // WN does this
    // needs "-" -> " " { POS.NOUN, "letter-bomb", "letter bomb" }, // WN does do this
    // needs "" -> " "  { POS.NOUN, "letterbomb", "letter bomb" }, // WN doesn't do this
    // - requires prefix AND suffix match or forgiving match -- slippery slope "I ran" -> "Iran"
    // needs "" -> "." AND "X.X." -> "X. X." { "FD Roosevelt", "F.D. Roosevelt", "F. D. Roosevelt"} // WN doesn't do this

    //FIXME this strategy fails for special 3+ word
    // collocations like "wild-goose chase" and "internal-combustion engine"
    // when the query has no dashes (common).
    //
    // better strategy:
    // - try variants where each underscore (space) switched to
    //   a dash and each dash is switched to an underscore (space)
    //   in turn to create ALL possible variants
    //   - numDashes * numUnderscores variants
    //  * def do "X.[^_]" -> "X. " (e.g. "F.D." -> "F. D.")
    //  * consider similar for periods so "U.S.A." produces "U.S.A"
    // ? consider implementing algorithm as a series of modifcations of
    //   a single StringBuilder:
    //   * make mod, issue search (morphy.is_defined()) and store result

    //FIXME short circuit this if searchstr contains
    //no underscores (spaces), dashes, or periods as this
    //algorithm will do nothing
    //FIXME FIXME not true for insertion-variants
    final int firstUnderscore = searchstr.indexOf('_');
    final int firstDash = searchstr.indexOf('-');
    final int firstPeriod = searchstr.indexOf('.');
    if (firstUnderscore < 0 && firstDash < 0 && firstPeriod < 0) {
      return;
    }

    // vector of search strings
    final int MAX_FORMS = 5;
    final String[] strings = new String[MAX_FORMS];

    searchstr = searchstr.toLowerCase();
    for (int i = 0; i < MAX_FORMS; i++) {
      strings[i] = searchstr;
    }
    // [0] is original string (lowercased)

    // [1] is ALL underscores (spaces) to dashes
    strings[1] = strings[1].replace('_', '-');
    // [2] is ALL dashes to underscores (spaces)
    strings[2] = strings[2].replace('-', '_');

    // remove ALL spaces AND hyphens from search string
    // [3] is no underscores (spaces) or dashes
    //FIXME this strategy is a little crazy
    // * only allow this if a version with dashes exists ?
    strings[3] = strings[3].replace("_", "").replace("-", "");
    // remove ALL periods
    // [4] is no periods
    strings[4] = strings[4].replace(".", "");

    int j = -1;
    for (final String s : strings) {
      System.err.println("s["+(++j)+"]: "+s);
    }

    // Get offset of first entry.  Then eliminate duplicates
    // and get offsets of unique strings.

    Word word = morphy.is_defined(strings[0], pos);
    if (word != null) {
      words.add(word);
    }

    for (int i = 1; i < MAX_FORMS; i++) {
      if (strings[i] == null || strings[0].equals(strings[i])) {
        continue;
      }
      word = morphy.is_defined(strings[i], pos);
      if (word != null) {
        words.add(word);
      }
    }
    // cannot return duplicate Word since each is tied a specific String
  }
}
