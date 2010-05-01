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

import com.google.common.collect.UnmodifiableIterator;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Derive a new {@code Iterable} by concatenating the items
 * in each of the provided {@code Iterable}s.
 * @yawni.internal
 */
public final class ConcatenatedIterable<T> implements Iterable<T> {
  private static final long serialVersionUID = 1L;

  public static <T> Iterable<T> concat(final Iterable<T> base0, final Iterable<T> base1) {
    @SuppressWarnings("unchecked")
    final Iterable<T>[] bases = (Iterable<T>[]) new Iterable[]{base0, base1};
    return concat(bases);
  }

  public static <T>
    Iterable<T> concat(final Iterable<T> base0, final Iterable<T> base1, final Iterable<T> base2) {
      @SuppressWarnings("unchecked")
      final Iterable<T>[] bases = (Iterable<T>[]) new Iterable[] { base0, base1, base2 };
      return concat(bases);
  }

  public static <T>
    Iterable<T> concat(final Iterable<T> base0, final Iterable<T> base1,
                       final Iterable<T> base2, final Iterable<T> base3) {
      @SuppressWarnings("unchecked")
      final Iterable<T>[] bases = (Iterable<T>[]) new Iterable[] { base0, base1, base2, base3 };
      return concat(bases);
  }

  public static <T>
    Iterable<T> concat(final Iterable<T> base0, final Iterable<T> base1,
                       final Iterable<T> base2, final Iterable<T> base3,
                       final Iterable<T> base4) {
      @SuppressWarnings("unchecked")
      final Iterable<T>[] bases = (Iterable<T>[]) new Iterable[] { base0, base1, base2, base3, base4 };
      return concat(bases);
  }

  /** Primary factory method so template parameters are deduced. */
  public static <T> Iterable<T> concat(final Iterable<T>... bases) {
    return new ConcatenatedIterable<T>(bases);
  }

  private final Iterable<T>[] bases;

  ConcatenatedIterable(final Iterable<T>... bases) {
    if (bases.length == 0) {
      throw new IllegalArgumentException();
    }
    //TODO at least warn if any of the given 'bases' are equal to one another
    this.bases = bases;
  }

  /** {@inheritDoc} */
  public Iterator<T> iterator() {
    return new ConcatenatedIterator<T>(bases);
  }

  /**
   * Implements simple concat logic and presents logic as an
   * {@code Iterator}.
   *
   * TODO: - support remove()
   */
  private static class ConcatenatedIterator<T> extends UnmodifiableIterator<T> {
    private static final long serialVersionUID = 1L;
    private static final Object SENTINEL = new Object();

    private final Iterator<T>[] bases;
    // really T's except for the SENTINEL which forces us to
    // use Object[]
    private final Object[] tops;

    @SuppressWarnings("unchecked")
    ConcatenatedIterator(final Iterable<T>... bases) {
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
      // find first non-SENTINEL in tops
      for (int i = 0; i < tops.length; i++) {
        if (tops[i] != SENTINEL) {
          @SuppressWarnings("unchecked") // only SENTINEL is not a T
          final T t = (T)tops[i];
          if (bases[i].hasNext()) {
            tops[i] = bases[i].next();
          } else {
            tops[i] = SENTINEL;
          }
          return t;
        }
      }
      throw new NoSuchElementException();
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
  } // end class ConcatenatedIterator
}