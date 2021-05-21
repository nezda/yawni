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
package org.yawni.util.cache;

/**
 * A {@code Cache} is a collection of values that are indexed by keys and stored for an
 * unspecified amount of time (which the implementor of {@code Cache} may further specify).
 * Implementations utilize various strategies to evict entries (e.g., the least recently used entry)
 * and support concurrent access using different means ranging from fully serialized access via 
 * brute force synchronization to lock striping.
 */
public interface Cache<K, V> {
  /**
   * Stores {@code value} in the cache, indexed by {@code key}.  This operation makes
   * it <em>likely</em>, although not certain, that a subsequent call to {@code get} with the
   * same ({@code equals}) key will retrieve the same ({@code ==}) value.
   *
   * <p> Multiple calls to {@code put} with the same {@code key} and {@code value}
   * are idempotent.  A set of calls to {@code put} with the same {@code key} but
   * different {@code value}s has only the affect of the last call (assuming there were
   * no intervening calls to {@code get}).
   */
  V put(K key, V value);

  /**
   * If {@code key} was used in a previous call to {@code put}, this call <em>may</em>
   * return the {@code value} of that call, otherwise it returns {@code null}.
   */
  V get(K key);

  /**
   * Removes all values stored in this cache; subsequent calls to {@code get}
   * will return {@code null}.
   */
  void clear();
}