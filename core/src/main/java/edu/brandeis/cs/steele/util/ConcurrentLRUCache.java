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

import java.util.*;
import java.util.concurrent.*;

/**
 * A fixed-capacity <code>Cache</code> that stores the {@code n} values associate
 * with the {@code n} most recently accessed keys.  Backed by an {@link ConcurrentMap}.
 * FIXME currently totally broken and incomplete
 */
public class ConcurrentLRUCache<K, V> extends LinkedHashMap<K, V> implements Cache<K, V> {
  private static final long serialVersionUID = 1L;

  private final ConcurrentHashMap<K, V> backingMap;
  protected final int capacity;

  public ConcurrentLRUCache(final int capacity) {
    this.capacity = capacity;
    this.backingMap = new ConcurrentHashMap<K, V>();
  }

  @Override
  public V put(K key, V value) {
    return super.put(key, value);
  }

  @Override
  public V get(Object key) {
    return super.get(key);
  }

  @Override
  public void clear() {
    super.clear();
  }

  @Override
  protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
    // Return true to cause the oldest elm to be removed
    return size() > capacity;
  }
}
