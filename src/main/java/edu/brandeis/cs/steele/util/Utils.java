package edu.brandeis.cs.steele.util;

import java.util.List;
import java.util.Iterator;
import java.util.Comparator;

/** 
 */
public class Utils {
  private Utils() { }

  /**
   * @return <var>str</var> with its first letter {@link Character#toUpperCase}
   */
  public static String capitalize(final String str) {
    return Character.toUpperCase(str.charAt(0))+str.substring(1);
  }

  /**
   * Returns the absolute offset <i>into <var>l1</var></i> where the elements
   * of sequences <var>l1</var> and <var>l2</var> (with <var>l1</var> starting
   * at <var>l1s</var> and <var>l2</var> starting at <var>l2s</var>) are <u>first</u>
   * not <code>equals()</code> or <var>l1e</var> if no such offset exists.
   * Modeled after STL function by the same name, but assumes "random access iterators".
   */
  public static int mismatch(final List<?> l1, int l1s, final int l1e,
      final List<?> l2, int l2s) {
    while (l1s < l1e) {
      if (l2s >= l2.size() || false == equals(l1.get(l1s), l2.get(l2s))) {
        break;
      }
      l1s++; l2s++;
    }
    return l1s;
  }

  /** 
   * Equivalent to the STL function by the same name except the
   * <code>first</code> and <code>last</code> are implied to select the entire
   * contents of <param>iterable</param>.
   * @see <a href="http://www.sgi.com/tech/stl/distance.html">distane</a>
   */
  public static long distance(final Iterable<?> iterable) {
    long distance = 0;
    for(final Object obj : iterable) {
      distance++;
    }
    return distance;
  }

  /**
   * @param iterable generator of sequences to check for natural sortedness
   * @return whether or not the naturally <code>Comparable</code> elements of 
   * sequences emitted by <var>iterable</var> are produced in sorted order.
   */
  public static <T extends Object & Comparable<? super T>>
    boolean isSorted(final Iterable<? extends T> iterable) {
      return isSorted(iterable.iterator(), false);
    }

  /**
   * @param iterable generator of sequences to check for natural sortedness
   * @param infoException if <code>true</code>, throw informative <code>RuntimeException</code>
   * if <var>iterable</var> isn't sorted
   * @return whether or not the naturally <code>Comparable</code> elements of 
   * sequences emitted by <var>iterable</var> are produced in sorted order.
   */
  public static <T extends Object & Comparable<? super T>>
    boolean isSorted(final Iterable<? extends T> iterable, final boolean infoException) {
      return isSorted(iterable.iterator(), infoException);
    }

  /**
   * @param iterator sequence to check for natural sortedness
   * @return whether or not the naturally <code>Comparable</code> elements of 
   * the sequence emitted by <var>iterator</var> are produced in sorted order.
   */
  public static <T extends Object & Comparable<? super T>>
    boolean isSorted(final Iterator<? extends T> iterator) {
      return isSorted(iterator, false);
    }

  /**
   * @param iterator sequence to check for natural sortedness
   * @param infoException if <code>true</code>, throw informative <code>RuntimeException</code>
   * if <var>iterator</var> isn't sorted
   * @return whether or not the naturally <code>Comparable</code> elements
   * emitted by <var>iterator</var> are produced in sorted order.
   */
  public static <T extends Object & Comparable<? super T>>
    boolean isSorted(final Iterator<? extends T> iterator, final boolean infoException) {
      if (iterator.hasNext() ) {
        T prev = iterator.next();
        while (iterator.hasNext()) {
          final T curr = iterator.next();
          if (prev.compareTo(curr) > 0) {
            if (infoException) {
              final StringBuilder msg = new StringBuilder("Sort failure with prev: ").
                append(prev).append(" curr: ").append(curr);
              throw new IllegalArgumentException(msg.toString());
            }
            return false;
          }
          prev = curr;
        }
      }
      return true;
    }

  public enum UniqueMode {
    EQUALITY,
    EQUALS
  } // end enum UniqueMode

  private static final UniqueMode DEFAULT_UNIQUE_MODE = UniqueMode.EQUALITY;

  public static <T extends Object & Comparable<? super T>>
    boolean isUnique(final Iterable<? extends T> iterable, final boolean infoException) {
      return isUnique(iterable.iterator(), infoException, DEFAULT_UNIQUE_MODE);
    }
  
  /**
   *TODO
   * Make versions of isSorted (IsSorted) and isUnique (IsUnique) that are
   * <code>Iterator</code>/<code>Iterable</code> decorators which pass their type-signature through.  They should
   * do nothing but verify that their constraint is met and throw an informative
   * <code>RuntimeException</code> if it is not.
   * IsUnique can optionally <i>make</i> a sorted sequence unique.
   * IsUnique should be able to make its decision based on <code>equals()</code> or <code>compareTo() == 0</code>
   * although there could be discontinuous violations that it can't detect with exotic/wrong 
   * <code>Comparable</code>/<code>Comparator</code>s -- add an enum.
   */

  public static <T extends Object & Comparable<? super T>>
    boolean isUnique(final Iterator<? extends T> iterator, final boolean infoException, final UniqueMode uniqueMode) {
      if (iterator.hasNext() ) {
        T prev = iterator.next();
        while (iterator.hasNext()) {
          final T curr = iterator.next();
          final boolean constraintViolated;
          switch (uniqueMode) {
            case EQUALITY: 
              constraintViolated =  prev.compareTo(curr) == 0;
              break;
            case EQUALS:
              constraintViolated = prev.equals(curr);
              break;
            default:
              throw new IllegalArgumentException("unknown UniqueMode "+uniqueMode);
          }
          if (constraintViolated) {
            if (infoException) {
              final StringBuilder msg = new StringBuilder("isUnique ").
                append("(").append(uniqueMode).append(" mode) failure with prev: ").
                append(prev).append(" curr: ").append(curr);
              //FIXME 
              throw new IllegalArgumentException(msg.toString());
              //XXX System.err.println(msg);
              //XXX new Exception().printStackTrace();
            }
            //FIXME return false;
            return false;
          }
          //XXX System.err.println(curr);
          prev = curr;
        }
      }
      return true;
    }

  /**
   * Removes duplicates from <param>list</param>.  Does not require
   * <param>list</param> be sorted but does assume <param>list</param>
   * contains no null elements and is short (brute force algorithm).
   */
  public static <T> List<T> uniq(List<T> list) {
    //log.warning("input list: "+list+" list.size(): "+list.size());
    if (list == null || list.size() <= 1) {
      return list;
    }
    //TODO if list size > x, use a HashSet<T> or a 
    // sort/uniq strategy
    int n = list.size();
    for (int i = 0; i < n; i++) {
      final T ith = list.get(i);
      for (int j = i + 1; j < n; j++) {
        final T jth = list.get(j);
        if (ith.equals(jth)) {
          // overwrite jth with n-1th and decrement n and j
          list.set(j, list.get(n - 1));
          n--;
          j--;
        }
      }
    }
    if (n < list.size()) {
      //TODO new list would plug small memory leak here
      list = list.subList(0, n);
    }
    //final Set<T> set = new HashSet<T>(list);
    //log.warning("output list: "+list+" list.size(): "+list.size());
    return list;
  }

  /**
   * <code>null</code>-tolerant version of {@link Object#equals}
   */
  public static boolean equals(final Object o1, final Object o2) {
    return o1 == o2 || (o1 != null && o1.equals(o2));
  }

  public static class WordNetLexicalComparator implements Comparator<CharSequence> {
    // for use with Word lemmas which are all lowercased
    public static final WordNetLexicalComparator GIVEN_CASE_INSTANCE = new WordNetLexicalComparator(false);
    // for use with WordSense lemmas which include natural case information
    public static final WordNetLexicalComparator TO_LOWERCASE_INSTANCE = new WordNetLexicalComparator(true);

    private final boolean lowerCase;
    private WordNetLexicalComparator(final boolean lowerCase) {
      this.lowerCase = lowerCase;
    }

    public int compare(final CharSequence s1, final CharSequence s2) {
      final int s1Len = s1.length();
      final int s2Len = s2.length();

      int o1 = 0, o2 = 0, result;
      int end = s1Len < s2Len ? s1Len : s2Len;
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

    public boolean equals(Object that) {
      // this is a singleton
      return this == that;
    }
  }

}
