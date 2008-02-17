/*
 * LRUCache utility class
 *
 * Copyright 1998 by Oliver Steele.  You can use this software freely so long as you preserve
 * the copyright notice and this restriction, and label your changes.
 */
package edu.brandeis.cs.steele.util;
import java.util.*;

/** A fixed-capacity <code>Cache</code> that stores the <var>n</var> most recently used
 * keys.
 *
 * @author Oliver Steele, steele@cs.brandeis.edu
 * @version 1.0
 */
public class LRUCache extends LinkedHashMap implements Cache {
  protected int capacity;

  public LRUCache(int capacity) {
    this.capacity = capacity;
  }

  public synchronized boolean isCached(Object key) {
    return super.containsKey(key);
  }

  public synchronized Object put(Object key, Object value) {
    return super.put(key, value);
  }

  public synchronized Object get(Object key) {
    return super.get(key);
  }

  public synchronized Object remove(Object key) {
    return super.remove(key);
  }

  public synchronized void clear() {
    super.clear();
  }

  @Override protected boolean removeEldestEntry(Map.Entry eldest) {
    // Return true to cause the oldest elm to be removed
    return size() > capacity;
  }
}
