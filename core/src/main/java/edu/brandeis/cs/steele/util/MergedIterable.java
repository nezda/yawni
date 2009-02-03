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
package edu.brandeis.cs.steele.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Derive a new <code>Iterable</code> by merge sorting the items
 * in each of the provided mutually <i>sortable</i> <code>Iterable</code>s
 * whose traversal order <i>is</i> sorted.
 */
public class MergedIterable<T extends Object & Comparable<? super T>> implements Iterable<T> {
  private static final long serialVersionUID = 1L;

  private final Iterable<T>[] bases;

  public MergedIterable(final Iterable<T>... bases) {
    this(false, bases);
  }

  public MergedIterable(final boolean validateSort, final Iterable<T>... bases) {
    if (bases.length == 0) {
      throw new IllegalArgumentException();
    }
    validateSort(validateSort, bases);
    //TODO at least warn if any of the given 'bases' are equal to one another
    this.bases = bases;
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
   * <code>Iterator</code>.  Look at the 'top' of each base
   * <code>Iterator</code> caching these <tt>n</tt> values.
   * <code>next()</code> will return the minimum of these values.
   *
   * <h4>Running Time</h4>
   * Let <tt>k</tt> be the number of <code>Iterable</code> sequences to merge
   * (which themselves are presented in sorted order).  Let <tt>n</tt> be the
   * sum of the number of items in all <tt>k</tt> sequences.  The running time
   * of this algorithm is <tt>O(n * k)</tt>.  Optimal running time would be
   * <tt>O(n * lg k)</tt> if the sequence heads were stored in a priority
   * queue which will not be beneficial for small <tt>k</tt> (e.g. <tt>k < 10</tt>).
   *
   * <h4>TODO</h4>
   * - support remove()
   */
  private static class MergedIterator<T extends Object & Comparable<? super T>> implements Iterator<T> {
    private static final long serialVersionUID = 1L;
    private static final Object SENTINEL = new Object();

    private final Iterator<T>[] bases;
    private final Object[] tops;

    MergedIterator(final Iterable<T>... bases) {
      this.bases = (Iterator<T>[]) new Iterator[bases.length];
      this.tops = new Object[bases.length];
      int i = -1;
      for (final Iterable<T> iterable : bases) {
        final Iterator<T> it = iterable.iterator();
        ++i;
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
            final Comparable minObj = (Comparable)tops[min];
            final Comparable currObj = (Comparable)tops[i];
            if(minObj.compareTo(currObj) > 0) {
              min = i;
            }
          }
        }
      }
      if (tops[min] == SENTINEL) {
        throw new NoSuchElementException();
      } else {
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
