package edu.brandeis.cs.steele.wn.browser;

import java.awt.*;
import java.util.*;

/**
 * Simple FocusTraversalPolicy which cycles through provided components in sequential order
 * and defauls to the first component
 */
class SimpleFocusTraversalPolicy extends FocusTraversalPolicy {
  private final Vector<Component> order;

  public SimpleFocusTraversalPolicy(final java.util.List<Component> order) {
    this.order = new Vector<Component>(order.size());
    this.order.addAll(order);
  }

  public Component getComponentAfter(final Container focusCycleRoot, final Component aComponent) {
    return getNextComponent(focusCycleRoot, aComponent, true);
  }

  public Component getComponentBefore(final Container focusCycleRoot, final Component aComponent) {
    return getNextComponent(focusCycleRoot, aComponent, false);
  }

  public Component getDefaultComponent(Container focusCycleRoot) {
    return order.get(0);
  }

  public Component getLastComponent(Container focusCycleRoot) {
    return order.lastElement();
  }

  public Component getFirstComponent(Container focusCycleRoot) {
    return order.get(0);
  }

  private Component getNextComponent(final Container focusCycleRoot, final Component aComponent, final boolean after) {
    final int idx = order.indexOf(aComponent);
    if(idx < 0) {
      return getDefaultComponent(focusCycleRoot);
    }
    for(int
        n = order.size(), i = next(idx, n, after), cnt = 0;
        cnt < n;
        i = next(i, n, after), cnt++) {
      final Component comp = order.get(i);
      if(comp.isEnabled() && comp.isFocusable()) {
        return order.get(i);
      }
    }
    return getDefaultComponent(focusCycleRoot);
  }

  private int next(final int i, final int n, final boolean after) {
    if(after) {
      // 0 1 2 0 1 2
      return (i + 1) % n;
    } else {
      assert i >= 0;
      // 0 2 1 0 2 1
      if(i - 1 == 0) {
        return n - 1;
      } else {
        return i - 1;
      }
    }
  }
} // end class SimpleFocusTraversalPolicy

