/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.yawni.util;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Iterator;
import java.util.Objects;

/**
 * Predicates and functions for {@code Iterable}s and {@code List}s.
 */
public class Utils {
  private Utils() { }

  /**
   * @return {@code str} with its first letter {@link Character#toUpperCase}
   * TODO move to CharSequences ?
   */
  public static String capitalize(final CharSequence str) {
    return Character.toUpperCase(str.charAt(0)) + str.subSequence(1, str.length()).toString();
  }

  /**
   * Returns the absolute offset <em>into {@code l1}</em> where the elements
   * of sequences {@code l1} and {@code l2} (with {@code l1} starting
   * at {@code l1s} and {@code l2} starting at {@code l2s}) are <u>first</u>
   * not {@code equals()} or {@code l1e} if no such offset exists.
   *
   * <p> Modeled after C++ STL <a href="http://www.sgi.com/tech/stl/mismatch.html">mismatch</a>,
   * but assumes "random access iterators".
   */
  public static int mismatch(final List<?> l1, int l1s, final int l1e,
      final List<?> l2, int l2s) {
    while (l1s < l1e) {
      if (l2s >= l2.size() || ! Objects.equals(l1.get(l1s), l2.get(l2s))) {
        break;
      }
      l1s++; l2s++;
    }
    return l1s;
  }

  /**
   * @param iterable generator of sequences to check for natural sortedness
   * @return whether or not the naturally {@code Comparable} elements of
   * sequences emitted by {@code iterable} are produced in sorted order.
   */
  public static <T extends Object & Comparable<? super T>>
    boolean isSorted(final Iterable<? extends T> iterable) {
      return isSorted(iterable.iterator(), false);
    }

  /**
   * @param iterable generator of sequences to check for natural sortedness
   * @param infoException if {@code true}, throw informative {@code RuntimeException}
   * if {@code iterable} isn't sorted
   * @return whether or not the naturally {@code Comparable} elements of
   * sequences emitted by {@code iterable} are produced in sorted order.
   */
  public static <T extends Object & Comparable<? super T>>
    boolean isSorted(final Iterable<? extends T> iterable, final boolean infoException) {
      return isSorted(iterable.iterator(), infoException);
    }

  /**
   * @param iterator sequence to check for natural sortedness
   * @return whether or not the naturally {@code Comparable} elements of
   * the sequence emitted by {@code iterator} are produced in sorted order.
   */
  public static <T extends Object & Comparable<? super T>>
    boolean isSorted(final Iterator<? extends T> iterator) {
      return isSorted(iterator, false);
    }

  /**
   * @param iterator sequence to check for natural sortedness
   * @param infoException if {@code true}, throw informative {@code RuntimeException}
   * if {@code iterator} isn't sorted
   * @return whether or not the naturally {@code Comparable} elements
   * emitted by {@code iterator} are produced in sorted order.
   * @throws IllegalArgumentException with informative message if {@code infoException}
   * and <em>not</em> sorted.
   */
	// Ordering.isOrdered does all but infoException
  public static <T extends Object & Comparable<? super T>>
    boolean isSorted(final Iterator<? extends T> iterator, final boolean infoException) {
      if (iterator.hasNext()) {
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

  /**
   * Determines how pairs of items are determined to be distinct / interchangable.
   */
  public enum UniqueMode {
    /** Equal with respect to {@link Object#equals(java.lang.Object)} */
    EQUALS,
    /**
     * Only references to the same {@code Object} are the same.
     * Not yet implemented.
     */
    IDENTITY,
    /** Equivalent with respect to some {@link java.util.Comparator}. */
    EQUIVALENT,
  } // end enum UniqueMode

// erasure causes ambiguity
//  /**
//   * Note: relies on equals and hashCode being correct.
//   */
//  public static <T> boolean isUnique(final Collection<T> items) {
//    return isUnique(items, UniqueMode.EQUALS);
//  }
//
//  public static <T> boolean isUnique(final Collection<T> items, final UniqueMode mode) {
//    switch (mode) {
//      case EQUALS:
//        // don't optimize with (items instanceof Set ||) because this has a different meaning
//        return items.size() == new HashSet<T>(items).size();
//      //case IDENTITY: return items.size() == new IdentityHashSet<T>(items).size();
//      default: throw new UnsupportedOperationException("Unsupported mode "+mode);
//    }
//  }

  private static final UniqueMode DEFAULT_UNIQUE_MODE = UniqueMode.EQUIVALENT;

  public static <T extends Object & Comparable<? super T>>
    boolean isUnique(final Iterable<? extends T> iterable) {
      return isUnique(iterable.iterator(), false, DEFAULT_UNIQUE_MODE);
    }

  public static <T extends Object & Comparable<? super T>>
    boolean isUnique(final Iterable<? extends T> iterable, final boolean infoException) {
      return isUnique(iterable.iterator(), infoException, DEFAULT_UNIQUE_MODE);
    }

  /**
   *TODO
   * Make versions of isSorted (IsSorted) and isUnique (IsUnique) that are
   * {@code Iterator}/{@code Iterable} decorators which pass their type-signature through.  They should
   * do nothing but verify that their constraint is met and throw an informative
   * {@code RuntimeException} if it is not.
   * IsUnique can optionally <i>make</i> a sorted sequence unique.
   * IsUnique should be able to make its decision based on {@code equals()} or {@code compareTo() == 0}
   * although there could be discontinuous violations that it can't detect with exotic/wrong
   * {@code Comparable}/{@code Comparator}s -- add an enum.
   */

  /**
   * @param iterator sequence to inspect
   * @param infoException if {@code true}, throws a descriptive exception if the sequence contains adjacent duplicates.
   * @param uniqueMode
   * @return {@code true} if {@code iterator} contains no adjacent duplicates, as determined by
   * the {@code uniqueMode}.
   * @throws IllegalArgumentException with informative message if {@code infoException}
   * and contains adjacent duplicates.
   */
  public static <T extends Object & Comparable<? super T>>
    boolean isUnique(final Iterator<? extends T> iterator, final boolean infoException, final UniqueMode uniqueMode) {
      if (iterator.hasNext()) {
        T prev = iterator.next();
        while (iterator.hasNext()) {
          final T curr = iterator.next();
          final boolean constraintViolated;
          switch (uniqueMode) {
            case EQUIVALENT:
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
              throw new IllegalArgumentException(msg.toString());
            }
            return false;
          }
          //XXX System.err.println(curr);
          prev = curr;
        }
      }
      return true;
    }

  /**
   * Removes adjacent duplicates from {@code list}.  <b>Assumes</b>
   * {@code iterable}'s items are emitted in sorted order.
   */
  public static <T extends Object & Comparable<? super T>>
    Iterable<T> uniq(final Iterable<T> base) {
      return uniq(false, base);
  }

  /**
   * Validating form of {@link Utils#uniq(java.lang.Iterable)}.
   */
  public static <T extends Object & Comparable<? super T>>
    Iterable<T> uniq(final boolean validateSort, final Iterable<T> base) {
      return new Uniq<>(validateSort, base);
  }

  /**
   * Removes duplicates from {@code list}.  Does <em>not</em> require
   * {@code list} be sorted but does assume {@code list}
   * contains no {@code null} elements and is "short" (brute force algorithm).
   */
  public static <T> List<T> dedup(List<T> list) {
    //log.warn("input list: "+list+" list.size(): "+list.size());
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
      //TODO new List would plug small memory leak here
      list = list.subList(0, n);
    }
    //final Set<T> set = new HashSet<T>(list);
    //log.warn("output list: "+list+" list.size(): "+list.size());
    return list;
  }

  /**
   * @return the first item from {@code iterable} if it is not empty, or {@code null}
   */
//	@Deprecated // use com.google.common.collect.Iterables#getFirst
  public static <T> T first(final Iterable<T> iterable) {
		return Iterables.getFirst(iterable, null);
  }

  /**
   * @return the last item from {@code list} if it is not empty, or {@code null}
   */
	@Deprecated // use com.google.common.collect.Iterables.getLast
  public static <T> T last(final List<T> list) {
		return Iterables.getLast(list, null);
  }

  /**
   * Lazy initialization idiom for adding non-null {@code item} to
   * list, creating list if it is {@code null}.
   */
  public static <T> List<T> add(List<T> list, final T item) {
    if (item != null) {
      if (list == null || list == Collections.emptyList()) {
        list = Lists.newArrayList();
      }
      list.add(item);
    }
    return list;
  }

  /**
   * @param ints to {@link Arrays#hashCode(int[])} as varargs
   * @return a corresponding hash code
   */
  public static int hash(int... ints) {
    return Arrays.hashCode(ints);
  }
}