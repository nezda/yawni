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

import java.util.concurrent.ConcurrentMap;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Memory-sensitive {@code Cache} backed by a <a href="https://github.com/ben-manes/caffeine">Caffeine</a>
 * {@link ConcurrentMap} with {@link java.lang.ref.SoftReference}-values.
 */
class CaffeineConcurrentSoftCache<K, V> implements Cache<K, V> {
  private static final Logger log = LoggerFactory.getLogger(CaffeineConcurrentSoftCache.class);
  private static final long serialVersionUID = 1L;

  private final com.github.benmanes.caffeine.cache.Cache<K, V> backingCache;

  public CaffeineConcurrentSoftCache(final int initialCapacity) {
    final Caffeine<@NonNull Object, @NonNull Object> builder = Caffeine.newBuilder()
        // use "initialCapacity" as a maximumSize because softValues don't seem to be cleared quick enough under load
        .maximumSize(initialCapacity)
        .softValues();
    if (log.isDebugEnabled()) {
      builder.recordStats();
    }
    backingCache = builder.build();
  }

  @Override
  public V put(K key, V value) {
    backingCache.put(key, value);
    // not supported by Caffeine Cache#put
    return null;
  }

  // used for adding trace output to understand cache behavior
  private int queryCount = 0;

  @Override
  public V get(K key) {
    if (log.isDebugEnabled()) {
      if (++queryCount % 1_000 == 0) {
        log.debug("{}", backingCache.stats());
      }
    }
    return backingCache.getIfPresent(key);
  }

  @Override
  public void clear() {
    backingCache.invalidateAll();
  }
}