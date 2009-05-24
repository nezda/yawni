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
/*
 * Copyright 1998 by Oliver Steele.  You can use this software freely so long as you preserve
 * the copyright notice.
 */
package org.yawni.util;

/**
 * A <code>Cache</code> is a collection of values that are indexed by keys and that are stored for an
 * unspecified amount of time (which the implementor of <code>Cache</code> may further specify).
 */
public interface Cache<K, V> {
  /** Store {@code value} in the cache, indexed by {@code key}.  This operation makes
   * it likely, although not certain, that a subsquent call to <code>get</code> with the
   * same (<code>equals</code>) key will retrieve the same (<code>==</code>) value.
   *
   * <P>Multiple calls to <code>put</code> with the same {@code key} and {@code value}
   * are idempotent.  A set of calls to <code>put</code> with the same {@code key} but
   * different {@code value}s has only the affect of the last call (assuming there were
   * no intervening calls to <code>get</code>).
   */
  public Object put(K key, V value);

  /** If {@code key} was used in a previous call to <code>put</code>, this call may
   * return the {@code value} of that call.  Otherwise it returns <code>null</code>.
   */
  public V get(Object key);

  /** Remove all values stored in this cache.  Subsequent calls to <code>get</code>
   * will return <code>null</code>.
   */
  public void clear();
}
