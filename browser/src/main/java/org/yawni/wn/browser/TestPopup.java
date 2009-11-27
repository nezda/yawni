package org.yawni.wn.browser;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;

/**
 * Demonstrates programmatic {@link JMenuItem} selection;
 * specifically how to make the first item selected by default
 */
public class TestPopup extends JFrame {
  public static void main(String[] args) {
    final JFrame frame = new JFrame("TestPopup");
    frame.setSize(640, 480);
    frame.getContentPane().addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) {
          popupTriggered(e);
        }
      }
      private void popupTriggered(MouseEvent e) {
        final JPopupMenu menu = new JPopupMenu();
        final JMenuItem item0 = new JMenuItem("JMenuItem 0");
        final JMenuItem item1 = new JMenuItem("JMenuItem 1");
        menu.add(item0);
        menu.add(item1);
        menu.pack();
        // use invokeLater or just do this after the menu has been shown
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            MenuSelectionManager.defaultManager().setSelectedPath(new MenuElement[]{menu, item0});
          }
        });
        int x = (int) ((int) (frame.getSize().width - (menu.getPreferredSize().width / 2.)) / 2.);
        int y = (int) ((int) (frame.getSize().height - (menu.getPreferredSize().height / 2.)) / 2.);
        menu.show(frame, x, y);
        // doesn't work:
        //item0.setSelected(true);
        // doesn't work:
        //menu.getSelectionModel().setSelectedIndex(0);
        // bingo; see also MenuKeyListener / MenuKeyEvent
//        MenuSelectionManager.defaultManager().setSelectedPath(new MenuElement[]{menu, item0});
      }
    });
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }
}