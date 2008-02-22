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
public class LRUCache<K, V> extends LinkedHashMap<K, V> implements Cache<K, V> {
  protected int capacity;

  public LRUCache(int capacity) {
    this.capacity = capacity;
  }

  @Override 
  public synchronized V put(K key, V value) {
    //FIXME XXX HACK DISABLED
    //if(true) { return null; }
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

  @Override 
  protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
    // Return true to cause the oldest elm to be removed
    return size() > capacity;
  }
}
