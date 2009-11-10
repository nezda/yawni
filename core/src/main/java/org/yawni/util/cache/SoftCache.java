package org.yawni.util.cache;

/**
 * Cache based on {@link sun.misc.SoftCache}.
 */
public class SoftCache<K, V> implements Cache<K, V> {
  private static final long serialVersionUID = 1L;

  private final sun.misc.SoftCache backingMap;

  public SoftCache(final int initialCapacity) {
    this.backingMap = new sun.misc.SoftCache(initialCapacity) {
//      @Override
//      protected Object fill(Object key) {
//        return null;
//      }
    };
  }

  @Override
  @SuppressWarnings("unchecked")
  public synchronized V put(K key, V value) {
    return (V) backingMap.put(key, value);
  }

  @Override
  @SuppressWarnings("unchecked")
  public synchronized V get(K key) {
    return (V) backingMap.get(key);
  }

  @Override
  public synchronized void clear() {
    backingMap.clear();
  }
}