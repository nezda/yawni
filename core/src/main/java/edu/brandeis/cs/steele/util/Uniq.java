package edu.brandeis.cs.steele.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Derive a new <code>Iterable</code> by removing adjacent duplicates
 * of the provided <i>sortable</i> <code>Iterable</code>s.
 * <p>Much like the STL version below:<br> 
 * <code>std::unique_copy(c.begin(), c.end(),  
 *   std::ostream_iterator<c::type>(std::cout, " "));</code>
 */
class Uniq<T extends Object & Comparable<? super T>> implements Iterable<T> {
  private static final long serialVersionUID = 1L;

  private final Iterable<T> base;

  Uniq(final Iterable<T> base) {
    this(false, base);
  }

  Uniq(final boolean validateSort, final Iterable<T> base) {
    validateSort(validateSort, base);
    this.base = base;
  }

  /** {@inheritDoc} */
  public Iterator<T> iterator() {
    return new UniqIterator<T>(base);
  }

  private void validateSort(final boolean validateSort, final Iterable<T> base) {
    if (false == validateSort) {
      return;
    }
    if (Utils.isSorted(base, true) == false) {
      final StringBuilder msg = new StringBuilder("Iterable ").
        append(base).append(" violates sort criteria.");
      throw new IllegalArgumentException(msg.toString());
    }
  }

  /**
   * <h4>TODO</h4>
   * - support remove()
   */
  private static class UniqIterator<T extends Object & Comparable<? super T>> implements Iterator<T> {
    private static final long serialVersionUID = 1L;
    private static final Object SENTINEL = new Object();

    private final Iterator<T> base;
    private Object top;

    UniqIterator(final Iterable<T> iterable) {
      this.base = iterable.iterator();
      if (this.base.hasNext()) {
        this.top = base.next();
      } else {
        this.top = SENTINEL;
      }
    }
    /** {@inheritDoc} */
    public T next() {
      if (top == SENTINEL) {
        throw new NoSuchElementException();
      } else {
        // store curr value of top
        // find next value of top discarding any that are equal to currTop
        // or setting top to SENTINEL to mark eos
        T currTop = (T)top;
        Object nextTop = SENTINEL;
        while (base.hasNext()) {
          final T next = base.next();
          if (currTop.compareTo(next) == 0) {
            continue;
          } else {
            nextTop = next;
            break;
          }
        }
        top = nextTop;
        return currTop;
      }
    }
    /** {@inheritDoc} */
    // user's don't have to explicitly call this although that's a bit crazy
    public boolean hasNext() {
      return top != SENTINEL;
    }
    /** {@inheritDoc} */
    public void remove() {
      throw new UnsupportedOperationException();
      // this is call refers to the last iterator which returned an item via next()
    }
  } // end class UniqIterator
}
