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
 * Flattens an {@code Iterable} of an {@code Iterable} of {@code T} into
 * an {@code Iterable} of {@code T}.
 */
public class MultiLevelIterable<T> implements Iterable<T> {
  private static final long serialVersionUID = 1L;

  /** 
   * Primary factory method which deduces template parameters.
   * @param base of approixmate type {@code Iterable<Iterable<T>>}
   * @return {@code Iterable<T>}
   */
  public static <T>
    Iterable<T> of(final Iterable<? extends Iterable<T>> base) {
      return new MultiLevelIterable<T>(base);
  }

  private final Iterable<? extends Iterable<T>> base;

  MultiLevelIterable(final Iterable<? extends Iterable<T>> base) {
    this.base = base;
  }

  /** {@inheritDoc} */
  public Iterator<T> iterator() {
    return new MultiLevelIterator<T>(base);
  }

  private static class MultiLevelIterator<T> implements Iterator<T> {
    private static final long serialVersionUID = 1L;
    private static final Object SENTINEL = new Object();

    private final Iterator<? extends Iterable<T>> base;
    // Iterator<T> or SENTINEL
    private Object top;

    MultiLevelIterator(final Iterable<? extends Iterable<T>> base) {
      this.base = base.iterator();
      chamber(true);
    }

    // invariants
    // if top == SENTINEL
    //   throws NoSuchElementException()
    // else, on exist
    //   top.hasNext() OR top = SENTINEL
    private void chamber(final boolean init) {
      if (init == false && top == SENTINEL) {
        // drained
        throw new NoSuchElementException();
      } else {
        @SuppressWarnings("unchecked")
        final Iterator<T> topIt = (Iterator<T>)top;
        if(init == false && topIt.hasNext()) {
          // loaded
          return;
        } else {
          while(base.hasNext()) {
            final Iterator<T> candTop = base.next().iterator();
            if(candTop.hasNext()) {
              top = candTop;
              // loaded
              return;
            }
          }
        }
        top = SENTINEL;
      }
    }

    /** {@inheritDoc} */
    public T next() {
      if (top == SENTINEL) {
        throw new NoSuchElementException();
      } else {
        @SuppressWarnings("unchecked")
        final T toReturn = ((Iterator<T>)top).next();
        chamber(false);
        return toReturn;
      }
    }
    /** {@inheritDoc} */
    // user's don't have to explicitly call this although that's a bit crazy
    public boolean hasNext() {
      return top != SENTINEL;
    }
    /** {@inheritDoc} */
    public void remove() {
      throw new UnsupportedOperationException();
      // this is call refers to the last iterator which returned an item via next()
    }
  } // end class MultiLevelIterator
}