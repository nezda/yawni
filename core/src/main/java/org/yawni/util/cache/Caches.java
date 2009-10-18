package org.yawni.util.cache;

import org.yawni.util.*;
import org.yawni.util.cache.Cache;
import org.yawni.util.cache.ConcurrentLinkedHashMap.EvictionPolicy;
import static org.yawni.util.cache.ConcurrentLinkedHashMap.EvictionPolicy.*;

public class Caches {
  public static <K, V> Cache<K, V> withCapacity(final int capacity) {
    return new LRUCache<K, V>(capacity);
//    return new ConcurrentLRUCache<K, V>(capacity, LRU);
//    return new ConcurrentLRUCache<K, V>(capacity, SECOND_CHANCE);
//    return new ConcurrentLRUCache<K, V>(capacity);
//    return new UnboundedCache<K, V>(capacity);
  }
}