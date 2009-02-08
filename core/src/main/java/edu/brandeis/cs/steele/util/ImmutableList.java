/*
 *  Copyright (C) 2007 Google Inc.
 * 
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

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.RandomAccess;

/**
 * Particularly useful when {@code E} is immutable (end.g., {@code String}, {@code Integer}) which is
 * sometimes referred to as "deeply immutable".  
 * Strives for performance and memory characteristics of a C99 const array, especially for
 * small sizes (0-5).  "unrolled", "inside out" implementation for small sizes sacrifices verbosity for space.
 *
 * Uses cases:
 * - when would you have millions of List<T> ? especially short ones
 *   ? XML parsing / parse tree (read-only JDOM? would be fun to template that anyway if JAXB isn't already far superior)
 *   ? graph / tree data structures
 *   ? ML data structures
 *   - caches like JWordNet uses all the time - impossible to "poison" :) which we leave to chance now
 *   - Web-2.0 tags
 *   - ImmutableMultimap
 *
 * @param <E>
 */

// Comparison to Google Collections version
// + uses less memory
//   + doesn't use offset, length impl (or even Object[] ref and instance for some sizes)
//     * subList impl sorda justifies this
//   + much less for many common cases (i.end., sizes (1-5))
//   * trades some "extra" code for memory
// + supports null elements
//   * this is of dubious value - why did they do this? block errors ? speed up impl ?
//   * null has some legit uses
// * their equals() and hashCode() are optimized for comparing instances to one another which could be very good
//   - this impl is only optimized for comparing to RandomAccess List, but still has get() and null-check overhead
// + uses Iterators internally for fewer methods (end.g., contains()) - less transient allocation
// - doesn't specialize serialization
//   * they do this very well ("explicit serialized forms, though readReplace() and writeReplace() are slower)
// - doesn't use general utility impls
//   - Nullable, Iterators, ImmutableCollection, Collections2
//   - very clean slick, comprehensive use of covariant return types
// - no "bonus" APIs (end.g., copyOf())

// maximally optimized methods
// - get(i), isEmpty(), contains(), subList (for small sizes)
// 
// - consider generating code for methods like get(i), subList
//
// - hopefully compiler won't have problems with deeply nested implementation classes
//   ImmutableList -> AbstractList -> Singleton -> Doubleton -> ...

public abstract class ImmutableList<E> implements List<E>, RandomAccess {
  @SuppressWarnings("unchecked")
  public static <E> ImmutableList<E> of() {
    return Nothington.INSTANCE;
  }
  public static <E> ImmutableList<E> of(E e0) {
    return new Singleton<E>(e0);
  }
  public static <E> ImmutableList<E> of(E e0, E e1) {
    return new Doubleton<E>(e0, e1);
  }
  public static <E> ImmutableList<E> of(E e0, E e1, E e2) {
    return new Tripleton<E>(e0, e1, e2);
  }
  public static <E> ImmutableList<E> of(E e0, E e1, E e2, E e3) {
    return new Quadrupleton<E>(e0, e1, e2, e3);
  }
  public static <E> ImmutableList<E> of(E e0, E e1, E e2, E e3, E e4) {
    return new Quintupleton<E>(e0, e1, e2, e3, e4);
  }
  /**
   * Selects the most efficient implementation based on the length of {@code all}
   */
  public static <E> ImmutableList<E> of(E... all) {
    switch (all.length) {
      case 0: return ImmutableList.of();
      case 1: return ImmutableList.of(all[0]);
      case 2: return ImmutableList.of(all[0], all[1]);
      case 3: return ImmutableList.of(all[0], all[1], all[2]);
      case 4: return ImmutableList.of(all[0], all[1], all[2], all[3]);
      case 5: return ImmutableList.of(all[0], all[1], all[2], all[3], all[4]);
      default:
        if (all.length > 5) {
          //return new Restleton<E>(all);
          return new RegularImmutableList<E>(all);
        } else {
          // this is impossible
          throw new IllegalStateException();
        }
    }
  }
  public static <E> ImmutableList<E> of(final Iterable<? extends E> elements) {
    if (elements instanceof ImmutableList) {
      @SuppressWarnings("unchecked")
      final ImmutableList<E> elementsAsImmutableList = (ImmutableList<E>) elements;
      return elementsAsImmutableList;
    } else if (elements instanceof Collection) {
      @SuppressWarnings("unchecked")
      final Collection<E> elementsAsCollection = (Collection<E>) elements;
      final int size = elementsAsCollection.size();
      @SuppressWarnings("unchecked")
      final E[] elementsAsArray = (E[]) new Object[size];
      final E[] returnedElementsAsArray = elementsAsCollection.toArray(elementsAsArray);
      assert returnedElementsAsArray == elementsAsArray;
      return ImmutableList.of(elementsAsArray);
    } else {
      final Collection<E> elementsAsCollection = new ArrayList<E>();
      for(final E e : elements) {
        elementsAsCollection.add(e);
      }
      return ImmutableList.of(elementsAsCollection);
    }
  }

  private ImmutableList() {}

  static boolean eq(Object obj, Object e) {
    return obj == null ? e == null : obj.equals(e);
  }

  /**
   * Returns a new array of the given length with the same type as a reference array.
   * @param reference any array of the desired type
   * @param length the length of the new array
   */
  // lifted from Google Collections ObjectArrays
  static <T> T[] newArray(T[] reference, int length) {
    final Class<?> type = reference.getClass().getComponentType();
    // the cast is safe because result.getClass() == reference.getClass()
    @SuppressWarnings("unchecked")
    final T[] result = (T[]) Array.newInstance(type, length);
    return result;
  }

  // lifted from Google Collections
  private static Object[] copyIntoArray(Object... items) {
    final Object[] array = new Object[items.length];
    int index = 0;
    for (final Object element : items) {
//      if (element == null) {
//        throw new NullPointerException("at index " + index);
//      }
      array[index++] = element;
    }
    return array;
  }

  // lifted from Google Collections
  private static Object[] copyIntoArray(Iterable<?> items, int size) {
    final Object[] array = new Object[size];
    int index = 0;
    for (final Object element : items) {
//      if (element == null) {
//        throw new NullPointerException("at index " + index);
//      }
      array[index++] = element;
    }
    return array;
  }

  /**
   * Equivalent to {@link Collections#emptyList()}
   */
  static final class Nothington<E> extends AbstractImmutableList<E> {
    static final Nothington INSTANCE = new Nothington();
    static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
    private Nothington() {
      // no reason to make more than 1 of these
    }
    /* @Override */
    public E get(int index) {
      throw new IndexOutOfBoundsException("Index: " + index);
    }
    /* @Override */
    public int size() {
      return 0;
    }
    @Override
    public boolean isEmpty() {
      return true;
    }
    @Override
    public boolean contains(Object obj) {
      return false;
    }
    @Override
    public boolean containsAll(Collection<?> needles) {
      return needles.isEmpty();
    }
    @Override
    public int indexOf(Object e) {
      return -1;
    }
    @Override
    public int lastIndexOf(Object e) {
      return -1;
    }
    @Override
    public Iterator<E> iterator() {
      return Collections.<E>emptyList().iterator();
    }
    @Override
    public ListIterator<E> listIterator() {
      return Collections.<E>emptyList().listIterator();
    }
    @Override
    public ListIterator<E> listIterator(int index) {
      return Collections.<E>emptyList().listIterator(index);
    }
    /* @Override */
    public ImmutableList<E> subList(int fromIndex, int toIndex) {
      if (fromIndex != 0 || toIndex != 0) {
        throw new IndexOutOfBoundsException("Invalid range: " + fromIndex
            + ".." + toIndex + ", list size is 0");
      }
      return this;
    }
    @Override
    public Object[] toArray() {
      return EMPTY_OBJECT_ARRAY;
    }
    @Override
    public <T> T[] toArray(T[] a) {
      if (a.length > 0) {
        a[0] = null;
      }
      return a;
    }
    @Override
    public int hashCode() {
      return 1;
    }
    @Override
    public boolean equals(Object obj) {
      return obj instanceof List && ((List<?>) obj).isEmpty();
    }
    @Override
    public String toString() {
      return "[]";
    }
    private Object readResolve() {
      return INSTANCE;
    }
  } // end class Nothington

  /**
   * Equivalent to {@link Collections#singletonList()}
   */
  static class Singleton<E> extends AbstractImmutableList<E> {
    protected final E e0;
    Singleton(E e0) {
      this.e0 = e0;
    }
    /* @Override */
    public E get(int index) {
      // this impl looks clunky for Singleton, but pays off in Doubleton, ...
      switch (index) {
        case 0: return e0;
        default: throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
      }
    }
    /* @Override */
    public int size() {
      return 1;
    }
    @Override
    public boolean contains(Object obj) {
      // zero allocation vs. AbstractCollection impl
      return eq(obj, e0);
    }
    @Override
    public int indexOf(Object e) {
      // zero allocation vs. AbstractCollection impl
      return contains(e) ? 0 : -1;
    }
    @Override
    public int lastIndexOf(Object e) {
      // zero allocation vs. AbstractCollection impl
      return contains(e) ? 0 : -1;
    }
    /* @Override */
    @SuppressWarnings("fallthrough")
    public ImmutableList<E> subList(int fromIndex, int toIndex) {
      // fromIndex = {0,1}
      // toIndex = {0,1}
      switch (fromIndex) {
        case 0:
          switch (toIndex) {
            case 0: return ImmutableList.of();
            case 1: return this;
          }
        case 1:
          switch (toIndex) {
            case 1: return ImmutableList.of();
          }
      }
      throw new IndexOutOfBoundsException("Invalid range: " + fromIndex
            + ".." + toIndex + ", list size is " + size());
    }
  } // end class Singleton
  
  static class Doubleton<E> extends Singleton<E> {
    protected final E e1;
    Doubleton(E e0, E e1) {
      super(e0);
      this.e1 = e1;
    }
    @Override
    public E get(int index) {
      switch (index) {
        case 0: return e0;
        case 1: return e1;
        default: throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
      }
    }
    @Override
    public int size() {
      return 2;
    }
    @Override
    public boolean contains(Object obj) {
      // zero allocation vs. AbstractCollection impl
      // TODO redundant null check (in super.contains() impl)
      // TODO excessive method overhead ? (using super.contains())
      return super.contains(obj) || eq(obj, e1);
    }
    @Override
    public int indexOf(Object obj) {
      if (eq(obj, e0)) {
        return 0;
        // TODO redundant null check
      } else if (eq(obj, e1)) {
        return 1;
      } else {
        return -1;
      }
    }
    @Override
    public int lastIndexOf(Object obj) {
      if (eq(obj, e1)) {
        return 1;
        // TODO redundant null check
      } else if (eq(obj, e0)) {
        return 0;
      } else {
        return -1;
      }
    }
    @Override
    @SuppressWarnings("fallthrough")
    public ImmutableList<E> subList(int fromIndex, int toIndex) {
      // fromIndex = {0,1,2}
      // toIndex = {0,1,2}
      switch (fromIndex) {
        case 0:
          switch (toIndex) {
            case 0: return ImmutableList.of();
            case 1: return ImmutableList.of(e0);
            case 2: return this;
          }
        case 1:
          switch (toIndex) {
            case 1: return ImmutableList.of();
            case 2: return ImmutableList.of(e1);
          }
        case 2:
          switch (toIndex) {
            case 2: return ImmutableList.of();
          }
      }
      throw new IndexOutOfBoundsException("Invalid range: " + fromIndex
            + ".." + toIndex + ", list size is " + size());
    }
  } // end class Doubleton

  static class Tripleton<E> extends Doubleton<E> {
    protected final E e2;
    Tripleton(E e0, E e1, E e2) {
      super(e0, e1);
      this.e2 = e2;
    }
    @Override
    public E get(int index) {
      switch (index) {
        case 0: return e0;
        case 1: return e1;
        case 2: return e2;
        default: throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
      }
    }
    @Override
    public int size() {
      return 3;
    }
    @Override
    public boolean contains(Object obj) {
      // zero allocation vs. AbstractCollection impl
      // TODO redundant null check (in super.contains() impl)
      // TODO excessive method overhead ? (using super.contains())
      return super.contains(obj) || eq(obj, e2);
    }
    @Override
    public int indexOf(Object obj) {
      if (eq(obj, e0)) {
        return 0;
        // TODO redundant null check
      } else if (eq(obj, e1)) {
        return 1;
        // TODO redundant null check
      } else if (eq(obj, e2)) {
        return 2;
      } else {
        return -1;
      }
    }
    @Override
    public int lastIndexOf(Object obj) {
      if (eq(obj, e2)) {
        return 2;
        // TODO redundant null check
      } else if (eq(obj, e1)) {
        return 1;
        // TODO redundant null check
      } else if (eq(obj, e0)) {
        return 0;
      } else {
        return -1;
      }
    }
    @Override
    @SuppressWarnings("fallthrough")
    public ImmutableList<E> subList(int fromIndex, int toIndex) {
      // fromIndex = {0,1,2,3}
      // toIndex = {0,1,2,3}
      switch (fromIndex) {
        case 0:
          switch (toIndex) {
            case 0: return ImmutableList.of();
            case 1: return ImmutableList.of(e0);
            case 2: return ImmutableList.of(e0, e1);
            case 3: return this;
          }
        case 1:
          switch (toIndex) {
            case 1: return ImmutableList.of();
            case 2: return ImmutableList.of(e1);
            case 3: return ImmutableList.of(e1, e2);
          }
        case 2:
          switch (toIndex) {
            case 2: return ImmutableList.of();
            case 3: return ImmutableList.of(e2);
          }
        case 3:
          switch (toIndex) {
            case 3: return ImmutableList.of();
          }
      }
      throw new IndexOutOfBoundsException("Invalid range: " + fromIndex
            + ".." + toIndex + ", list size is " + size());
    }
  } // end class Tripleton

  static class Quadrupleton<E> extends Tripleton<E> {
    protected final E e3;
    Quadrupleton(E e0, E e1, E e2, E e3) {
      super(e0, e1, e2);
      this.e3 = e3;
    }
    @Override
    public E get(int index) {
      switch (index) {
        case 0: return e0;
        case 1: return e1;
        case 2: return e2;
        case 3: return e3;
        default: throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
      }
    }
    @Override
    public int size() {
      return 4;
    }
    @Override
    public boolean contains(Object obj) {
      // zero allocation vs. AbstractCollection impl
      // TODO redundant null check (in super.contains() impl)
      // TODO excessive method overhead ? (using super.contains())
      return super.contains(obj) || eq(obj, e3);
    }
    @Override
    public int indexOf(Object obj) {
      if (eq(obj, e0)) {
        return 0;
        // TODO redundant null check
      } else if (eq(obj, e1)) {
        return 1;
        // TODO redundant null check
      } else if (eq(obj, e2)) {
        return 2;
      } else if (eq(obj, e3)) {
        // TODO redundant null check
        return 3;
      } else {
        return -1;
      }
    }
    @Override
    public int lastIndexOf(Object obj) {
      if (eq(obj, e3)) {
        return 3;
        // TODO redundant null check
      } else if (eq(obj, e2)) {
        return 2;
        // TODO redundant null check
      } else if (eq(obj, e1)) {
        return 1;
        // TODO redundant null check
      } else if (eq(obj, e0)) {
        return 0;
      } else {
        return -1;
      }
    }
    @Override
    @SuppressWarnings("fallthrough")
    public ImmutableList<E> subList(int fromIndex, int toIndex) {
      // fromIndex = {0,1,2,3,4}
      // toIndex = {0,1,2,3,4}
      switch (fromIndex) {
        case 0:
          switch (toIndex) {
            case 0: return ImmutableList.of();
            case 1: return ImmutableList.of(e0);
            case 2: return ImmutableList.of(e0, e1);
            case 3: return ImmutableList.of(e0, e1, e2);
            case 4: return this;
          }
        case 1:
          switch (toIndex) {
            case 1: return ImmutableList.of();
            case 2: return ImmutableList.of(e1);
            case 3: return ImmutableList.of(e1, e2);
            case 4: return ImmutableList.of(e1, e2, e3);
          }
        case 2:
          switch (toIndex) {
            case 2: return ImmutableList.of();
            case 3: return ImmutableList.of(e2);
            case 4: return ImmutableList.of(e2, e3);
          }
        case 3:
          switch (toIndex) {
            case 3: return ImmutableList.of();
            case 4: return ImmutableList.of(e3);
          }
        case 4:
          switch (toIndex) {
            case 4: return ImmutableList.of();
          }
      }
      throw new IndexOutOfBoundsException("Invalid range: " + fromIndex
            + ".." + toIndex + ", list size is " + size());
    }
  } // end class Quadrupleton

  static class Quintupleton<E> extends Quadrupleton<E> {
    protected final E e4;
    Quintupleton(E e0, E e1, E e2, E e3, E e4) {
      super(e0, e1, e2, e3);
      this.e4 = e4;
    }
    @Override
    public E get(int index) {
      switch (index) {
        case 0: return e0;
        case 1: return e1;
        case 2: return e2;
        case 3: return e3;
        case 4: return e4;
        default: throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
      }
    }
    @Override
    public int size() {
      return 5;
    }
    @Override
    public boolean contains(Object obj) {
      // zero allocation vs. AbstractCollection impl
      // TODO redundant null check (in super.contains() impl)
      // TODO excessive method overhead ? (using super.contains())
      return super.contains(obj) || eq(obj, e4);
    }
    @Override
    public int indexOf(Object obj) {
      if (eq(obj, e0)) {
        return 0;
        // TODO redundant null check
      } else if (eq(obj, e1)) {
        return 1;
        // TODO redundant null check
      } else if (eq(obj, e2)) {
        return 2;
      } else if (eq(obj, e3)) {
        // TODO redundant null check
        return 3;
      } else if (eq(obj, e4)) {
        // TODO redundant null check
        return 4;
      } else {
        return -1;
      }
    }
    @Override
    public int lastIndexOf(Object obj) {
      if (eq(obj, e4)) {
        return 4;
        // TODO redundant null check
      } else if (eq(obj, e3)) {
        return 3;
        // TODO redundant null check
      } else if (eq(obj, e2)) {
        return 2;
        // TODO redundant null check
      } else if (eq(obj, e1)) {
        return 1;
        // TODO redundant null check
      } else if (eq(obj, e0)) {
        return 0;
      } else {
        return -1;
      }
    }
    @Override
    @SuppressWarnings("fallthrough")
    public ImmutableList<E> subList(int fromIndex, int toIndex) {
      // fromIndex = {0,1,2,3,4,5}
      // toIndex = {0,1,2,3,4,5}
      switch (fromIndex) {
        case 0:
          switch (toIndex) {
            case 0: return ImmutableList.of();
            case 1: return ImmutableList.of(e0);
            case 2: return ImmutableList.of(e0, e1);
            case 3: return ImmutableList.of(e0, e1, e2);
            case 4: return ImmutableList.of(e0, e1, e2, e3);
            case 5: return this;
          }
        case 1:
          switch (toIndex) {
            case 1: return ImmutableList.of();
            case 2: return ImmutableList.of(e1);
            case 3: return ImmutableList.of(e1, e2);
            case 4: return ImmutableList.of(e1, e2, e3);
            case 5: return ImmutableList.of(e1, e2, e3, e4);
          }
        case 2:
          switch (toIndex) {
            case 2: return ImmutableList.of();
            case 3: return ImmutableList.of(e2);
            case 4: return ImmutableList.of(e2, e3);
            case 5: return ImmutableList.of(e2, e3, e4);
          }
        case 3:
          switch (toIndex) {
            case 3: return ImmutableList.of();
            case 4: return ImmutableList.of(e3);
            case 5: return ImmutableList.of(e3, e4);
          }
        case 4:
          switch (toIndex) {
            case 4: return ImmutableList.of();
            case 5: return ImmutableList.of(e4);
          }
        case 5:
          switch (toIndex) {
            case 5: return ImmutableList.of();
          }
      }
      throw new IndexOutOfBoundsException("Invalid range: " + fromIndex
            + ".." + toIndex + ", list size is " + size());
    }
  } // end class Quintupleton

  /**
   * <h3> Broken subList impl! Do not use </h3>
   * Classic {@code ArrayList}-style implementation.  Implementation liberally copied
   * from GoogleCollections ImmutableList.RegularImmutableList
   * @param <E>
   */
  @Deprecated
  static class Restleton<E> extends AbstractImmutableList<E> {
    private final Object[] items;
    Restleton(E[] all) {
      this.items = copyIntoArray(all);
    }
    public final int size() {
      return items.length;
    }
//    public boolean contains(Object target) {
//      for (int i = begin(), n = size(); i < n; i++) {
//        // TODO redundant null check
//        if (eq(items[i], target)) {
//          return true;
//        }
//      }
//      return false;
//    }
    @Override
    public Object[] toArray() {
      Object[] newArray = new Object[size()];
      System.arraycopy(items, 0, newArray, 0, size());
      return newArray;
    }
    @Override
    public <T> T[] toArray(T[] other) {
      if (other.length < size()) {
        other = newArray(other, size());
      } else if (other.length > size()) {
        other[size()] = null;
      }
      System.arraycopy(items, 0, other, 0, size());
      return other;
    }
    @SuppressWarnings("unchecked")
    public E get(int index) {
      try {
        return (E) items[index];
      } catch(ArrayIndexOutOfBoundsException aioobe) {
        throw new IndexOutOfBoundsException(
            "Invalid index: " + index + ", list size is " + size());
      }
    }
//    public int indexOf(Object target) {
//      for (int i = begin(), n = size(); i < n; i++) {
//        // TODO redundant null check
//        if (eq(items[i], target)) {
//          return i;
//        }
//      }
//      return -1;
//    }
//    public int lastIndexOf(Object target) {
//      for (int i = size() - 1; i >= begin(); i--) {
//        // TODO redundant null check
//        if (eq(items[i], target)) {
//          return i;
//        }
//      }
//      return -1;
//    }
    public ImmutableList<E> subList(int fromIndex, int toIndex) {
      // - using 2 ints (b, end) and parent reference (Object(8)+4+4+parent(4) = 24 on 32-bit arch
      //   - could optimize with b=0 and end=size() variants :)
      if (fromIndex < 0 || toIndex > size() || fromIndex > toIndex) {
        throw new IndexOutOfBoundsException("Invalid range: " + fromIndex
            + ".." + toIndex + ", list size is " + size());
      }
      if (fromIndex == toIndex) {
        return ImmutableList.of();
      } else if (fromIndex == 0 && toIndex == size()) {
        return this;
      } else {
        return new SubList(fromIndex, toIndex);
      }
    }

    /**
     * Clipped view of its enclosing Restleton with a modified
     * - begin()
     * - end()
     * - size()
     */
    class SubList extends AbstractImmutableList<E> {
      final int begin;
      final int end;
      SubList(int begin, int end) {
        this.begin = begin;
        this.end = end;
        System.out.println("SubList begin(): "+begin()+" end(): "+end()+" size(): "+size());
      }
      @Override
      int begin() {
        return begin;
      }
      @Override
      public int end() {
        return end;
      }
      /* @Override */
      public int size() {
        return end - begin;
      }
      @Override
      public Object[] toArray() {
        throw new UnsupportedOperationException("Not supported yet.");
      }
      @Override
      public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException("Not supported yet.");
      }
      public E get(int index) {
        if (index < 0) {
          throw new IndexOutOfBoundsException();
        }
        // index needs to be adjusted for s and end
        // index 0 -> begin
        index += begin;
        if (index > end) {
          throw new IndexOutOfBoundsException();
        }
        System.out.println("begin: "+begin+" end: "+end()+" size: "+size()+" index: "+index);
        return Restleton.this.get(index);
      }
      /* @Override */
      public ImmutableList<E> subList(int fromIndex, int toIndex) {
//        if (fromIndex < 0) {
//          throw new IndexOutOfBoundsException();
//        }
//        // translate incoming indices
//        fromIndex += begin;
//        toIndex += begin;
//        if (toIndex > end) {
//          throw new IndexOutOfBoundsException();
//        }
//        return Restleton.this.subList(fromIndex, toIndex);
        if (fromIndex == toIndex) {
          return ImmutableList.of();
        } else if (fromIndex == 0 && toIndex == size()) {
          return this;
        } else {
          // translate incoming indices
          fromIndex += begin;
          toIndex += begin;
          //return new SubList(fromIndex, toIndex);
          return Restleton.this.subList(fromIndex, toIndex);
        }
      }
    } // end class SubList
  } // end class Restleton
  
  /**
   * Classic {@code ArrayList}-style implementation.  Implementation liberally copied
   * from GoogleCollections ImmutableList.RegularImmutableList
   * @param <E>
   */
  private static final class RegularImmutableList<E> extends AbstractImmutableList<E> {
    private final int offset;
    private final int size;
    private final Object[] array;

    private RegularImmutableList(Object[] array, int offset, int size) {
      this.offset = offset;
      this.size = size;
      this.array = array;
    }
    private RegularImmutableList(Object[] array) {
      this(array, 0, array.length);
    }
    public int size() {
      return size;
    }
    @Override
    public Object[] toArray() {
      final Object[] newArray = new Object[size()];
      System.arraycopy(array, offset, newArray, 0, size);
      return newArray;
    }
    @Override
    public <T> T[] toArray(T[] other) {
      if (other.length < size) {
        other = newArray(other, size);
      } else if (other.length > size) {
        other[size] = null;
      }
      System.arraycopy(array, offset, other, 0, size);
      return other;
    }
    // The fake cast to E is safe because the creation methods only allow E's
    @SuppressWarnings("unchecked")
    public E get(int index) {
      if (index < 0 || index >= size) {
        throw new IndexOutOfBoundsException(
            "Invalid index: " + index + ", list size is " + size);
      }
      return (E) array[index + offset];
    }
    @Override
    public ImmutableList<E> subList(int fromIndex, int toIndex) {
      if (fromIndex < 0 || toIndex > size || fromIndex > toIndex) {
        throw new IndexOutOfBoundsException("Invalid range: " + fromIndex
            + ".." + toIndex + ", list size is " + size);
      }
      return (fromIndex == toIndex)
          ? ImmutableList.<E>of()
          : new RegularImmutableList<E>(
              array, offset + fromIndex, toIndex - fromIndex);
    }
  } // end class RegularImmutableList

  /**
   * {@inheritDoc}
   * Ensures covariant subList type inferences works
   */
  public abstract ImmutableList<E> subList(int fromIndex, int toIndex);

  static abstract class AbstractImmutableList<E> extends ImmutableList<E> {
    // base implementation
    /* @Override */
    public boolean contains(Object target) {
      for (int i = begin(), n = end(); i < n; i++) {
        // TODO redundant null check
        if (eq(get(i), target)) {
          return true;
        }
      }
      return false;
    }
    // only Nothington overrides
    /* @Override */
    public boolean isEmpty() {
      return false;
    }
    public int indexOf(Object target) {
      for (int i = begin(), n = end(); i < n; i++) {
        // TODO redundant null check
        if (eq(get(i), target)) {
          return i;
        }
      }
      return -1;
    }
    public int lastIndexOf(Object target) {
      for (int i = end() - 1; i >= begin(); i--) {
        // TODO redundant null check
        if (eq(get(i), target)) {
          return i;
        }
      }
      return -1;
    }
    // only Nothington overrides
    /* @Override */
    public Iterator<E> iterator() {
      return new SimpleListIterator();
    }
    // only Nothington overrides
    /* @Override */
    public ListIterator<E> listIterator() {
      return new FullListIterator(begin());
    }
    /* @Override */
    public ListIterator<E> listIterator(int index) {
      return new FullListIterator(index);
    }
    // only Nothington overrides
    /* @Override */
    public boolean containsAll(Collection<?> c) {
      // TODO consider optimiziation strategies - c not necessarily a RandomAccess & List, but
      // we made that assumption for equals()
      for (final Object target : c) {
        if (false == contains(target)) {
          return false;
        }
      }
      return true;
    }
    /**
     * @throws UnsupportedOperationException
     */
    /* @Override */
    public final boolean add(E e) {
      throw new UnsupportedOperationException();
    }
    /**
     * @throws UnsupportedOperationException
     */
    public final boolean remove(Object o) {
      throw new UnsupportedOperationException();
    }
    /**
     * @throws UnsupportedOperationException
     */
    public final boolean addAll(Collection<? extends E> c) {
      throw new UnsupportedOperationException();
    }
    /**
     * @throws UnsupportedOperationException
     */
    public final boolean removeAll(Collection<?> c) {
      throw new UnsupportedOperationException();
    }
    /**
     * @throws UnsupportedOperationException
     */
    /* @Override */
    public final boolean retainAll(Collection<?> c) {
      throw new UnsupportedOperationException();
    }
    /**
     * @throws UnsupportedOperationException
     */
    /* @Override */
    public final boolean addAll(int index, Collection<? extends E> c) {
      throw new UnsupportedOperationException();
    }
    /**
     * @throws UnsupportedOperationException
     */
    /* @Override */
    public final E remove(int idx) {
      throw new UnsupportedOperationException();
    }
    /**
     * @throws UnsupportedOperationException
     */
    /* @Override */
    public final void add(int idx, E e) {
      throw new UnsupportedOperationException();
    }
    /**
     * @throws UnsupportedOperationException
     */
    /* @Override */
    public final E set(int idx, E e) {
      throw new UnsupportedOperationException();
    }
    /**
     * @throws UnsupportedOperationException
     */
    /* @Override */
    public final void clear() {
      throw new UnsupportedOperationException();
    }
    
    /* @Override */
    public Object[] toArray() {
      final Object[] newArray = new Object[size()];
      for (int i = begin(), n = end(); i < n; i++) {
        newArray[i] = get(i);
      }
      return newArray;
    }
    /* @Override */
    public <T> T[] toArray(T[] other) {
      if (other.length < size()) {
        other = newArray(other, size());
      } else if (other.length > size()) {
        other[size()] = null;
      }
      for (int i = begin(), n = end(); i < n; i++) {
        other[i] = (T) get(i);
      }
      return other;
    }
    /**
     * {@inheritDoc}
     * <p> <i> This implementation is optimized for comparing to {@link RandomAccess} {@link List} </i> </p>
     */
    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (false == (obj instanceof List)) {
        return false;
      }
      final List<?> that = (List<?>) obj;
      if (that.size() != this.size()) {
        return false;
      }
      for (int i = begin(), n = this.end(); i < n; i++) {
        if (false == eq(that.get(i), this.get(i))) {
          return false;
        }
      }
      return true;
    }
    @Override
    public int hashCode() {
      int result = 1;
      // optimized for RandomAaccess - zero allocation
      for (int i = begin(), n = end(); i < n; i++) {
        final E next = get(i);
        result = (31 * result) + (next == null ? 0 : next.hashCode());
      }
      return result;
    }
    @Override
    public String toString() {
      final StringBuilder buffer = new StringBuilder(size() * 16);
      buffer.append('[').append(get(begin()));
      for (int i = begin() + 1, n = end(); i < n; i++) {
        //System.out.println("begin(): "+begin()+" end(): "+end()+" size(): "+size());
        final E next = get(i);
        buffer.append(", ");
        // there is no way to have an ImmutableList which contains itself
        //if (next != this) {
          buffer.append(next);
        //} else {
        //  buffer.append("(this ImmutableList)");
        //}
      }
      return buffer.append(']').toString();
    }

    // name borrowed from C++ STL
    int begin() {
      return 0;
    }
    // name borrowed from C++ STL
    int end() {
      return size();
    }

    /**
     * Stating the obvious: this is a on-static class so instances
     * have implicit {@code AbstractImmutableList.this} reference.
     */
    // Lifted from Apache Harmony
    class SimpleListIterator implements Iterator<E> {
      // initial value is carefully chosen
      int pos = begin() - 1;
      /* @Override */
      public final boolean hasNext() {
        return pos + 1 < end();
      }
      /* @Override */
      public E next() {
        try {
          final E result = get(pos + 1);
          pos++;
          return result;
        } catch (IndexOutOfBoundsException e) {
          throw new NoSuchElementException();
        }
      }
      /**
       * @throws UnsupportedOperationException
       */
      /* @Override */
      public final void remove() {
        throw new UnsupportedOperationException();
      }
    } // end class SimpleListIterator

    /**
     * Stating the obvious: this is a on-static class so instances
     * have implicit {@code AbstractImmutableList.this} reference.
     */
    // Lifted from Apache Harmony
    final class FullListIterator extends SimpleListIterator implements ListIterator<E> {
      FullListIterator(int start) {
        super();
        if (begin() <= start && start <= end()) {
          pos = start - 1;
        } else {
          throw new IndexOutOfBoundsException();
        }
      }
      @Override
      public E next() {
        try {
          final E result = get(pos + 1);
         ++pos;
          return result;
        } catch (IndexOutOfBoundsException e) {
          throw new NoSuchElementException();
        }
      }
      /* @Override */
      public boolean hasPrevious() {
        return pos >= begin();
      }
      /* @Override */
      public int nextIndex() {
        return pos + 1;
      }
      /* @Override */
      public E previous() {
        try {
          final E result = get(pos);
          pos--;
          return result;
        } catch (IndexOutOfBoundsException e) {
          throw new NoSuchElementException();
        }
      }
      /* @Override */
      public int previousIndex() {
        return pos;
      }
      /**
       * @throws UnsupportedOperationException
       */
      /* @Override */
      public void add(E object) {
        throw new UnsupportedOperationException();
      }
      /**
       * @throws UnsupportedOperationException
       */
      /* @Override */
      public void set(E object) {
        throw new UnsupportedOperationException();
      }
    } // end clss FullListIterator
  } // end class AbstractImmutableList

  /**
   * Random notes
   * <ul>
   *   <li> <b> Using {@code Iterator}s is slow compared to {@link ImmutableList#get(int)} - unnecessary allocation / deallocation. </b>
   *        Unfortunately, the new {@code foreach} syntax is so convenient.
   *   </li>
   *   <li> speed: inefficiencies in most implementations of base classes ({@link AbstractCollection},
   *        {@link AbstractList}) in the form of {@link Iterator}-based implementations of key methods (end.g., {@link #equals}) </li>
   * </ul>
   */
}