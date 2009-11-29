package org.yawni.wn.browser;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.Map.Entry;
import javax.swing.JPanel;
import javax.swing.UIManager;

/**
 * hack to work around bug where apple.awt.brushMetalLook doesn't work for JDialog
 * courtesy http://www.davidpires.com/blog/archives/the-perils-of-java-swing-on-mac-os-x
 * doesn't work if JDialog has any components on it (they would need similar paint() overrides)
 */
class UnifiedTitleBar extends JPanel implements WindowFocusListener {
  public static final int WINDOW_IN_FOCUS = 1;
  public static final int WINDOW_OUT_OF_FOCUS = 2;
  public static final int SHEET_IN_FOCUS = 3;
  private int focusValue = WINDOW_IN_FOCUS;
  SearchFrame outer;

  UnifiedTitleBar(SearchFrame outer) {
    super();
    this.outer = outer;
//    for (final Entry entry : UIManager.getLookAndFeelDefaults().entrySet()) {
//      System.err.println("UIManager: " + entry);
//    }
  }

  public void setFocusValue(int focusValue) {
    this.focusValue = focusValue;
    repaint();
    validate();
  }

  @Override
  public void paintComponent(Graphics g) {
    final Graphics2D g2d = (Graphics2D) g;
    switch (focusValue) {
      case WINDOW_IN_FOCUS:
        GradientPaint gradient = new GradientPaint(0, 0, new Color(188, 188, 188), 0, getHeight(), new Color(155, 155, 155));
        g2d.setPaint(gradient);
        break;
      case WINDOW_OUT_OF_FOCUS:
        g2d.setColor(new Color(229, 229, 221));
        break;
      case SHEET_IN_FOCUS:
        g2d.setColor(new Color(189, 189, 189));
    }
    g2d.fillRect(0, 0, getWidth(), getHeight());
  }

  public void windowGainedFocus(WindowEvent e) {
    focusValue = WINDOW_IN_FOCUS;
  }

  public void windowLostFocus(WindowEvent e) {
    focusValue = WINDOW_OUT_OF_FOCUS;
  }
}