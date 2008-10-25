package edu.brandeis.cs.steele.util;

/**
 * Strided array of unsigned 3-byte integers.  This means the maximum
 * value which can be stored is 16,777,215 (i.e., 0x00FFFFFF)
 * Provides 1 byte of storage savings per offset which can add up in
 * some applications but preserves fast random access.
 * Like an array, an OffsetArray is not resizable.
 *
 * Features:
 * <ul>
 *   <li> TODO sort, including partial ranges</li>
 *   <li> TODO binary search, including partial ranges </li>
 *   <li> TODO fast serialization and deserialization ({@link Externalizable}). </li>
 * </ul>
 */
public class OffsetArray {
  // note these shift versions may be faster than their multiplication / division
  // counterparts:
  // x * 3 = x + (x << 1)
  // x / 3 = x >> 1 ... XXX
  private final int stride = 3;
  private final int maxValue = 0x00FFFFFF;
  private byte[] content;
  private int length;
  /**
   * @param number of offsets to store
   */
  public OffsetArray(final int length) {
    if (length < 0) {
      throw new IllegalArgumentException("invalid length "+length);
    }
    this.length = length;
    this.content = new byte[stride * length];
  }

  public final int get(int offset) {
    offset *= 3;
    return 
      (content[offset] & 0xFF) |
      (content[offset + 1] & 0xFF) << 8 |
      (content[offset + 2] & 0xFF) << 16;
  }

  public final void set(int offset, final int value) {
    if(value < 0 || value > maxValue) {
      throw new IllegalArgumentException("invalid value "+value);
    }
    if(offset >= length) {
      // disallow any partial write
      throw new ArrayIndexOutOfBoundsException("offset "+offset+" >= "+length);
    }
    offset *= 3;
    content[offset] = (byte) (0xFF & value);
    content[offset + 1] = (byte) (0xFF & (value >> 8));
    content[offset + 2] = (byte) (0xFF & (value >> 16));
  }

  public final int size() {
    return length;
  }

  public final boolean isEmpty() {
    return length == 0;
  }
  
  // stick with Object hashCode for now

  @Override
  public final boolean equals(Object obj) {
    if(obj instanceof OffsetArray) {
      final OffsetArray that = (OffsetArray)obj;
      return java.util.Arrays.equals(this.content, that.content);
    }
    return false;
  }

  @Override
  public final String toString() {
    final StringBuilder buffer = new StringBuilder();
    buffer.append("[");
    for(int i=0; i<length; i++) {
      buffer.append(get(i));
      if(i != (length - 1)) {
        buffer.append(", ");
      }
    }
    buffer.append("]");
    return buffer.toString();
  }
}
