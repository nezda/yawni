package org.yawni.util.cache;

import java.util.Map;

/**
 * Memory-sensitive {@code Cache} based on {@link sun.misc.SoftCache} which
 * is based on {@link java.lang.ref.SoftReference}s.
 */
public class SoftCache<K, V> implements Cache<K, V> {
  private static final long serialVersionUID = 1L;

  private final Map<K, V> backingMap;

  @SuppressWarnings("unchecked")
  public SoftCache(final int initialCapacity) {
    this.backingMap = (Map<K, V>) new sun.misc.SoftCache(initialCapacity);
  }

  @Override
  public synchronized V put(K key, V value) {
    return backingMap.put(key, value);
  }

  @Override
  public synchronized V get(K key) {
    return backingMap.get(key);
  }

  @Override
  public synchronized void clear() {
    backingMap.clear();
  }
}