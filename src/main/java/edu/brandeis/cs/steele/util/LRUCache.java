/*
 * Copyright 1998 by Oliver Steele.  You can use this software freely so long as you preserve
 * the copyright notice and this restriction, and label your changes.
 */
package edu.brandeis.cs.steele.util;

import java.util.*;

/** A fixed-capacity <code>Cache</code> that stores the <var>n</var> values associate
 * with the <var>n</var> most recently accessed keys.
 *
 * @author Oliver Steele, steele@cs.brandeis.edu
 * @version 1.0
 */
public class LRUCache<K, V> extends LinkedHashMap<K, V> implements Cache<K, V> {
  private static final long serialVersionUID = 1L;

  private static final float DEFAULT_LOAD_FACTOR = 0.75f;
  private static final boolean accessOrder = true; // means access-order (LRU)
  //private static final boolean accessOrder = false; // means insertion-order

  protected final int capacity;

  public LRUCache(final int capacity) {
    super(capacity /* initial capacity */,
        DEFAULT_LOAD_FACTOR,
        accessOrder);
    // actual capacity (ie max size)
    this.capacity = capacity;
  }

  @Override
  public synchronized V put(final K key, final V value) {
    //to disable for testing uncomment this
    //if(true) { return null; }
    return super.put(key, value);
  }

  @Override
  public synchronized V get(final Object key) {
    return super.get(key);
  }

  @Override
  public synchronized void clear() {
    super.clear();
  }

  @Override
  protected boolean removeEldestEntry(final Map.Entry<K, V> eldest) {
    // Return true to cause the oldest elm to be removed
    return size() > capacity;
  }
}
