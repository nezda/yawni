package edu.brandeis.cs.steele.wn.browser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.plaf.basic.*;
import javax.swing.plaf.metal.*;

/**
 * ComboBox-like component control / container.
 * @author http://www.oreilly.com/catalog/swinghks/
 */
class DropDownComponent extends JComponent 
implements ActionListener, AncestorListener {
  private static final long serialVersionUID = 1L;

  protected JComponent drop_down_comp;
  protected JComponent visible_comp;
  protected JButton arrow;
  protected JWindow popup;

  /**
   * @param vcomp typically a JButton
   * @param ddcomp typically a list, but really anything will work
   */
  public DropDownComponent(final JComponent vcomp, final JComponent ddcomp) {
    this.drop_down_comp = ddcomp;
    this.visible_comp = vcomp;

    this.arrow = new JButton(new MetalComboBoxIcon());
    final Insets insets = arrow.getMargin();
    this.arrow.setMargin( new Insets(insets.top, 1, insets.bottom, 1));
    setupLayout();

    this.arrow.addActionListener(this);
    addAncestorListener(this);
  }

  protected void setupLayout() {
    final GridBagLayout gbl = new GridBagLayout();
    final GridBagConstraints c = new GridBagConstraints();
    setLayout(gbl);

    c.weightx = 1.0;  c.weighty = 1.0;
    c.gridx = 0;  c.gridy = 0;
    c.fill = c.BOTH;
    gbl.setConstraints(visible_comp,c);
    add(visible_comp);

    c.weightx = 0;
    c.gridx++;
    gbl.setConstraints(arrow,c);
    add(arrow);
  }

  public void actionPerformed(final ActionEvent evt) {
    // build popup window
    this.popup = new JWindow(getFrame(null));
    this.popup.getContentPane().add(drop_down_comp);
    this.popup.addWindowFocusListener(new WindowAdapter() {
      public void windowLostFocus(WindowEvent evt) {
        popup.setVisible(false);
      }
    });
    popup.pack();

    // show the popup window
    final Point pt = visible_comp.getLocationOnScreen();
    System.out.println("pt = " + pt);
    pt.translate(visible_comp.getWidth()-popup.getWidth(),visible_comp.getHeight());
    System.out.println("pt = " + pt);
    this.popup.setLocation(pt);
    this.popup.toFront();
    this.popup.setVisible(true);
    this.popup.requestFocusInWindow();
  }

  protected Frame getFrame(Component comp) {
    if (comp == null) {
      comp = this;
    }
    if (comp.getParent() instanceof Frame) {
      return (Frame)comp.getParent();
    }
    return getFrame(comp.getParent());
  }

  public void ancestorAdded(AncestorEvent evt) { 
    hidePopup();
  }

  public void ancestorRemoved(AncestorEvent evt) { 
    hidePopup();
  }

  public void ancestorMoved(AncestorEvent evt) {
    if (evt.getSource() != popup) {
      hidePopup();
    }
  }

  public void hidePopup() {
    if (popup != null && popup.isVisible()) {
      popup.setVisible(false);
    }
  }
}
