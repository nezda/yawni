package edu.brandeis.cs.steele.util;

import java.util.*;

/**
 * Apply a function
 */
public abstract class MutatedIterable<T, R> implements Iterable<R> {
  private static final long serialVersionUID = 1L;
  private final Iterable<T> base;
  private final Class<R> clazz;
  public MutatedIterable(final Iterable<T> base, final Class<R> clazz) {
    this.base = base;
    this.clazz = clazz;
  }
  
  abstract public R apply(final T t);

  @SuppressWarnings(value = "unchecked")
  public Iterator<R> iterator() {
    // i fought the compiler and the compiler won
    return (Iterator<R>)new MutatedIterator(base.iterator());
  }

  public class MutatedIterator implements Iterator {
    private final Iterator innerBase;
    public MutatedIterator(final Iterator<T> innerBase) {
      this.innerBase = innerBase;
    }
    public boolean hasNext() { return innerBase.hasNext(); }
    @SuppressWarnings(value = "unchecked")
    public Object next() { return apply((T)innerBase.next()); }
    public void remove() { innerBase.remove(); }
  } // end class MutatedIterator
}
