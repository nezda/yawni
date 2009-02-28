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
/*
 * Copyright 1998 by Oliver Steele.  You can use this software freely so long as you preserve
 * the copyright notice and this restriction, and label your changes.
 */
package org.yawni.util;
import java.util.*;
//TODO replace with Google Collections PeekableIterator ?
/**
 * A wrapper for objects that are declared as <code>Iterator</code>s but don't fully implement
 * <code>hasNext</code>, to bring them into conformance with the specification of that
 * method.
 *
 * It's sometimes difficult to determine whether a next element exists without trying to generate
 * it.  (This is particularly true when reading elements from a stream.)  Unfortunately, the
 * <code>Iterator</code> protocol distributes the work of determining whether another
 * element exists, and supplying it, across two methods.  A call that implements an <code>Iterator</code> that terminates on
 * failure to generate must therefore cache the next result.  This class can be used as a
 * wrapper, to cache the result independently of the generator logic.  <code>LookAheadIterator.hasNext</code>
 * returns <code>false</code> when <code>hasNext</code> of the wrapped object returns <code>false</code>,
 * <i>or</i> when <code>next</code> of the wrapped class throws a {@link NoSuchElementException}.
 *
 * <p>An {@code Iterator<String>} that supplies the lines of a file until the file ends
 * can be written thusly:
 * <pre>{@code
 * new LookAheadIterator<String>(new Iterator<String>() {
 *   BuffereredReader input = ...;
 *   public boolean hasNext() { return true; }
 *   public String next() {
 *     String line = input.readLine();
 *     if (line == null) {
 *       throw new NoSuchElementException();
 *     }
 *     return line;
 *   }
 * }
 * }</pre>
 *
 * <p>An <code>Iterator</code> that generates the natural numbers below the first with
 * that satisfy predicate {@code p} can be written thusly:
 * <pre>{@code
 * new LookAheadIterator<Integer>(new Iterator<Integer>() {
 *   int n = 0;
 *   public boolean hasNext() { return true; }
 *   public Integer next() {
 *     int value = n++;
 *     if (p(value)) {
 *       throw new NoSuchElementException();
 *     }
 *     return value;
 *   }
 *}
 *}</pre>
 */

// alternate exception-free impl
// - internally always call next() first and implement hasNext() in terms of
//   next() which will set "nextItem" to POISON when actual data source is exhausted
//   On each call to next(), source will be probed for another item to distribute
//   which will be cached.
public class LookAheadIterator<T> implements Iterator<T> {
  protected Iterator<T> ground;
  protected boolean peeked;
  protected T nextObject;
  protected boolean more;

  public LookAheadIterator(final Iterator<T> ground) {
    this.peeked = false;
    this.ground = ground;
  }

  protected void lookahead() {
    if (peeked == false) {
      more = ground.hasNext();
      if (more) {
        try {
          nextObject = ground.next();
        } catch (NoSuchElementException e) {
          more = false;
        }
      }
      peeked = true;
    }
  }

  /** {@inheritDoc} */
  public boolean hasNext() {
    lookahead();
    return more;
  }

  /** {@inheritDoc} */
  public T next() {
    lookahead();
    if (more) {
      T result = nextObject;
      nextObject = null; // to facilitate GC
      peeked = false;
      return result;
    } else {
      throw new NoSuchElementException();
    }
  }

  /** {@inheritDoc} */
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
