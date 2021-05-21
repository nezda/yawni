/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.yawni.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;

import java.io.Serializable;
import java.util.Iterator;

/**
 * Derive a new {@code Iterable} whose traversal order is sorted by merge sorting the items
 * in each of the provided <em>mutually sortable</em> <em>sorted</em> {@code Iterable}s.
 *
 * <p> TODO add support for {@link java.util.Comparator}s
 * @yawni.internal
 */
public final class MergedIterable<T extends Object & Comparable<? super T>>
    implements Iterable<T>, Serializable {
  private static final long serialVersionUID = 1L;

  /** Primary factory method so template parameters are deduced. */
  @SafeVarargs
  public static <T extends Object & Comparable<? super T>>
    Iterable<T> merge(final Iterable<T>... bases) {
      return merge(false, ImmutableList.copyOf(bases));
  }

  /** Primary factory method so template parameters are deduced. */
  public static <T extends Object & Comparable<? super T>>
    Iterable<T> merge(final Iterable<Iterable<T>> bases) {
      return merge(false, ImmutableList.copyOf(bases));
  }

  /** Validating factory method so template parameters are deduced. */
  @SafeVarargs
  public static <T extends Object & Comparable<? super T>>
    Iterable<T> merge(final boolean validateSort, final Iterable<T>... bases) {
      return new MergedIterable<>(validateSort, ImmutableList.copyOf(bases));
  }

  /** Validating factory method so template parameters are deduced. */
  public static <T extends Object & Comparable<? super T>>
    Iterable<T> merge(final boolean validateSort, final ImmutableList<Iterable<T>> bases) {
      return new MergedIterable<>(validateSort, ImmutableList.copyOf(bases));
  }

  private final ImmutableList<Iterable<T>> bases;

  private MergedIterable(final boolean validateSort, final ImmutableList<Iterable<T>> bases) {
    if (Iterables.isEmpty(bases)) {
      throw new IllegalArgumentException();
    }
    validateSort(validateSort, bases);
    //TODO at least warn if any of the given 'bases' are equal to one another
    this.bases = bases;
  }

  @Override
  public Iterator<T> iterator() {
    return Iterables.mergeSorted(bases, Ordering.natural()).iterator();
  }

  private void validateSort(final boolean validateSort, final Iterable<Iterable<T>> bases) {
    if (! validateSort) {
      return;
    }
    for (final Iterable<T> iterable : bases) {
      if (! Utils.isSorted(iterable, true)) {
        final String msg = "Iterable " + iterable + " violates sort criteria.";
        throw new IllegalArgumentException(msg);
      }
    }
  }
}