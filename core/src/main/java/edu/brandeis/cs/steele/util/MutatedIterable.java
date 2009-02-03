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

/**
 * Derive a new <code>Iterable</code> by calling a method on each of the base
 * <code>Iterable</code>s' <code>Iterator</code>'s items as computed by
 * implementations of the {@link #apply} method.
 */
public abstract class MutatedIterable<T, R> implements Iterable<R> {
  private static final long serialVersionUID = 1L;
  private final Iterable<T> base;
  public MutatedIterable(final Iterable<T> base) {
    this.base = base;
  }

  abstract public R apply(final T t);

  @SuppressWarnings("unchecked")
  public Iterator<R> iterator() {
    // i fought the compiler and the compiler won
    return (Iterator<R>)new MutatedIterator(base.iterator());
  }

  public class MutatedIterator implements Iterator {
    private final Iterator innerBase;
    public MutatedIterator(final Iterator<T> innerBase) {
      this.innerBase = innerBase;
    }
    public boolean hasNext() { return innerBase.hasNext(); }
    @SuppressWarnings(value = "unchecked")
    public Object next() { return apply((T)innerBase.next()); }
    public void remove() { innerBase.remove(); }
  } // end class MutatedIterator
}
