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
public class UnboundedCache extends HashMap implements Cache {
  public synchronized Object put(Object key, Object value) {
    return super.put(key, value);
  }

  public synchronized boolean isCached(Object key) {
    return super.containsKey(key);
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
}
