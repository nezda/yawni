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
 * Derive a new {@code Iterable} by removing adjacent duplicates
 * of the provided <em>sortable</em> {@code Iterable}>s.
 *
 * <p> Much like the STL version below:<br>
 * {@code std::unique_copy(c.begin(), c.end(),
 *   std::ostream_iterator<c::type>(std::cout, " "));}
 *
 * <p> TODO add support for {@link java.util.Comparator}s
 */
final class Uniq<T extends Object & Comparable<? super T>> implements Iterable<T> {
  private static final long serialVersionUID = 1L;

  private final Iterable<T> base;

  Uniq(final Iterable<T> base) {
    this(false, base);
  }

  Uniq(final boolean validateSort, final Iterable<T> base) {
    validateSort(validateSort, base);
    this.base = base;
  }

  @Override
  public Iterator<T> iterator() {
    return new UniqIterator<>(base);
  }

  private void validateSort(final boolean validateSort, final Iterable<T> base) {
    if (! validateSort) {
      return;
    }
    if (! Utils.isSorted(base, true)) {
      final StringBuilder msg = new StringBuilder("Iterable ").
        append(base).append(" violates sort criteria.");
      throw new IllegalArgumentException(msg.toString());
    }
  }

  /**
   * TODO: - support remove()
   */
  private static class UniqIterator<T extends Object & Comparable<? super T>> extends UnmodifiableIterator<T> {
    private static final long serialVersionUID = 1L;
    private static final Object SENTINEL = new Object();

    private final Iterator<T> base;
    private Object top;

    UniqIterator(final Iterable<T> iterable) {
      this.base = iterable.iterator();
      if (this.base.hasNext()) {
        this.top = base.next();
      } else {
        this.top = SENTINEL;
      }
    }
    @Override
    public T next() {
      if (top == SENTINEL) {
        throw new NoSuchElementException();
      } else {
        // store curr value of top
        // find next value of top discarding any that are equal to currTop
        // or setting top to SENTINEL to mark eos
        @SuppressWarnings("unchecked")
        T currTop = (T)top;
        Object nextTop = SENTINEL;
        while (base.hasNext()) {
          final T next = base.next();
          if (currTop.compareTo(next) == 0) {
            continue;
          } else {
            nextTop = next;
            break;
          }
        }
        top = nextTop;
        return currTop;
      }
    }
    // user's don't have to explicitly call this although that's a bit crazy
		@Override
    public boolean hasNext() {
      return top != SENTINEL;
    }
  } // end class UniqIterator
}