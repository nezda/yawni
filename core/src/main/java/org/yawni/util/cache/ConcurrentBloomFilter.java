/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
// Copied from http://code.google.com/p/concurrentlinkedhashmap/wiki/BloomFilter
// with last update Oct  04, 2009
package org.yawni.util.cache;

import static java.lang.Long.bitCount;
import static java.lang.Math.abs;
import static java.lang.Math.ceil;
import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.max;
import static java.lang.Math.pow;
import static java.util.Arrays.copyOf;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicStampedReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A thread-safe Bloom filter implementation that uses a restricted version of the
 * {@link Set} interface. All mutative operations are implemented by making a fresh
 * copy of the underlying array. The set does not support traversal or removal
 * operations and may report false positives on membership queries.
 * <p>
 * The copy-on-write model is ordinarily too costly, but due to the high read/write
 * ratio common to Bloom filters this may be <em>more</em> efficient than alternatives
 * when membership operations vastly out number mutations. It is useful when you cannot
 * or do not want to synchronize membership queries, yet need to preclude interference
 * among concurrent threads.
 * <p>
 * A Bloom filter is a space and time efficient probabilistic data structure that is used
 * to test whether an element is a member of a set. False positives are possible, but false
 * negatives are not. Elements can be added to the set, but not removed. The more elements
 * that are added to the set the larger the probability of false positives. While risking
 * false positives, Bloom filters have a space advantage over other data structures for
 * representing sets by not storing the data items.
 *
 * @author <a href="mailto:ben.manes@gmail.com">Ben Manes</a>
 */
public final class ConcurrentBloomFilter<E> extends AbstractSet<E> {
  private final AtomicStampedReference<long[]> ref;
  private final float probability;
  private final int capacity;
  private final int length;
  private final int hashes;
  private final Lock lock;
  private final int bits;

  /**
   * Creates a Bloom filter that can store up to an expected maximum capacity with an acceptable probability
   * that a membership query will result in a false positive. The filter will size itself based on the given
   * parameters.
   *
   * @param capacity    The expected maximum number of elements to be inserted into the Bloom filter.
   * @param probability The acceptable false positive probability for membership queries.
   */
  public ConcurrentBloomFilter(int capacity, float probability) {
    if ((capacity <= 0) || (probability <= 0) || (probability >= 1)) {
      throw new IllegalArgumentException();
    }
    this.capacity = max(capacity, Long.SIZE);
    this.bits = bits(capacity, probability);
    this.length = bits / Long.SIZE;
    this.lock = new ReentrantLock();
    this.hashes = numberOfHashes(capacity, bits);
    this.probability = probability(hashes, capacity, bits);
    this.ref = new AtomicStampedReference<long[]>(new long[length], 0);
  }

  /**
   * Calculates the <tt>false positive probability</tt> of the {@link #contains(Object)}
   * method returning <tt>true</tt> for an object that had not been inserted into the
   * Bloom filter.
   *
   * @param hashes   The number of hashing algorithms applied to an element.
   * @param capacity The estimated number of elements to be inserted into the Bloom filter.
   * @param bits     The number of bits that can be used for storing membership.
   * @return         The estimated false positive probability.
   */
  private static float probability(int hashes, int capacity, int bits) {
    return (float) pow((1 - exp(-hashes * ((double) capacity / bits))), hashes);
  }

  /**
   * Calculates the optimal number of hashing algorithms, k, at a given sizing to
   * minimize the probability.
   *
   * @param capacity The estimated number of elements to be inserted into the Bloom filter.
   * @param bits     The number of bits that can be used for storing membership.
   * @return         The optimal number of hashing functions.
   */
  private static int numberOfHashes(int capacity, int bits) {
    return (int) ceil((((double)bits) / capacity) * log(2));
  }

  /**
   * Calculates the required number of bits assuming an optimal number of hashing
   * algorithms are available. The optimal value is normalized to the closest word.
   *
   * @param capacity    The estimated number of elements to be inserted into the Bloom filter.
   * @param probability The desired false positive probability of a membership query.
   * @return            The required number of storage bits.
   */
  private static int bits(int capacity, float probability) {
    int optimal = (int) ceil(abs(capacity * log(probability) / pow(log(2), 2)));
    int offset = Long.SIZE - (optimal % Long.SIZE);
    return (offset == 0) ? optimal : (optimal + offset);
  }

  /**
   * Returns the <tt>false positive probability</tt> of the {@link #contains(Object)}
   * method returning <tt>true</tt> for an object that had not been inserted into the
   * Bloom filter.
   *
   * @return The false positive probability for membership queries.
   */
  public float probability() {
    return probability;
  }

  /**
   * Returns the expected maximum number of elements that may be inserted into the Bloom filter.
   * The Bloom filter's space efficiency has been optimized to support this capacity with the
   * false positive probability specified by {@link #probability()}.
   *
   * @return The expected maximum number of elements to be inserted into the Bloom filter.
   */
  public int capacity() {
    return capacity;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int size() {
    return ref.getStamp();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void clear() {
    lock.lock();
    try {
      ref.set(new long[length], 0);
    } finally {
      lock.unlock();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean contains(final Object o) {
    final int[] size = new int[1];
    final long[] words = ref.get(size);
    if (size[0] == 0) {
      return false;
    }
    final int[] indexes = indexes(o);
    for (int index : indexes) {
      if (! getAt(index, words)) {
        return false;
      }
    }
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean add(final E o) {
    final int[] indexes = indexes(o);
    boolean added = false;
    lock.lock();
    try {
      final int[] size = new int[1];
      final long[] words = copyOf(ref.get(size), length);
      assert words.length == length;
      for (final int index : indexes) {
        added |= setAt(index, words);
      }
      if (added) {
        ref.set(words, ++size[0]);
      }
    } finally {
      lock.unlock();
    }
    return added;
  }

  /**
   * Retrieves the flag stored at the index location in the given array.
   *
   * @param index The bit location of the flag.
   * @param words The array to lookup in.
   * @return      The flag's value.
   */
  private boolean getAt(final int index, final long[] words) {
    //assert index >= 0 && index < bits;
    final int i = index / Long.SIZE;
    final int bitIndex = index % Long.SIZE;
    return (words[i] & (1L << bitIndex)) != 0;
  }

  /**
   * Sets the flag stored at the index location in the given array.
   *
   * @param index The bit location of the flag.
   * @param words The array to update.
   * @return      If updated.
   */
  private boolean setAt(final int index, final long[] words) {
    //assert index >= 0 && index < bits;
    final int i = index / Long.SIZE;
    final int bitIndex = index % Long.SIZE;
    final long mask = (1L << bitIndex);
    if ((words[i] & mask) == 0) {
      words[i] |= mask;
      return true;
    }
    return false;
  }

  /**
   * Calculates the index position for the object.
   *
   * @param o The object.
   * @return  The index to the bit.
   */
  private int[] indexes(Object o) {
    final int h = o.hashCode();
    final int[] indexes = new int[hashes];
    for (int i = 0; i < hashes; i++) {
      // each of the k hash functions' values will be used % bits
      // each hash is formed by simply adding and i to the base
      // hash h and hashing that sum
      indexes[i] = abs(hash(h + i)) % bits;
    }
    return indexes;
  }

  /**
   * Doug Lea's universal hashing algorithm used in the collection libraries.
   */
  private int hash(int hashCode) {
    // Spread bits using variant of single-word Wang/Jenkins hash
    hashCode += (hashCode << 15) ^ 0xffffcd7d;
    hashCode ^= (hashCode >>> 10);
    hashCode += (hashCode << 3);
    hashCode ^= (hashCode >>> 6);
    hashCode += (hashCode << 2) + (hashCode << 14);
    return hashCode ^ (hashCode >>> 16);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Iterator<E> iterator() {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof ConcurrentBloomFilter<?>)) {
      return false;
    }
    ConcurrentBloomFilter<?> filter = (ConcurrentBloomFilter<?>) o;
    int[] size1 = new int[1];
    int[] size2 = new int[1];
    long[] words1 = ref.get(size1);
    long[] words2 = filter.ref.get(size2);
    return (size1[0] == size2[0]) && Arrays.equals(words1, words2);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Arrays.hashCode(ref.getReference());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    final int size[] = new int[1];
    final long[] words = ref.get(size);
    return new StringBuilder("{").
      append("probability=").
      append(probability).
      append(", ").
      append("hashes=").
      append(hashes).
      append(", ").
      append("capacity=").
      append(capacity).
      append(", ").
      append("size=").
      append(size[0]).
      append(", ").
      append("bits=").
      append(bits).
      append(", ").
      append("set-bit-count=").
      append(toBitCount(words)).
      append(", ").
      append("value=").
      append(toBinaryArrayString(words)).
      append('}').
      toString();
  }

  /**
   * Calculates the population count, the number of one-bits, in the words.
   *
   * @param words The consistent view of the data.
   * @return      The number of one-bits in the two's complement binary representation.
   */
  private int toBitCount(final long[] words) {
    int population = 0;
    for (final long word : words) {
      population += bitCount(word);
    }
    return population;
  }

  /**
   * Creates a pretty-printed binary string representation of the data.
   *
   * @param words The consistent view of the data.
   * @return      A binary string representation.
   */
  private String toBinaryArrayString(long[] words) {
    final StringBuilder buffer = new StringBuilder(words.length);
    final int maxWords = 1;
    for (int i = words.length - 1, wordNum = 0; i >= 0 && wordNum < maxWords; i--, wordNum++) {
      final long word = words[i];
      appendBinaryString(word, buffer);
    }
    if (words.length > maxWords) {
      buffer.append("...");
    }
    return buffer.toString();
  }

  private static void appendBinaryString(final long word, final StringBuilder buffer) {
    for (int i = 0, lz = Long.numberOfLeadingZeros(word); i < lz; i++) {
      buffer.append('0');
    }
    buffer.append(Long.toBinaryString(word));
  }
}