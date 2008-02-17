/*
 * Lookahead utility classe
 *
 * Copyright 1998 by Oliver Steele.  You can use this software freely so long as you preserve
 * the copyright notice and this restriction, and label your changes.
 */
package edu.brandeis.cs.steele.util;
import java.util.*;

/** A wrapper for objects that declared <code>Iterator</code>s that don't fully implement
 * <code>hasNext</code>, to bring them into conformance with the specification of that
 * function.
 *
 * It's sometimes difficult to determine whether a next element exists without trying to generate
 * it.  (This is particularly true when reading elements from a stream.)  Unfortunately, the
 * <code>Iterator</code> protocol distributes the work of determining whether another
 * element exists, and supplying it, across two methods.  A call that implements an enumerator that terminates on
 * failure to generate must therefore cache the next result.  This class can be used as a
 * wrapper, to cache the result independently of the generator logic.  <code>LookaheadIterator.hasNext</code>
 * returns false when <code>hasNext</code> of the wrapped object returns false,
 * <i>or</i> when <code>next</code> of the wrapped class 
 *
 * <p>An <code>Iterator&gt;String&lt;</code> that supplies the lines of a file until the file ends
 * can be written thus:
 * <pre>
 * new LookaheadIterator&gt;String&lt;(new Iterator&gt;String&lt;() {
 *   InputStream input = ...;
 *   public boolean hasNext() { return true; }
 *   public String next() {
 *     String line = input.readLine();
 *     if (line == null) {
 *       throw new NoSuchElementException();
 *     }
 *     return line;
 *   }
 * }
 * </pre>
 *
 * <p>An <code>Iterator</code> that generates the natural numbers below the first with
 * the property <var>p</var> can be written thus:
 * <pre>
 * new LookaheadIterator&gt;Integer&lt;(new Iterator&gt;Integer&lt;() {
 *   int n = 0;
 *   public boolean hasNext() { return true; }
 *   public Integer next() {
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
public class LookaheadIterator<T> implements Iterator<T> {
  protected Iterator<T> ground;
  protected boolean peeked;
  protected T nextObject;
  protected boolean more;

  public LookaheadIterator(final Iterator<T> ground) {
    this.peeked = false;
    this.ground = ground;
  }

  protected void lookahead() {
    if (peeked == false) {
      more = ground.hasNext();
      if (more) {
        try {
          nextObject = ground.next();
        } catch (NoSuchElementException e) {
          more = false;
        }
      }
      peeked = true;
    }
  }

  public boolean hasNext() {
    lookahead();
    return more;
  }

  public T next() {
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

  public void remove() {
    throw new UnsupportedOperationException();
  }
}
