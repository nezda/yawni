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

import org.yawni.util.*;
import org.yawni.util.cache.ConcurrentLinkedHashMap.EvictionPolicy;
import static org.yawni.util.cache.ConcurrentLinkedHashMap.EvictionPolicy.*;

/**
 * A fixed-capacity {@code Cache} that stores the {@code n} values associate
 * with the {@code n} most recently accessed keys.
 */
public class ConcurrentLRUCache<K, V> implements Cache<K, V> {
  private static final long serialVersionUID = 1L;

  private final ConcurrentLinkedHashMap<K, V> backingMap;
  protected final int capacity;

  public ConcurrentLRUCache(final int capacity, final EvictionPolicy evictionPolicy) {
    this.capacity = capacity;
    this.backingMap = ConcurrentLinkedHashMap.create(evictionPolicy, capacity);
  }

  public ConcurrentLRUCache(final int capacity) {
    this.capacity = capacity;
    this.backingMap = ConcurrentLinkedHashMap.create(FIFO, capacity);
  }

  @Override
  public V put(K key, V value) {
    return backingMap.put(key, value);
  }

  @Override
  public V get(K key) {
    return backingMap.get(key);
  }

  @Override
  public void clear() {
    backingMap.clear();
  }
}