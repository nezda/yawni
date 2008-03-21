/*
 * Unbounded cache utility class
 *
 */
package edu.brandeis.cs.steele.util;
import java.util.*;

/** 
 * A {@link Cache} of unbounded capacity.  Use this at your own risk.
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
