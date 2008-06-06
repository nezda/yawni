package edu.brandeis.cs.steele.util;

import java.util.List;

/** 
 */
public class Utils {
  private Utils() { }

  public static String capitalize(final String str) {
    return Character.toUpperCase(str.charAt(0))+str.substring(1);
  }

  /**
   * Returns the absolute offset <i>into <var>l1</var></i> where the elements
   * of sequences <var>l1</var> and <var>l2</var> (with <var>l1</var> starting
   * at <var>l1s</var> and <var>l2</var> starting at <var>l2s</var>) are first
   * not <code>equals()</code> or <var>l1e</var> if no such offset exists.
   * Modeled after STL function by the same name, but assumes "random access iterators".
   */
  public static int mismatch(final List<?> l1, int l1s, final int l1e,
      final List<?> l2, int l2s) {
    while (l1s < l1e) {
      try {
      if (l2s >= l2.size() || false == equals(l1.get(l1s), l2.get(l2s))) {
        break;
      }
      } catch(ArrayIndexOutOfBoundsException aioobe) {
        System.err.println("l1s: "+l1s+" l1e: "+l1e+" l2s: "+l2s+" l1: "+l1+" l2: "+l2);
        throw aioobe;
      }
      l1s++; l2s++;
    }
    return l1s;
  }

  public static boolean equals(final Object o1, final Object o2) {
    return o1 == o2 || (o1 != null && o1.equals(o2));
  }
}
