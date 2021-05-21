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

import com.google.common.base.Stopwatch;
import com.google.common.base.Suppliers;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import com.google.common.primitives.Ints;

import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.yawni.wordnet.POS;
import org.yawni.wordnet.Relation;
import org.yawni.wordnet.RelationType;
import org.yawni.wordnet.Synset;
import org.yawni.wordnet.WordNet;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

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
              final String msg = "Sort failure: prev.compareTo(curr): "
                  + prev.compareTo(curr) + " prev: " + prev + " curr: " + curr;
              throw new IllegalArgumentException(msg);
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
//@Deprecated // use com.google.common.collect.Iterables#getFirst
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

  static class Counted<T> implements Comparable<Counted<T>> {
    public final T item;
    public final int count;

    Counted(final T item, final int count) {
      this.item = item;
      this.count = count;
    }

    @Override
    public int compareTo(final Counted<T> that) {
      return Ints.compare(this.count, that.count);
    }
  }

  public static ListMultimap<Synset, Synset> dijkstraPath(Synset source) {
    // standard algorithm copy/ported from https://github.com/gengoai/hermes/blob/987e7a05f9919476a86ce0af3d527a18fae8336c/wordnet/src/main/java/com/gengoai/hermes/wordnet/WordNet.java#L109
    //    Counter<Synset> dist = Counters.newCounter();
    WordNet wordNet = WordNet.getInstance();
    Multiset<Synset> dist = HashMultiset.create();
    Map<Synset, Synset> previous = new HashMap<>();
    Set<Synset> visited = Sets.<Synset>newHashSet(source);
    assert visited.size() == 1;

    for (Synset other : wordNet.synsets(source.getPOS())) {
      if (!other.equals(source)) {
        dist.setCount(other, Integer.MAX_VALUE);
        previous.put(other, null);
      }
    }

    PriorityQueue<Counted<Synset>> queue = new PriorityQueue<>();
    queue.add(new Counted<>(source, 0));

    while (!queue.isEmpty()) {
      Counted<Synset> next = queue.remove();

      Synset synset = next.item;
      visited.add(synset);

      Iterable<Synset> neighbors = Stream.of(
              synset.getSemanticRelationTargets(RelationType.HYPERNYM),
              synset.getSemanticRelationTargets(RelationType.INSTANCE_HYPERNYM),
              synset.getSemanticRelationTargets(RelationType.HYPONYM),
              synset.getSemanticRelationTargets(RelationType.INSTANCE_HYPONYM)
      ).flatMap(s -> s).collect(toImmutableList());

      for (Synset neighbor : neighbors) {
        final int alt = dist.count(synset);
        if (alt != Integer.MAX_VALUE && (alt + 1) < dist.count(neighbor)) {
          dist.setCount(neighbor, alt + 1);
          previous.put(neighbor, synset);
        }
        if (!visited.contains(neighbor)) {
          queue.add(new Counted<>(neighbor, alt));
        }
      }
    }

    final ImmutableListMultimap.Builder<Synset, Synset> path = ImmutableListMultimap.builder();
    for (Synset other : wordNet.synsets(source.getPOS())) {
      if (other.equals(source) || dist.count(other) == Integer.MAX_VALUE) {
        continue;
      }

      Deque<Synset> stack = new LinkedList<>();
      Synset u = other;
      while (u != null && previous.containsKey(u)) {
        stack.push(u);
        u = previous.get(u);
      }
      while (!stack.isEmpty()) {
        Synset to = stack.pop();
        path.put(other, to);
      }
    }

    return path.build();
  }

  private static final LoadingCache<Synset, ListMultimap<Synset, Synset>> shortestPathCache =
      CacheBuilder.newBuilder().maximumSize(200_000)
          .build(CacheLoader.from(
              input -> input == null
                  ? null
                  : dijkstraPath(input)
              ));

  private static List<Synset> shortestPath(Synset synset1, Synset synset2) {
    return shortestPathCache
            .getUnchecked(synset1)
            .get(synset2);
  }

  /**
   * Calculates the distance between synsets.
   *
   * @param synset1 Synset 1
   * @param synset2 Synset 2
   * @return The distance
   */
  public static int distance(Synset synset1, Synset synset2) {
    checkNotNull(synset1);
    checkNotNull(synset2);
    if (synset1.equals(synset2)) {
      return 0;
    }
    List<Synset> path = shortestPath(synset1, synset2);
    return path.isEmpty()
        ? Integer.MAX_VALUE
        : path.size() - 1;
  }

  private static final Supplier<Set<Synset>> ROOT_CACHE =
      Suppliers.memoize(Utils::findRoots);

  /**
   * Find the "root" {@code Synset}s: those with no {@link RelationType#HYPERNYM} or
   * {@link RelationType#INSTANCE_HYPERNYM}.
   */
  public static Set<Synset> findRoots() {
    final Stopwatch stopwatch = Stopwatch.createStarted();
    try {
      return Streams.stream(WordNet.getInstance().synsets(POS.ALL))
          .filter(synset -> synset.getRelations().stream()
              .filter(Relation::isSemantic)
              .noneMatch(relation ->
                  relation.getType() == RelationType.HYPERNYM ||
                      relation.getType() == RelationType.INSTANCE_HYPERNYM))
          .collect(toImmutableSet());
    } finally {
      stopwatch.stop();
      System.err.println("findRoots took "+stopwatch);
    }
  }

  /**
   * The shortest distance from {@code synset} to a "root" {@code Synset} (one with no
   * {@link RelationType#HYPERNYM} or {@link RelationType#INSTANCE_HYPERNYM}
   */
  public static int depth(Synset synset) {
    return ROOT_CACHE.get().stream()
        .mapToInt(root -> distance(synset, root))
        .min().orElse(-1);
  }

  /**
   * Gets the node that is least common subsumer: the {@code Synset} with maximum height that
   * is a parent to both nodes.
   *
   * @param synset1 The first node
   * @param synset2 The second node
   * @return The least common subsumer or null
   */
  public static Synset getLeastCommonSubsumer(Synset synset1, Synset synset2) {
    checkNotNull(synset1);
    checkNotNull(synset2);

    if (synset1.equals(synset2)) {
      return synset1;
    }

    Stopwatch stopwatch = Stopwatch.createStarted();
    List<Synset> path = shortestPath(synset1, synset2);
    stopwatch.stop();
    System.err.println("  shortestPath took "+stopwatch);
    if (path.isEmpty()) {
      return null;
    }

    stopwatch.reset().start();
    int node1Height = Utils.depth(synset1);
    int node2Height = Utils.depth(synset2);
//    stopwatch.stop();
//    System.err.println("  depth finding took "+stopwatch);
    int minHeight = Math.min(node1Height, node2Height);
    int maxHeight = Integer.MIN_VALUE;
    Synset lcs = null;
    for (Synset s : path) {
      if (s.equals(synset1) || s.equals(synset2)) {
        continue;
      }
      int height = Utils.depth(s);
      if (height < minHeight && height > maxHeight) {
        maxHeight = height;
        lcs = s;
      }
    }
    if (lcs == null) {
      if (node1Height < node2Height) {
        return synset1;
      }
      return synset2;
    }
    return lcs;
  }

  /**
   * @param ints to {@link Arrays#hashCode(int[])} as varargs
   * @return a corresponding hash code
   */
  public static int hash(int... ints) {
    return Arrays.hashCode(ints);
  }
}