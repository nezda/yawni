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

import java.io.Serializable;
import static java.lang.Long.bitCount;
import static java.lang.Math.abs;
import static java.lang.Math.ceil;
import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.max;
import static java.lang.Math.pow;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Set;

/**
 * A <a href="http://en.wikipedia.org/wiki/Bloom_filter">Bloom filter</a> implementation that uses a restricted version of the
 * {@link Set} interface. The set does not support traversal, i.e., {@link #iterator iterator()}, or removal
 * operations and may report false positives on membership queries.
 * <p>
 * A Bloom filter is a space and time efficient probabilistic data structure that is used
 * to test whether an element is a member of a set. False positives are possible, but false
 * negatives are not. Elements can be added to the set, but not removed. The more elements
 * that are added to the set, the larger the probability of false positives. While risking
 * false positives, Bloom filters have a tunable space advantage over other data structures for
 * representing sets by not storing the data items.
 *
 * @author <a href="mailto:ben.manes@gmail.com">Ben Manes</a>
 */
public final class BloomFilter<E> extends AbstractSet<E> implements Serializable {
  private static final long serialVersionUID = 3;

  private final Hasher<E> hasher;
  private long[] words;
  private int size;
  private final double probability;
  private final int capacity;
  private final int length;
  private final int hashes;
  private final int bits;

  /**
   * The number of times this has been structurally modified.
   * Try to detect concurrency violations and fail-fast.
   * @see ConcurrentModificationException
   */
  private volatile transient int modCount = 0;

  private static class DefaultHasher implements Hasher {
    private static final long serialVersionUID = 3;
    public int hashCode(Object e) {
      return e.hashCode();
    }
  } // end class DefaultHasher

  private static final Hasher DEFAULT_HASHER = new DefaultHasher();

  private static <E> Hasher<E> hasher() {
    @SuppressWarnings("unchecked")
    final Hasher<E> hasher = (Hasher<E>)DEFAULT_HASHER;
    return hasher;
  }

  /**
   * Creates a Bloom filter that can store up to an expected maximum capacity with an acceptable probability
   * that a membership query will result in a false positive. The filter will size itself based on the given
   * parameters.
   *
   * @param capacity    The expected maximum number of elements to be inserted into the Bloom filter.
   * @param probability The acceptable false positive probability for membership queries.
   */
  public BloomFilter(final int capacity, final double probability) {
    this(capacity, probability, BloomFilter.<E>hasher());
  }

  /**
   * Creates a Bloom filter that can store up to an expected maximum capacity with an acceptable probability
   * that a membership query will result in a false positive. The filter will size itself based on the given
   * parameters.
   *
   * @param capacity    The expected maximum number of elements to be inserted into the Bloom filter.
   * @param probability The acceptable false positive probability for membership queries.
   * @param hasher
   */
  public BloomFilter(final int capacity, final double probability, final Hasher<E> hasher) {
    if ((capacity <= 0) || (probability <= 0) || (probability >= 1)) {
      throw new IllegalArgumentException();
    }
    this.hasher = hasher;
    this.capacity = max(capacity, Long.SIZE);
    this.bits = bits(capacity, probability);
    this.length = bits / Long.SIZE;
    this.hashes = numberOfHashes(capacity, bits);
    this.probability = probability(hashes, capacity, bits);
    this.words = new long[length];
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
  private static int bits(int capacity, double probability) {
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
  public double probability() {
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
    return size;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void clear() {
    modCount++;
    words = new long[length];
    size = 0;
  }

  private int objectHash(final Object o) {
    return hasher.hashCode(o);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean contains(final Object o) {
    if (size == 0) {
      return false;
    }
    final int h = objectHash(o);
    final int expectedModCount = modCount;
    for (int i = 0; i < hashes; i++) {
      if (! getAt(indexValue(h, i), words)) {
        return false;
      }
    }
    assert modCount == expectedModCount;
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean add(final E o) {
    boolean added = false;
    final int h = objectHash(o);
    final int expectedModCount = modCount + 1;
    modCount++;
    for (int i = 0; i < hashes; i++) {
      added |= setAt(indexValue(h, i), words);
    }
    if (added) {
      size++;
    }
    assert modCount == expectedModCount;
    return added;
  }

  /**
   * Retrieves the flag stored at the index location in the given array.
   *
   * @param index The bit location of the flag.
   * @param words The array to lookup in.
   * @return      The flag's value.
   */
  private static boolean getAt(final int index, final long[] words) {
//    final int i = index / Long.SIZE;
//    assert i == index >>> 6;
//    final int bitIndex = index % Long.SIZE;
//    final int altBitIndex = index & ((1 << 6) - 1);
    final int i = index >>> 6;
    final int bitIndex = index & ((1 << 6) - 1);
    return (words[i] & (1L << bitIndex)) != 0;
  }

  /**
   * Sets the flag stored at the index location in the given array.
   *
   * @param index The bit location of the flag.
   * @param words The array to update.
   * @return      If updated.
   */
  private static boolean setAt(final int index, final long[] words) {
//    final int i = index / Long.SIZE;
//    assert i == index >>> 6;
//    final int bitIndex = index % Long.SIZE;
//    final int altBitIndex = index & ((1 << 6) - 1);
//    assert bitIndex == altBitIndex;
    final int i = index >>> 6;
    final int bitIndex = index & ((1 << 6) - 1);
    final long mask = (1L << bitIndex);
    if ((words[i] & mask) == 0) {
      words[i] |= mask;
      return true;
    }
    return false;
  }

  private int indexValue(int h, int idx) {
    //assert idx >= 0 && idx < hashes;
    // each of the k hash functions' values will be used % bits
    return abs(hash(h + idx)) % bits;
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
    } else if (!(o instanceof BloomFilter<?>)) {
      return false;
    }
    final BloomFilter<?> that = (BloomFilter<?>) o;
    return (this.size == that.size) && Arrays.equals(this.words, that.words);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Arrays.hashCode(words);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    final int expectedModCount = modCount;
    try {
    return new StringBuilder("{").
//      append("probability=").
//      append(probability).
      append(String.format("probability=%.5f", probability)).
      append(", ").
      append("hashes=").
      append(hashes).
      append(", ").
      append("capacity=").
      append(capacity).
      append(", ").
      append("size=").
      append(size).
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
    } finally {
      assert expectedModCount == modCount;
    }
  }

  /**
   * Calculates the population count, the number of one-bits, in the words.
   *
   * @param words The consistent view of the data.
   * @return      The number of one-bits in the two's complement binary representation.
   */
  private static int toBitCount(final long[] words) {
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
  private static String toBinaryArrayString(long[] words) {
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