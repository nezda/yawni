/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.yawni.wordnet.browser;

import java.awt.Component;
import java.awt.Insets;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a labeled CommandButton with an associated JPopupMenu.  The menu can
 * be dynamically populated and changed.
 *
 * <p> Key features:
 * <ul>
 *   <li> popup width is as wide as the contents, button width is only wide enough to accomodate label and
 *   icon across platforms </li>
 *   <li> down arrow indicates combobox-like behavior </li>
 *   <li> looks good across look {@literal &} feels </li>
 *   <li> activation by keyboard (spacebar and Enter) and mouse click support </li>
 *   <li> type-element-prefix-to-navigate popup menu (like JComboBox) </li>
 * </ul>
 * <p> TODO tab / shift+tab navigation from menu - maybe arrows too? complicated because would require
 * coordination among group of PopdownButtons and focus manager.
 *
 * <p> Similar to OS X {@code NSPopUpButton} in {@code "Pull Down"} mode.
 */
// These issues seems to have been avoided:
//   if mouse inButton and menu keyboard activated, takes double keyboard action to hide menu
// Mouse click event missed after use JPopupMenu
// https://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4694797
// https://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4765250
public class PopdownButton extends JButton {
  private static final Logger log = LoggerFactory.getLogger(PopdownButton.class.getName());

  private final JPopupMenu popupMenu;
  private boolean shouldHandlePopupWillBecomeInvisible = true;

  // 0> click toggles T, shows menu
  //    1> click (no toggle) T, hides menu
  //       2> click (no toggle), shows menu
  //          3> click toggles, hides menu
  //    1> click away untoggles, hides menu
  //
  // failing interactions:
  // - click, leave mouse pointer in button
  // - hit space bar (accepts menu item) - toggles my button, hides menu
  //
  // nice-to-haves
  // - menu shown/hidden on press, rather than on release
  // - default down triangle 'buddy' icon
  // - check out Kirill's CommandButton
  //
  // functioning interactions:
  // - pure keyboard with mouse pointer not in button
  // - pure mouse
  public PopdownButton(final String buttonLabel) {
    super(buttonLabel);
    // Install Action on the button to hide and show the popup
    // menu as appropriate.
    final Action action = createButtonAction(buttonLabel);
    this.setAction(action);

    this.setVerticalTextPosition(AbstractButton.CENTER);
    this.setHorizontalTextPosition(AbstractButton.LEADING);

    // https://developer.apple.com/technotes/tn2007/tn2196.html#BUTTONS
    this.putClientProperty("JButton.buttonType", "textured");
    this.putClientProperty("JComponent.sizeVariant", "regular");

    enterPressesWhenFocused(this);

    this.popupMenu = new JPopupMenu();
    // should popupMenu be a focus cycle root?
    //this.popupMenu.

    this.setComponentPopupMenu(popupMenu);

    popupMenu.setLightWeightPopupEnabled(false);
//    System.err.println("popupMenu: "+popupMenu.isLightWeightPopupEnabled());
    popupMenu.addMenuKeyListener(new DefaultMenuKeyListener());

    // add a popup menu listener to update the button's selection state
    // when the menu is being dismissed.
    popupMenu.addPopupMenuListener(createPopupMenuListener());

    // The special sauce to make this command button work right.
    // God bless
    // - https://explodingpixels.wordpress.com/2008/11/10/prevent-popup-menu-dismissal/
    // - https://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6350814
    // Install a special client property on the button to prevent it from
    // closing the popup when the down arrow is pressed.
    final JComboBox<Object> box = new JComboBox<>();
    final Object preventHide = box.getClientProperty("doNotCancelPopup");
    this.putClientProperty("doNotCancelPopup", preventHide);
  }

  private static void enterPressesWhenFocused(final AbstractButton button) {
    button.registerKeyboardAction(
      button.getActionForKeyStroke(
      KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false)),
      KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false),
      JComponent.WHEN_FOCUSED);

    button.registerKeyboardAction(
      button.getActionForKeyStroke(
      KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, true)),
      KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, true),
      JComponent.WHEN_FOCUSED);
  }

  private Action createButtonAction(final String label) {
    final AbstractAction action = new AbstractAction(label) {
      public void actionPerformed(final ActionEvent evt) {
        if (!isEnabled()) {
          log.warn("not enabled");
          return;
        }
        // if the popup menu is currently showing, then hide it.
        // else if the popup menu is not showing, then show it.
        if (popupMenu.isShowing()) {
          hidePopupMenu();
        } else {
          showPopupMenu();
        }
      }
    };
    //final Icon rightArrow = javax.swing.plaf.basic.BasicIconFactory.getMenuArrowIcon();
    final Icon downArrow = new javax.swing.plaf.metal.MetalComboBoxIcon();
    action.putValue(AbstractAction.SMALL_ICON, downArrow);
    return action;
  }

  private PopupMenuListener createPopupMenuListener() {
    return new PopupMenuListener() {
      public void popupMenuWillBecomeVisible(final PopupMenuEvent evt) {
        // no op
      }

      public void popupMenuWillBecomeInvisible(final PopupMenuEvent evt) {
        // handle this event if so indicated. the only time we don't handle
        // this event is when the button itself is pressed, the press action
        // toggles the button selected state for us. this case handles when
        // the button has been toggled, but the user clicks outside the
        // button in order to dismiss the menu.
        if (shouldHandlePopupWillBecomeInvisible) {
          PopdownButton.this.setSelected(false);
        }
      }

      public void popupMenuCanceled(final PopupMenuEvent evt) {
        log.debug("cancelled {}", evt);
        // the popup menu has been canceled externally (either by
        // pressing escape or clicking off of the popup menu). update
        // the button's state to reflect the menu dismissal.
        PopdownButton.this.setSelected(false);
      }
    };
  }

  protected void hidePopupMenu() {
    shouldHandlePopupWillBecomeInvisible = false;
    popupMenu.setVisible(false);
    shouldHandlePopupWillBecomeInvisible = true;
  }

  private void showPopupMenu() {
    // show the menu below the button, and slightly to the right.
    final Insets margins = this.getMargin();
    final int px = 5;
    final int py = 1 + this.getHeight() - margins.bottom;
    popupMenu.pack();
    final MenuElement[] elements = popupMenu.getSubElements();
    if (elements.length > 0 && elements[0] instanceof JMenuItem) {
//      System.err.println("customizing...");
//      final JMenuItem item0 = (JMenuItem) elements[0];

//      item0.setSelected(true);
//      item0.setArmed(false);
//      item0.setArmed(true);

//      System.err.println("isBorderPained: "+item0.isBorderPainted());
//      System.err.println("isOpaque: "+item0.isOpaque());
//      System.err.println("isSelected: "+item0.isSelected());
//      System.err.println("isArmed: "+item0.isArmed());
//      System.err.println("isFocusPainted: "+item0.isFocusPainted());
//      System.err.println("border: "+item0.getBorder());
//      System.err.println("border opaque: "+item0.getBorder().isBorderOpaque());
//      item0.setFocusPainted(true);
    }
    popupMenu.show(this, px, py);
//    final boolean focused = popupMenu.requestFocusInWindow();
//    final boolean focused = popupMenu.requestFocus(false);
//    System.err.println("popup focused?: "+focused);
  }

  @SuppressWarnings("unused")
  public AbstractButton getButton() {
    return this;
  }

  public JPopupMenu getPopupMenu() {
    return popupMenu;
  }

  /** adapted from Apache Harmony {@link JComboBox.DefaultKeySelectionManager} */
  class DefaultMenuKeyListener implements MenuKeyListener {
    DefaultMenuKeyListener() {
      // assume single-level menu
    }
    private final StringBuffer keySequence = new StringBuffer();
    private final Timer timer = new Timer(1000, new ActionListener() {
      public void actionPerformed(final ActionEvent evt) {
        log.trace("timer popped");
        clearKeySequence();
        timer.stop();
      }
    });
    private void clearKeySequence() {
      keySequence.setLength(0);
    }
    public void menuKeyTyped(final MenuKeyEvent evt) {
      log.debug("typed: {}", evt);
//      System.err.println("  "+Arrays.toString(evt.getPath()));
//      for (int i = 0; i < evt.getPath().length; i++) {
//        System.err.println("  "+i+": "+evt.getPath()[i]);
//      }
      final MenuElement[] elements = getPopupMenu().getSubElements();
      if (log.isTraceEnabled()) {
        for (int i = 0; i < elements.length; i++) {
          log.trace("  SUB[{}]: {}", i, text(elements[i]));
        }
      }
      final int selectionForKey = selectionForKey(evt.getKeyChar());
      if (selectionForKey >= 0) {
        log.debug("  SELECTED SUB[{}]: {}", selectionForKey, text(elements[selectionForKey]));
        // select path
        setArmedItem(elements[selectionForKey]);
      } else {
        log.debug("  SELECTED null");
      }
    }
    @Override
    public void menuKeyPressed(final MenuKeyEvent evt) {
    }
    @Override
    public void menuKeyReleased(final MenuKeyEvent evt) {
    }
    @SuppressWarnings("unused")
    private void printClasses(Object o) {
      Class<?> clazz = o.getClass();
      do {
        log.debug("clazz: {}", clazz);
        clazz = clazz.getSuperclass();
      } while (clazz != null);
      log.debug(""); // blank line
    }
    private void setArmedItem(final MenuElement element) {
      log.trace("ARMED {}", element);
//      printClasses(element);
      final JMenuItem item = (JMenuItem) element;
      MenuSelectionManager.defaultManager().setSelectedPath(new MenuElement[]{getPopupMenu(), item});
      assert item.isArmed();
    }
    private String text(final MenuElement element) {
      final Component comp = element.getComponent();
      if (! (comp instanceof JMenuItem)) {
        return element.toString();
      } else {
        final JMenuItem item = (JMenuItem) comp;
        return item.getText();
      }
    }
    private int getSelectedIndex() {
      return getPopupMenu().getSelectionModel().getSelectedIndex();
    }
    private int getSize() {
      return getPopupMenu().getSubElements().length;
    }
    private MenuElement getElementAt(int i) {
      return getPopupMenu().getSubElements()[i];
    }
    public int selectionForKey(final char keyChar) {
      keySequence.append(keyChar);
      timer.start();
      int result = findNextOccurence();
      if (result != -1) {
        return result;
      } else {
        clearKeySequence();
        keySequence.append(keyChar);
        return findNextOccurence();
      }
    }
    private int findNextOccurence() {
      final String beginPart = keySequence.toString().toUpperCase();
      int selectedIndex = getSelectedIndex();
      // key presses assumed to be attempting to continue current
      // match or navigate to NEXT match
      for (int i = selectedIndex + 1; i < getSize(); i++) {
        final MenuElement elem = getElementAt(i);
        if (text(elem).toUpperCase().startsWith(beginPart)) {
          return i;
        }
      }
      // start from beginning if no match from currentSelection
      for (int i = 0; i <= selectedIndex; i++) {
        final MenuElement elem = getElementAt(i);
        if (text(elem).toUpperCase().startsWith(beginPart)) {
          return i;
        }
      }
      return -1;
    }
  } // end class DefaultMenuKeyListener
}