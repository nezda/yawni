/*
 * WordNet-Java
 *
 * Copyright 1998 by Oliver Steele.  You can use this software freely so long as you preserve
 * the copyright notice and this restriction, and label your changes.
 */
package edu.brandeis.cs.steele.wn;
import java.util.NoSuchElementException;

/** Represents the major syntactic categories, or <b>p</b>art's <b>o</b>f
 * <b>s</b>peech used in WordNet.  Each <code>POS</code> has a human-readable
 * label that can be used to print it, and a key by which it can be looked up.
 *
 * @author Oliver Steele, steele@cs.brandeis.edu
 * @version 1.0
 */
public enum POS {
  //
  // Class variables
  //
  NOUN("noun", "n", 1),
  VERB("verb", "v", 2),
  ADJ("adjective", "a", 3),
  ADV("adverb", "r", 4),
  SAT_ADJ("satellite adjective", "s", 5); // ADJECTIVE_SATELLITE

  private static POS[] VALUES = values();

  static POS fromOrdinal(final byte ordinal) {
    return VALUES[ordinal];
  }

  /** A list of all <code>POS</code>s <b>except {@link POS#SAT_ADJ}</b> which doesn't
   * have its own data files. 
   */
  public static final POS[] CATS = {NOUN, VERB, ADJ, ADV}; 

  //
  // Instance implementation
  //
  protected final String label;
  protected final String toString;
  protected final String key;
  protected final int wnCode;

  POS(final String label, final String key, final int wnCode) {
    this.label = label;
    this.key = key;
    this.wnCode = wnCode;
    this.toString = new StringBuffer("[POS ").append(label).append("]").toString();
  }

  //
  // Object methods
  //

  @Override public String toString() {
    return toString;
  }
  
  //
  // Accessor
  //
  /** Return a label intended for textual presentation. */
  public String getLabel() {
    return label;
  }

  /** The integer used in the original C WordNet APIs. */
  int getWordNetCode() {
    return wnCode;
  }

  /** Return the <code>POS</code> whose key matches <var>key</var>.
   * @exception NoSuchElementException If <var>key</var> doesn't name any <code>POS</code>.
   */
  public static POS lookup(final CharSequence key) {
    for (int i = 0; i < CATS.length; ++i) {
      if (CATS[i].key.contentEquals(key)) {
        return CATS[i];
      }
    }
    throw new NoSuchElementException("unknown POS " + key);
  }
}
