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

//import org.yawni.util.cache.ConcurrentLinkedHashMap.EvictionPolicy;
//import static org.yawni.util.cache.ConcurrentLinkedHashMap.EvictionPolicy.*;

/**
 * Factory used to centralize {@link Cache} creation throughout Yawni.
 */
public class Caches {
  /** Centralized {@link Cache} factory. */
  public static <K, V> Cache<K, V> withCapacity(final int capacity) {
    return new CaffeineConcurrentSoftCache<>(capacity);
//    return new ConcurrentSoftCache<>(capacity);
//    return new LRUCache<K, V>(capacity);
//    return new ConcurrentLRUCache<K, V>(capacity, LRU);
//    return new ConcurrentLRUCache<K, V>(capacity, SECOND_CHANCE);
//    return new ConcurrentLRUCache<K, V>(capacity);
//    return new WeakHashMapCache<K, V>(capacity);
//    return new UnboundedCache<K, V>(capacity);
  }
}