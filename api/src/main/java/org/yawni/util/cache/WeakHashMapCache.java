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

import java.util.WeakHashMap;

/**
 * Simple {@code Cache} backed by {@link WeakHashMap}.
 * All methods are thread-safe by brute-force synchronization.
 */
class WeakHashMapCache<K, V> implements Cache<K, V> {
  private static final long serialVersionUID = 1L;

  private final WeakHashMap<K, V> backingMap;

  public WeakHashMapCache(final int initialCapacity) {
    this.backingMap = new WeakHashMap<>(initialCapacity);
  }

  @Override
  public synchronized V put(K key, V value) {
    return backingMap.put(key, value);
  }

  @Override
  public synchronized V get(K key) {
    return backingMap.get(key);
  }

  @Override
  public synchronized void clear() {
    backingMap.clear();
  }
}