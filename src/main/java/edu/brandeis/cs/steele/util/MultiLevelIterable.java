package edu.brandeis.cs.steele.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/** 
 * Flattens an <code>Iterable</code> of an <code>Iterable</code> of <code>T</code> into
 * an <code>Iterable</code> of <code>T</code>.
 */
public class MultiLevelIterable<T> implements Iterable<T> {
  private static final long serialVersionUID = 1L;
  
  private final Iterable<? extends Iterable<T>> base;

  public MultiLevelIterable(final Iterable<? extends Iterable<T>> base) {
    this.base = base;
  }

  /** Primary factory method which deduces template parameters. */
  public static <T> 
    Iterable<T> of(final Iterable<? extends Iterable<T>> base) {
      return new MultiLevelIterable<T>(base);
  }
  
  /** {@inheritDoc} */
  public Iterator<T> iterator() {
    return new MultiLevelIterator<T>(base);
  }

  private static class MultiLevelIterator<T> implements Iterator<T> {
    private static final long serialVersionUID = 1L;
    private static final Object SENTINEL = new Object();

    private final Iterator<? extends Iterable<T>> base;
    // Iterator<T> or SENTINEL
    private Object top;

    MultiLevelIterator(final Iterable<? extends Iterable<T>> base) {
      this.base = base.iterator();
      chamber(true);
    }
    
    // invariants
    // if top == SENTINEL
    //   throws NoSuchElementException()
    // else, on exist
    //   top.hasNext() OR top = SENTINEL
    private void chamber(final boolean init) {
      if (init == false && top == SENTINEL) {
        // drained 
        throw new NoSuchElementException();
      } else {
        final Iterator<T> topIt = (Iterator<T>)top;
        if(init == false && topIt.hasNext()) {
          // loaded
          return;
        } else {
          while(base.hasNext()) {
            final Iterator<T> candTop = base.next().iterator();
            if(candTop.hasNext()) {
              top = candTop;
              // loaded
              return;
            }
          }
        }
        top = SENTINEL;
      }
    }

    /** {@inheritDoc} */
    public T next() {
      if (top == SENTINEL) {
        throw new NoSuchElementException();
      } else {
        final T toReturn = ((Iterator<T>)top).next();
        chamber(false);
        return toReturn;
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
  } // end class MultiLevelIterator
}
