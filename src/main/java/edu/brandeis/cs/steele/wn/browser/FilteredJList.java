package edu.brandeis.cs.steele.wn.browser;

import java.awt.*;
import javax.swing.event.*;
import javax.swing.*;
import javax.swing.text.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

/**
 * Make JList built to be backed by a large Iterator/Iterable
 * which fires update events every n adds (n can be picked
 * to fill the visible screen asap).  This implies a threaded
 * iterator traversal which updates the model and periodically
 * signals the view on the event thread (SwingUtilities.invokeAndWait).
 * 
 * Reports the current number of matches for use in status bar.
 *
 * Additionally, supports filtering:
 * - with and without case
 * - substring
 * - prefix
 *
 * Built for extension:
 * - includes DocumentListener (the search field Document object)
 *   - gets query from search field from DocumentEvent via the Document
 * - filter is configurable 
 *   - boolean keep(final Object listItem, final String query)
 *   XXX drop this part and filter results via an IterableFilter predicate functor
 * - 2 things can change in parallel here
 *   - query (new Iterable)
 *   - content to filter (e.g. new content iterable)
 * - results to display / filter are updated via an Iterable which
 *   is traversed in its own thread
 *
 * <b>fireXxx() methods must only be called from the event thread
 * (JCiP, pp. 195) (e.g. via SwingUtilities.invokeAndWait() or
 * SwingUtilities.invokeLater().</b>
 * 
 * @author http://www.oreilly.com/catalog/swinghks/
 */
public class FilteredJList extends JList {
  private static final long serialVersionUID = 1L;
  private static final int DEFAULT_FIELD_WIDTH = 20;
  private FilterField filterField;

  public FilteredJList() {
    setModel(new FilterModel());
    //TODO setPrototypeCellValue("A" ** searchFieldWidth);
    this.filterField = new FilterField(DEFAULT_FIELD_WIDTH);
  }

  @Override
  public void setModel(final ListModel m) {
    if (false == (m instanceof FilterModel)) {
      throw new IllegalArgumentException();
    }
    super.setModel(m);
  }
  public FilterModel getFilterModel() {
    // this cast fails during construction
    return (FilterModel)super.getModel();
  }
  public JTextField getFilterField() {
    return filterField;
  }
  /** 
   * Convenience method to add candidate (ie unfiltered)
   * item to this list.  Delegates directly to model.
   */
  void addItem(final Object o) {
    getFilterModel().addElement(o);
  }

  /** non-static inner class to provide filtered model */
  class FilterModel extends AbstractListModel {
    private static final long serialVersionUID = 1L;
    private List allItems;
    private List filterItems;
    private final ExecutorService service;
    private int rowUpdateInterval;

    public FilterModel() {
      this.allItems = new Vector();
      this.filterItems = new Vector();
      this.service = Executors.newSingleThreadExecutor(new DaemonThreadFactory());
      //XXX setRowUpdateInterval(Integer.MAX_VALUE);
      setRowUpdateInterval(1);
    }
    /** {@inheritDoc */
    public Object getElementAt(final int index) {
      if (index < filterItems.size()) {
        return filterItems.get(index);
      } else {
        return null;
      }
    }
    /** {@inheritDoc */
    public int getSize() {
      return filterItems.size();
    }
    void setRowUpdateInterval(final int rowUpdateInterval) {
      //TODO ideally just copmute this based on visible row range on-the-fly
      if(rowUpdateInterval < 0) {
        throw new IllegalArgumentException("negative rowUpdateInterval "+rowUpdateInterval);
      }
      this.rowUpdateInterval = rowUpdateInterval;
    }
    /** 
     * Convenience method to add elements to full (ie unfiltered) model.
     * Triggers a refilter().
     */
    public void addElement(final Object o) {
      allItems.add(o);
      //FIXME refilter();
    }
    private void refilter(final String query) {
      // refilter() should be called in a non-event dispatch thread - it will need a little API
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
      // lifecyce
      // - wait for work
      // - execute work, periodically call back view via fireXxx() with SwingUtilities.invokeLater()
      // - interrupt and cancel work if new work arrives
      //
      // If a refilter() is currently running (in loop below), cancel it at the next opportunity
      // and start new refliter().  Easiest way to accomplish this is to fire partial change events
      // e.g. when first n filtered items are modified, fireContentsChanged(0, n)
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

      final int oldSize = filterItems.size();
      filterItems.clear();
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          fireIntervalRemoved(this, 0, oldSize);
        }
      });
      int numComputed = 0;
      for (int i = 0; i < allItems.size(); i++) {
        if (keep(allItems.get(i), query)) {
          numComputed++;
          filterItems.add(allItems.get(i));
          // fire interval added
        }
      }
      //System.err.println("SwingUtilities.isEventDispatchThread(): "+SwingUtilities.isEventDispatchThread());
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          fireContentsChanged(this, 0, getSize());
        }
      });
    }
    boolean keep(final Object item, final String query) {
      //return allItems.get(i).toString().indexOf(query, 0) != -1;
      return item.toString().regionMatches(true, 0, query, 0, query.length());
    }
  } // end class FilterModel

  static class DaemonThreadFactory implements ThreadFactory {
    public Thread newThread(Runnable r) {
      final Thread toReturn = new Thread(r);
      toReturn.setDaemon(true);
      return toReturn;
    }
  } // end class DaemonThreadFactory
  
  /** non-statc inner class provides filter-by-keystroke field */
  class FilterField extends JTextField implements DocumentListener {
    private static final long serialVersionUID = 1L;
    public FilterField(int width) {
      super(width);
      getDocument().addDocumentListener(this);
    }
    public void changedUpdate(final DocumentEvent evt) { getFilterModel().refilter(query(evt)); }
    public void insertUpdate(final DocumentEvent evt) { getFilterModel().refilter(query(evt)); }
    public void removeUpdate(final DocumentEvent evt) { getFilterModel().refilter(query(evt)); }
    private String query(final DocumentEvent evt) {
      final Document doc = evt.getDocument();
      try {
        return doc.getText(0, doc.getLength());
      } catch(BadLocationException ble) {
        throw new RuntimeException(ble);
      }
    }
  } // end class FilterField

  // test filter list
  public static void main(String[] args) {
    final List<String> listItems = Arrays.asList(
      "Chris", "Joshua", "Daniel", "Michael",
      "Don", "Kimi", "Kelly", "Keagan"
    );
    final JFrame frame = new JFrame("FilteredJList");
    frame.getContentPane().setLayout(new BorderLayout());
    // populate list
    final FilteredJList fjl = new FilteredJList();
    for(final String item : listItems) {
      fjl.addItem(item);
    }
    //list.setAllItems(list);

    // add to gui
    final JScrollPane pane = new JScrollPane(fjl,
          ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
          ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    frame.getContentPane().add(pane, BorderLayout.CENTER);
    frame.getContentPane().add(fjl.getFilterField(), BorderLayout.NORTH);
    frame.pack();
    frame.setVisible(true);
  }
}
