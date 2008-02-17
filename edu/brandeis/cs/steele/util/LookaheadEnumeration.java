/*
 * Lookahead utility classe
 *
 * Copyright 1998 by Oliver Steele.  You can use this software freely so long as you preserve
 * the copyright notice and this restriction, and label your changes.
 */
package edu.brandeis.cs.steele.util;
import java.util.*;

/** A wrapper for objects that declared <code>Enumeration</code>s that don't fully implement
 * <code>hasMoreElements</code>, to bring them into conformance with the specification of that
 * function.
 *
 * It's sometimes difficult to determine whether a next element exists without trying to generate
 * it.  (This is particularly true when reading elements from a stream.)  Unfortunately, the
 * <code>Enumeration</code> protocol distributes the work of determining whether another
 * element exists, and supplying it, across two methods.  A call that implements an enumerator that terminates on
 * failure to generate must therefore cache the next result.  This class can be used as a
 * wrapper, to cache the result independently of the generator logic.  <code>LookaheadEnumeration.hasMoreElements</code>
 * returns false when <code>hasMoreElements</code> of the wrapped object returns false,
 * <i>or</i> when <code>nextElement</code> of the wrapped class 
 *
 * <p>An <code>Enumeration&gt;String&lt;</code> that supplies the lines of a file until the file ends
 * can be written thus:
 * <pre>
 * new LookaheadEnumeration&gt;String&lt;(new Enumeration&gt;String&lt;() {
 *   InputStream input = ...;
 *   public boolean hasMoreElements() { return true; }
 *   public String nextElement() {
 *     String line = input.readLine();
 *     if (line == null) {
 *       throw new NoSuchElementException();
 *     }
 *     return line;
 *   }
 * }
 * </pre>
 *
 * <p>An <code>Enumeration</code> that generates the natural numbers below the first with
 * the property <var>p</var> can be written thus:
 * <pre>
 * new LookaheadEnumeration&gt;Integer&lt;(new Enumeration&gt;Integer&lt;() {
 *   int n = 0;
 *   public boolean hasMoreElements() { return true; }
 *   public Integer nextElement() {
 *     int value = n++;
 *     if (p(value)) {
 *       throw new NoSuchElementException();
 *     }
 *     return value;
 *   }
 * }
 * </pre>
 *
 * @author Oliver Steele, steele@cs.brandeis.edu
 * @version 1.0
 */
public class LookaheadEnumeration<T> implements Enumeration<T> {
  protected Enumeration<T> ground;
  protected boolean peeked = false;
  protected T nextObject;
  protected boolean more;

  public LookaheadEnumeration(Enumeration<T> ground) {
    this.ground = ground;
  }

  protected void lookahead() {
    if (peeked == false) {
      more = ground.hasMoreElements();
      if (more) {
        try {
          nextObject = ground.nextElement();
        } catch (NoSuchElementException e) {
          more = false;
        }
      }
      peeked = true;
    }
  }

  public boolean hasMoreElements() {
    lookahead();
    return more;
  }

  public T nextElement() {
    lookahead();
    if (more) {
      T result = nextObject;
      nextObject = null;	// to facilite GC
      peeked = false;
      return result;
    } else {
      throw new NoSuchElementException();
    }
  }
}
