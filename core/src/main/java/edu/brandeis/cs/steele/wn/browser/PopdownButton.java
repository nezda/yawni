package edu.brandeis.cs.steele.wn.browser;

import java.awt.Insets;
import java.awt.event.*;
import java.io.Serializable;
import javax.swing.*;
import javax.swing.event.*;

/**
 * Creates a labeled CommandButton with an associated JPopupMenu.  The menu can
 * be dynamically populated and changed.
 * 
 * <p>Key features:
 * <ul>
 *   <li> popup width is as wide as the contents, button width is only wide enough to accomodate label and
 *   icon across platforms
 *   <li> down arrow indicates combobox-like behavior
 *   <li> looks good across look & feels
 *   <li> activation by keyboard (spacebar and Enter) and mouse click support
 * </ul>
 * <br> TODO tab / shift+tab navigation from menu - maybe arrows too?
 * <br> TODO type-to-navigate popup menu (like JComboBox - code can be copied from there)
 */
// These issues seems to have been avoided:
// FIXME if mouse inButton and menu keyboard activated, takes double keyboard action to hide menu
// Mouse click event missed after use JPopupMenu
// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4694797
// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4765250
public class PopdownButton extends JButton {
  private static final long serialVersionUID = 1L;
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
  // - defautl down triangle 'buddy' icon
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
    //System.err.println("hor align: " + this.getHorizontalAlignment());
    //System.err.println("gap: " + this.getIconTextGap());
    // setup the default button state.
    //XXX this.setIcon(defaultIcon);
    //XXX this.setDisabledIcon(pressedAndSelectedIcon);
    //XXX this.setPressedIcon(pressedAndSelectedIcon);
    //XXX this.setSelectedIcon(pressedAndSelectedIcon);

    // not sure what this does
    //this.setComponentPopupMenu(menu);

    // http://developer.apple.com/technotes/tn2007/tn2196.html#BUTTONS
    this.putClientProperty("JButton.buttonType", "textured");
    this.putClientProperty("JComponent.sizeVariant", "regular");

    enterPressesWhenFocused(this);

    this.popupMenu = new JPopupMenu();
    // add a popup menu listener to update the button's selection state
    // when the menu is being dismissed.
    popupMenu.addPopupMenuListener(createPopupMenuListener());

    // The special sauce to make this command button work right.
    // God bless
    // - http://explodingpixels.wordpress.com/2008/11/10/prevent-popup-menu-dismissal/
    // - http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6350814
    // Install a special client property on the button to prevent it from
    // closing of the popup when the down arrow is pressed.
    final JComboBox box = new JComboBox();
    final Object preventHide = box.getClientProperty("doNotCancelPopup");
    this.putClientProperty("doNotCancelPopup", preventHide);
  }

  private static void enterPressesWhenFocused(final JButton button) {
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
      private static final long serialVersionUID = 1L;

      public void actionPerformed(final ActionEvent evt) {
        if (false == isEnabled()) {
          System.err.println("not enabled");
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
    final Insets margins = this.getMargin();
    final int px = 5;
    final int py = 1 + this.getHeight() - margins.bottom;
    popupMenu.show(this, px, py);
    System.err.println("focusable?: "+popupMenu.isFocusable());
    // show the menu below the button, and slightly to the right.
    //popupMenu.show(this, 5, this.getHeight());
  }

  public AbstractButton getButton() {
    return this;
  }

  public JPopupMenu getPopupMenu() {
    return popupMenu;
  }

  // snipped from Apache Harmony JComboBox
  // selectWithKeyChar()
  class DefaultKeySelectionManager implements JComboBox.KeySelectionManager, Serializable {
    public int selectionForKey(char aKey, final ComboBoxModel aModel) {
      int currentSelection = -1;
      final Object selectedItem = aModel.getSelectedItem();

      if (selectedItem != null) {
        for (int i = 0, c = aModel.getSize(); i < c; i++) {
          if (selectedItem == aModel.getElementAt(i)) {
            currentSelection = i;
            break;
          }
        }
      }

      //final String pattern = ("" + aKey).toLowerCase();
      aKey = Character.toLowerCase(aKey);

      for (int i = ++currentSelection, c = aModel.getSize(); i < c; i++) {
        final Object elem = aModel.getElementAt(i);
        if (elem != null && elem.toString() != null) {
          final String v = elem.toString().toLowerCase();
          if (v.length() > 0 && v.charAt(0) == aKey) {
            return i;
          }
        }
      }

      for (int i = 0; i < currentSelection; i++) {
        final Object elem = aModel.getElementAt(i);
        if (elem != null && elem.toString() != null) {
          final String v = elem.toString().toLowerCase();
          if (v.length() > 0 && v.charAt(0) == aKey) {
            return i;
          }
        }
      }
      return -1;
    }
  }
}
