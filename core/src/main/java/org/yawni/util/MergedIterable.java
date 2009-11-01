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

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Derive a new {@code Iterable} by merge sorting the items
 * in each of the provided <em>mutually sortable</em> {@code Iterable}s
 * whose traversal order <em>is</em> sorted.
 */
public final class MergedIterable<T extends Object & Comparable<? super T>> implements Iterable<T> {
  private static final long serialVersionUID = 1L;

  public static <T extends Object & Comparable<? super T>>
    Iterable<T> merge(final Iterable<T> base0, final Iterable<T> base1) {
      @SuppressWarnings("unchecked")
      final Iterable<T>[] bases = (Iterable<T>[]) new Iterable[] { base0, base1 };
      return merge(false, bases);
  }

  public static <T extends Object & Comparable<? super T>>
    Iterable<T> merge(final Iterable<T> base0, final Iterable<T> base1, final Iterable<T> base2) {
      @SuppressWarnings("unchecked")
      final Iterable<T>[] bases = (Iterable<T>[]) new Iterable[] { base0, base1, base2 };
      return merge(false, bases);
  }

  public static <T extends Object & Comparable<? super T>>
    Iterable<T> merge(final Iterable<T> base0, final Iterable<T> base1,
                      final Iterable<T> base2, final Iterable<T> base3) {
      @SuppressWarnings("unchecked")
      final Iterable<T>[] bases = (Iterable<T>[]) new Iterable[] { base0, base1, base2, base3 };
      return merge(false, bases);
  }

  public static <T extends Object & Comparable<? super T>>
    Iterable<T> merge(final Iterable<T> base0, final Iterable<T> base1,
                      final Iterable<T> base2, final Iterable<T> base3,
                      final Iterable<T> base4) {
      @SuppressWarnings("unchecked")
      final Iterable<T>[] bases = (Iterable<T>[]) new Iterable[] { base0, base1, base2, base3, base4 };
      return merge(false, bases);
  }

  public static <T extends Object & Comparable<? super T>>
    Iterable<T> merge(final boolean validateSort, final Iterable<T> base0, final Iterable<T> base1) {
      @SuppressWarnings("unchecked")
      final Iterable<T>[] bases = (Iterable<T>[]) new Iterable[] { base0, base1 };
      return merge(validateSort, bases);
  }

  public static <T extends Object & Comparable<? super T>>
    Iterable<T> merge(final boolean validateSort,
                      final Iterable<T> base0, final Iterable<T> base1, final Iterable<T> base2) {
      @SuppressWarnings("unchecked")
      final Iterable<T>[] bases = (Iterable<T>[]) new Iterable[] { base0, base1, base2 };
      return merge(validateSort, bases);
  }

  public static <T extends Object & Comparable<? super T>>
    Iterable<T> merge(final boolean validateSort,
                      final Iterable<T> base0, final Iterable<T> base1,
                      final Iterable<T> base2, final Iterable<T> base3) {
      @SuppressWarnings("unchecked")
      final Iterable<T>[] bases = (Iterable<T>[]) new Iterable[] { base0, base1, base2, base3 };
      return merge(validateSort, bases);
  }

  public static <T extends Object & Comparable<? super T>>
    Iterable<T> merge(final boolean validateSort,
                      final Iterable<T> base0, final Iterable<T> base1,
                      final Iterable<T> base2, final Iterable<T> base3,
                      final Iterable<T> base4) {
      @SuppressWarnings("unchecked")
      final Iterable<T>[] bases = (Iterable<T>[]) new Iterable[] { base0, base1, base2, base3, base4 };
      return merge(validateSort, bases);
  }

  /** Primary factory method so template parameters are deduced. */
  public static <T extends Object & Comparable<? super T>>
    Iterable<T> merge(final Iterable<T>... bases) {
      return merge(false, bases);
  }

  /** Validating factory method so template parameters are deduced. */
  public static <T extends Object & Comparable<? super T>>
    Iterable<T> merge(final boolean validateSort, final Iterable<T>... bases) {
      return new MergedIterable<T>(validateSort, bases);
  }

  private final Iterable<T>[] bases;

  MergedIterable(final Iterable<T>... bases) {
    this(false, bases);
  }

  MergedIterable(final boolean validateSort, final Iterable<T>... bases) {
    if (bases.length == 0) {
      throw new IllegalArgumentException();
    }
    validateSort(validateSort, bases);
    //TODO at least warn if any of the given 'bases' are equal to one another
    this.bases = bases;
  }

  /** {@inheritDoc} */
  public Iterator<T> iterator() {
    return new MergedIterator<T>(bases);
  }

  private void validateSort(final boolean validateSort, final Iterable<T>... bases) {
    if (false == validateSort) {
      return;
    }
    for (final Iterable<T> iterable : bases) {
      if (Utils.isSorted(iterable, true) == false) {
        final StringBuilder msg = new StringBuilder("Iterable ").
          append(iterable).append(" violates sort criteria.");
        throw new IllegalArgumentException(msg.toString());
      }
    }
  }

  /**
   * Implements simple k-way merge sort logic and presents logic as an
   * {@code Iterator}.  Look at the 'top' of each base
   * {@code Iterator} caching these <tt>n</tt> values.
   * {@code next()} will return the minimum of these values.
   *
   * <h4>Running Time</h4>
   * Let <tt>k</tt> be the number of {@code Iterable} sequences to merge
   * (which themselves are presented in sorted order).  Let <tt>n</tt> be the
   * sum of the number of items in all <tt>k</tt> sequences.  The running time
   * of this algorithm is {@code O(n * k)}.  Optimal running time would be
   * {@code O(n * lg k)} if the sequence heads were stored in a priority
   * queue which will not be beneficial for small <tt>k</tt> (e.g., {@code k < 10}).
   *
   * TODO - support remove()
   */
  private static class MergedIterator<T extends Object & Comparable<? super T>> implements Iterator<T> {
    private static final long serialVersionUID = 1L;
    private static final Object SENTINEL = new Object();

    private final Iterator<T>[] bases;
    // really T's except for the SENTINEL which forces us to
    // use Object[]
    private final Object[] tops;

    @SuppressWarnings("unchecked")
    MergedIterator(final Iterable<T>... bases) {
      this.bases = (Iterator<T>[]) new Iterator[bases.length];
      this.tops = new Object[bases.length];
      int i = -1;
      for (final Iterable<T> iterable : bases) {
        final Iterator<T> it = iterable.iterator();
        i++;
        this.bases[i] = it;
        if (it.hasNext()) {
          tops[i] = it.next();
        } else {
          tops[i] = SENTINEL;
        }
      }
    }
    
    /** {@inheritDoc} */
    public T next() {
      // find min() of non-SENTINEL tops, tag its Iterator again, and return that
      // min item
      int min = 0;
      for (int i=1; i < tops.length; i++) {
        if (tops[i] != SENTINEL) {
          if (tops[min] == SENTINEL) {
            min = i;
          } else {
            @SuppressWarnings("unchecked") // only SENTINEL is not a T
            final T minObj = (T)tops[min];
            @SuppressWarnings("unchecked")  // only SENTINEL is not a T
            final T currObj = (T)tops[i];
            if (minObj.compareTo(currObj) > 0) {
              min = i;
            }
          }
        }
      }
      if (tops[min] == SENTINEL) {
        throw new NoSuchElementException();
      } else {
        @SuppressWarnings("unchecked") // only SENTINEL is not a T
        final T t = (T)tops[min];
        if (bases[min].hasNext()) {
          tops[min] = bases[min].next();
        } else {
          tops[min] = SENTINEL;
        }
        return t;
      }
    }
    
    /** {@inheritDoc} */
    // user's don't have to explicitly call this although that's a bit crazy
    public boolean hasNext() {
      // at least one of tops is != SENTINEL
      for (final Object obj : tops) {
        if (obj != SENTINEL) {
          return true;
        }
      }
      return false;
    }
    
    /** {@inheritDoc} */
    public void remove() {
      throw new UnsupportedOperationException();
      // this is call refers to the last iterator which returned an item via next()
    }
  } // end class MergedIterator
}