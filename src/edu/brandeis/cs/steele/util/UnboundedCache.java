/*
 * Unbounded cache utility class
 *
 */
package edu.brandeis.cs.steele.util;
import java.util.*;

/** A <code>Cache</code> of unbounded capacity.  Uses this at your own risk.
 *
 * @author Luke Nezda
 * @version 1.0
 */
public class UnboundedCache<K, V> extends HashMap<K, V> implements Cache<K, V> {
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
