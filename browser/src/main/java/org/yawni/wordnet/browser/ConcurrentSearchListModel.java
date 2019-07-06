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

package org.yawni.wordnet.browser;

import javax.swing.event.*;
import javax.swing.*;
import javax.swing.text.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yawni.util.*;

/**
 * Built for easy extension:
 * <ul>
 *   <li> implements {@code ListModel}; built to display large {@code Iterable}s</li>
 *   <li> results to display / filter are updated via an {@code Iterable} which
 *   is traversed in its own thread; keeps filtering logic decoupled</li>
 *   <li> implements {@code DocumentListener} for search field which will interactively issue
 *   queries via method:
 *     {@code abstract public Iterable search(final String query)}
 *   </li>
 *   <ul>
 *     <li> this method will be called in a separate thread and should ideally
 *     support thread interruption</li>
 *     <li> gets query from search field from {@code DocumentEvent} via the {@code Document}</li>
 *   </ul>
 *   <li> uses {@code JList} reference to maintain display correctness and optimize responsiveness</li>
 * </ul>
 *
 * <h4>XXX (older prose design notes) XXX</h4>
 * Supports interactive display of a large Iterator/Iterable which (TODO start) fires update
 * events every n adds (n can be picked to fill the visible screen asap) (TODO end).
 * Traverses Iterable in a separate (single) thread and updates the model and
 * periodically signals the view on the event thread
 * ({@code SwingUtilities.invokeLater}).
 *
 * <p><strong>fireXxx() methods must only be called from the event thread
 * (JCiP, pp. 195) (e.g., via {@code SwingUtilities.invokeAndWait()} or
 * {@code SwingUtilities.invokeLater()}.</strong>
 *
 * <p>Inspired by <a href="http://www.oreilly.com/catalog/swinghks/">http://www.oreilly.com/catalog/swinghks/</a>
 */
public abstract class ConcurrentSearchListModel extends AbstractListModel implements DocumentListener {
  private static final Logger log = LoggerFactory.getLogger(ConcurrentSearchListModel.class.getName());
  private List filterItems;
  private final ExecutorService service;
  private int rowUpdateInterval;

  /** 
   * {@code jlist} should be focusable <em>if</em> it has contents (ie not empty)
   * <strong>and</strong> its values are <u>not changing</u>
   */
  private void setFocusable(final boolean focusable) {
    jlist.clearSelection();
    jlist.setFocusable(focusable);
  }
  private JList jlist;
  private Future lastTask;
  private String lastQuery;

  public ConcurrentSearchListModel() {
    //TODO consider CopyOnWriteArrayList
    this.filterItems = new Vector();
    this.service = Executors.newSingleThreadExecutor(new DaemonThreadFactory());
    //this.service = Executors.newFixedThreadPool(2, new DaemonThreadFactory());
    //setRowUpdateInterval(Integer.MAX_VALUE);
    //setRowUpdateInterval(1);
  }

  abstract public Iterable search(final String query);

  public void searchDone(final String query, final int numHits) { }

  public void setRowUpdateInterval(final int rowUpdateInterval) {
    //TODO ideally just compute this based on visible row range on-the-fly
    //XXX simple, but superior method is to make sure range [0, lastVisibleRow]
    //is updated ASAP, rest don't matter (at expense of complexity, could
    //even optimize further to range [firstVisibleRow, lastVisibleRow]
    if (rowUpdateInterval < 0) {
      throw new IllegalArgumentException("negative rowUpdateInterval "+rowUpdateInterval);
    }
    this.rowUpdateInterval = rowUpdateInterval;
  }
  
  public void setJList(final JList jlist) {
    assert jlist != null;
    this.jlist = jlist;
    setFocusable(false);
  }

  public Object getElementAt(final int index) {
    if (index < filterItems.size()) {
      return filterItems.get(index);
    } else {
      return null;
    }
  }

  public int getSize() {
    return filterItems.size();
  }

  // UncaughtExceptionHandler is better
  private static abstract class CatchAndRelease implements Runnable {
    abstract void doRun();
    public void run() {
      try {
        doRun();
      } catch (Throwable t) {
        log.error("uh oh: {}", t);
      }
    }
  } // end class CatchAndRelease
  
  private void redisplay(final Iterable toDisplay, final String query) {
    //XXX System.err.println("doRedisplay submitted "+new Date());
    final Future submittedTask =
      service.submit(new CatchAndRelease() {
      @Override
      void doRun() {
        doRedisplay(toDisplay, query);
        //XXX System.err.println("doRedisplay done      "+new Date());
      }
    });
    if (lastTask != null) {
      final boolean mayInterruptIfRunning = true;
      final boolean lastTaskCancelled = lastTask.cancel(mayInterruptIfRunning);
      if (lastTaskCancelled && lastTask.isCancelled()) {
        System.err.println("lastTaskCancelled: "+lastTaskCancelled+
            " lastQuery: \""+lastQuery+"\" query: \""+query+"\""+
            String.format(" %,d", System.currentTimeMillis()));
      }
    }
    lastTask = submittedTask;
    lastQuery = query;
  }
  // runs in background thread; respects interrupts
  // runs complete search in background, then updates displayed list
  // TODO
  // - run some of search, then display some of search, run more of search, update display
  private void doRedisplay(final Iterable toDisplay, final String query) {
    // redisplay() should be called in a non-event dispatch thread - it will need a little API
    // Easiest to use a single-threaded ExecutorService
    // - "waiting for work" is trivial
    // ? how do we know if there are any workers in progress ?
    // ? how do we know if there's any work queued ? (maintain queue of work)
    // - allows cancellation
    // - handles worker daemon-ness (ThreadFactory that creates daemon threads - doesn't need to be shutdown)
    // - handles work thread death
    // ? how to make latest request preempt all others ? bounded buffer of size 1
    // ? maybe ScheduledThreadPoolExecutor - executes last submitted first and tosses others ?
    //
    // life cycle
    // - wait for work
    // - execute work, periodically call back view via fireXxx() with SwingUtilities.invokeLater()
    // - interrupt and cancel work if new work arrives
    //
    // If a redisplay() is currently running (in loop below), cancel it at the next opportunity
    // and start new refliter().  Easiest way to accomplish this is to fire partial change events
    // e.g., when first n filtered items are modified, fireContentsChanged(0, n)
    // then as each batch of n changes fire contents changes for that range
    // - fancy responsiveness opti - use these
    //   - getFirstVisibleIndex()
    //   - getLastVisibleIndex()
    //
    // consider ConcurrentArrayList to ensure no ConcurrentModificationException
    // ? how to ensure view is of latest snapshot of filterItems and not an inconsistent,
    //   artificial Frankenstein view ?
    //
    // TODO Consider a delay before acting on this in case there are many in short succession ?
    // Easier to cancel and more responsive looking?
    //
    // Common use case:
    // - many keypresses in short succession which refine a search
    //   - consider a small delay before initiating a search
    //   - could refine initially filtered set
    // - series of keypresses (deletes) which expands a search

    // make sure the search field is always responsive

    final int oldSize = filterItems.size();
    final List newItems = new Vector();
    for (final Object obj : toDisplay) {
      if (Thread.interrupted()) {
        System.err.println("interrupted! query: \""+query+"\""+
            " newItems.size(): "+newItems.size()+String.format(" %,d", System.currentTimeMillis()));
        break;
      }
      newItems.add(obj);
      //TODO periodically fire interval added (using SwingUtilities.invokeLater()!)
      //FIXME check interrupted() to support cancellation !!!
      //  probably best to do this in search iterators ?
    }
    searchDone(query, newItems.size());
    //XXX try {
      //XXX SwingUtilities.invokeAndWait(new Runnable() {
      SwingUtilities.invokeLater(new CatchAndRelease() {
        @Override
        void doRun() {
          // mismatch strategy optimizes common prefixes
          final int s = Utils.mismatch(filterItems, 0, filterItems.size(), newItems, 0);
          if (s != 0) {
            System.err.println("s: "+s);
          }
          setFocusable(false);
          // final int ns = newItems.size();
          // final int os = filterItems.size();
          //
          // fireContentsChanged(this, ...) [0, os) // if ns > os
          // fireIntervalAdded(this, ...) [ns - os, ns) // always
          // fireIntervalRemoved(this, ...) [ns - os, os) if os > ns
          //
          // Note: the fireXxx methods take inclusive ranges, so we need to substract 1
          // from the end points to make these correct
          for (int i = s; i < newItems.size(); i++) {
            if (Thread.interrupted()) {
              System.err.println("alt interrupted!");
            }
            if (i < filterItems.size()) {
              filterItems.set(i, newItems.get(i));
            } else {
              filterItems.add(newItems.get(i));
            }
          }
          if (filterItems.size() > newItems.size()) {
            for (int i = filterItems.size() - 1; i >= newItems.size(); i--) {
              if (Thread.interrupted()) {
                System.err.println("falt interrupted!");
              }
              filterItems.remove(i);
            }
          }
          assert filterItems.size() == newItems.size();
          // clear the current selection
          fireContentsChanged(this, 0, Math.max(getSize(), oldSize));
          setFocusable(getSize() != 0);
          //XXX fireContentsChanged(this, 0, 20);
        }
      });
    //XXX } catch(final InterruptedException ie) {
    //XXX   throw new RuntimeException(ie);
    //XXX } catch(final java.lang.reflect.InvocationTargetException ite) {
    //XXX   throw new RuntimeException(ite);
    //XXX }
  }

  public void changedUpdate(final DocumentEvent evt) {
    final String query = query(evt);
    redisplay(search(query), query);
  }

  public void insertUpdate(final DocumentEvent evt) {
    final String query = query(evt);
    redisplay(search(query), query);
  }

  public void removeUpdate(final DocumentEvent evt) {
    final String query = query(evt);
    redisplay(search(query), query);
  }

  private String query(final DocumentEvent evt) {
    final Document doc = evt.getDocument();
    try {
      return doc.getText(0, doc.getLength());
    } catch (BadLocationException ble) {
      throw new RuntimeException(ble);
    }
  }

  static class DaemonThreadFactory implements ThreadFactory {
    public Thread newThread(Runnable r) {
      final Thread toReturn = new Thread(r);
      toReturn.setDaemon(true);
      return toReturn;
    }
  } // end class DaemonThreadFactory
}