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
package org.yawni.util.cache;

import java.util.*;

/**
 * A fixed-capacity {@code Cache} that stores the {@code n} values associated
 * with the {@code n} most recently accessed keys.
 * All methods are thread-safe by brute-force synchronization.
 */
public class LRUCache<K, V> implements Cache<K, V> {
  private static final long serialVersionUID = 1L;

  private static final float DEFAULT_LOAD_FACTOR = 0.75f;
  private static final boolean accessOrder = true; // means access-order (LRU)
  //private static final boolean accessOrder = false; // means insertion-order (FIFO)

  private final LinkedHashMap<K, V> backingMap;
  protected final int capacity;

  public LRUCache(final int capacity) {
    this.backingMap = new LinkedHashMap<K, V>(capacity /* initial capacity */,
        DEFAULT_LOAD_FACTOR,
        accessOrder) {
      @Override
      protected boolean removeEldestEntry(final Map.Entry<K, V> eldest) {
        // Return true to cause the oldest elm to be removed
        return size() > capacity;
      }
    };
    // actual capacity (ie max size)
    this.capacity = capacity;
  }

  @Override
  public synchronized V put(final K key, final V value) {
    //to disable for testing uncomment this
    //if(true) { return null; }
    return backingMap.put(key, value);
  }

  @Override
  public synchronized V get(final K key) {
    return backingMap.get(key);
  }

  @Override
  public synchronized void clear() {
    backingMap.clear();
  }
}