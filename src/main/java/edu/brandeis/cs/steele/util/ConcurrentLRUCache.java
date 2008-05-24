package edu.brandeis.cs.steele.util;

import java.util.*;
import java.util.concurrent.*;

/** A fixed-capacity <code>Cache</code> that stores the <var>n</var> values associate
 * with the <var>n</var> most recently accessed keys.  Backed by an {@link ConcurrentMap}.
 * FIXME currently totally broken and incomplete
 */
public class ConcurrentLRUCache<K, V> extends LinkedHashMap<K, V> implements Cache<K, V> {
  private static final long serialVersionUID = 1L;
  private final ConcurrentHashMap<K, V> backingMap;

  protected final int capacity;

  public ConcurrentLRUCache(final int capacity) {
    this.capacity = capacity;
    this.backingMap = new ConcurrentHashMap();
  }

  public V put(K key, V value) {
    return super.put(key, value);
  }

  public V get(Object key) {
    return super.get(key);
  }

  public void clear() {
    super.clear();
  }

  protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
    // Return true to cause the oldest elm to be removed
    return size() > capacity;
  }
}
