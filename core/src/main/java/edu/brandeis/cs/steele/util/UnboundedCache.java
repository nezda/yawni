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

import java.util.*;

/**
 * A {@link Cache} of unbounded capacity.  Use this at your own risk ({@link OutOfMemoryError}).
 */
public class UnboundedCache<K, V> extends HashMap<K, V> implements Cache<K, V> {
  private static final long serialVersionUID = 1L;

  @Override
  public synchronized V put(K key, V value) {
    return super.put(key, value);
  }

  @Override
  public synchronized V get(Object key) {
    return super.get(key);
  }

  @Override
  public synchronized void clear() {
    super.clear();
  }
}
