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
/*
 * Copyright 1998 by Oliver Steele.  You can use this software freely so long as you preserve
 * the copyright notice and this restriction, and label your changes.
 */
package edu.brandeis.cs.steele.wn.browser;

//import edu.brandeis.cs.steele.wn.DictionaryDatabase;
//import edu.brandeis.cs.steele.wn.RemoteFileManager;
//import edu.brandeis.cs.steele.wn.FileBackedDictionary;

import java.util.logging.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.*;
import java.util.prefs.*;

/**
 * A graphical interface to the WordNet online lexical database.
 *
 * <p>The GUI is loosely based on the interface to the Tcl/Tk version by David Slomin.
 *
 * <p>The browser can be invoked as follows:
 * <ul>
 *   <li> <code>java edu.brandeis.cs.steele.wn.Browser</code><br>
 *        To invoke a browser on the local database. </li>
 *
 *   <li> <code>java edu.brandeis.cs.steele.wn.Browser <var>dir</dir></code><br>
 *        To invoke a browser on a local database stored at <tt>dir</tt>. </li>
 * </ul>
 *
 * @author Oliver Steele, steele@cs.brandeis.edu
 * @version 1.0
 */
public class Browser extends JFrame {
  private static final Logger log = Logger.getLogger(Browser.class.getName());
  private static Preferences prefs = Preferences.userNodeForPackage(Browser.class);
  static {
    setSystemProperties();
  }

  private static final long serialVersionUID = 1L;
  // Check that we are on Mac OS X.  This is crucial to loading and using the OSXAdapter class.
  static final boolean MAC_OS_X = System.getProperty("os.name").toLowerCase().startsWith("mac os x");
  static final int MENU_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

  private final Application app = Application.getInstance();

  private final Dimension minSize;
  private final JMenuBar mainMenuBar;
  private final JMenu fileMenu;
  private final JMenuItem miSearch;
  private final JMenuItem miQuit;
  private final JMenu helpMenu;
  private final JMenuItem miAbout;
  private final BrowserPanel browserPanel;
  private final MoveMouseListener browserFrameMouseListener;
  private MoveMouseListener searchWindowMouseListener;
  private SearchFrame searchWindow;

  // FIXME ditch this magic number
  final Icon BLANK_ICON = new BlankIcon(14, 14);
  final int pad = 5;
  private final Border textAreaBorder;

  public Browser() {
    super(Application.getInstance().getName()+" Browser");
    // ⌾ \u233e APL FUNCTIONAL SYMBOL CIRCLE JOT
    // ⊚ \u229a CIRCLED RING OPERATOR
    // ◎ \u25ce BULLSEYE
    this.setName(Browser.class.getName());

    this.textAreaBorder = new BasicBorders.MarginBorder() {
      private static final long serialVersionUID = 1L;
      private final Insets insets = new Insets(pad, pad, pad, pad);
      @Override
      public Insets getBorderInsets(final Component c) {
        return insets;
      }
    };

    this.browserPanel = new BrowserPanel(this);
    this.add(browserPanel);
    this.browserFrameMouseListener = new MoveMouseListener(browserPanel);
    this.browserPanel.addMouseListener(browserFrameMouseListener);
    this.browserPanel.addMouseMotionListener(browserFrameMouseListener);
    this.browserPanel.wireToFrame(this);

    this.mainMenuBar = new JMenuBar();
    this.fileMenu = new JMenu("File");

    final Action searchAction = new AbstractAction("Substring Search") {
      private static final long serialVersionUID = 1L;
      private final int fake = init();
      int init() {
        final KeyStroke findKeyStroke = KeyStroke.getKeyStroke(
          KeyEvent.VK_F,
          KeyEvent.SHIFT_DOWN_MASK | MENU_MASK);
        putValue(Action.MNEMONIC_KEY, findKeyStroke.getKeyCode());
        putValue(Action.ACCELERATOR_KEY, findKeyStroke);
        putValue(Action.SMALL_ICON, BLANK_ICON);
        //putValue(Action.SMALL_ICON, browserPanel.createFindIcon(14, true));
        return 0;
      }
      public void actionPerformed(final ActionEvent evt) {
        showSearchWindow(browserPanel.getSearchText());
      }
    };
    this.miSearch = new JMenuItem(searchAction);
    this.fileMenu.add(miSearch);
    this.browserPanel.addMenuItems(this, this.fileMenu);
    this.fileMenu.addSeparator();

    final Action quitAction = new AbstractAction("Quit") {
      private static final long serialVersionUID = 1L;
      private final int fake = init();
      int init() {
        putValue(Action.MNEMONIC_KEY, KeyEvent.VK_Q);
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Q, MENU_MASK));
        putValue(Action.SMALL_ICON, BLANK_ICON);
        return 0;
      }
      public void actionPerformed(final ActionEvent evt) {
        PreferencesManager.saveSettings(Browser.this);
        System.exit(0);
      }
    };
    this.miQuit = new JMenuItem(quitAction);
    this.miQuit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, MENU_MASK));
    this.fileMenu.add(miQuit);
    this.mainMenuBar.add(fileMenu);
    this.mainMenuBar.add(Box.createHorizontalGlue());
    this.helpMenu = new JMenu("Help");
    this.mainMenuBar.add(helpMenu);

    final Action aboutAction = new AbstractAction("About") {
      private static final long serialVersionUID = 1L;
      private final int fake = init();
      int init() {
        //putValue(Action.SMALL_ICON, BLANK_ICON);
        return 0;
      }
      public void actionPerformed(final ActionEvent evt) {
        final String[] options = new String[] { "Dismiss" };
        JOptionPane.showOptionDialog(
            Browser.this,
            "<html>"+
            "<h2>JWordNet Browser</h2>"+
            "A graphical interface to the<br>"+
            "WordNet online lexical database.<br>"+
            "<br>"+
            "Version: "+app.getVersion()+" (Build "+app.getBuildNumber()+", "+app.getFormattedBuildDate()+")<br>"+
            "<br>"+
            "This Java version is by Luke Nezda and Oliver Steele.<br>"+
            "The GUI is loosely based on the Tcl/Tk interface<br>"+
            "by David Slomin and Randee Tengi.",
            "About JWordNet Browser",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null,
            options,
            options[0]);
      }
    };
    this.miAbout = new JMenuItem(aboutAction);
    this.helpMenu.add(miAbout);
    this.mainMenuBar.add(helpMenu);
    setJMenuBar(mainMenuBar);

    final WindowAdapter closer = new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent evt) {
        quitAction.actionPerformed(null);
      }
    };
    this.addWindowListener(closer);

    validate();

    this.minSize = new Dimension(getPreferredSize().width, getMinimumSize().height);
    setMinimumSize(minSize);
    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(final ComponentEvent evt) {
        int width = getWidth();
        int height = getHeight();
        // we check if either the width
        // or the height are below minimum
        boolean resize = false;
        if (width < minSize.width) {
          resize = true;
          width = minSize.width;
        }
        //if (height < minSize.height) {
        //  resize = true;
        //  height = minSize.height;
        //}
        if (resize) {
          setSize(width, height);
        }
      }
    });

    //pack();
    // centers the window
    //setLocationRelativeTo(null);
    PreferencesManager.loadSettings(this);
    //setVisible(true);

    browserPanel.debug();
  }

  private void showSearchWindow(final String searchText) {
    if (searchWindow == null) {
      searchWindow = new SearchFrame(this, browserPanel);
      searchWindowMouseListener = new MoveMouseListener(searchWindow.searchPanel());
      searchWindow.addMouseListener(searchWindowMouseListener);
      searchWindow.addMouseMotionListener(searchWindowMouseListener);
    }
    searchWindow.reposition();
    searchWindow.setSearchText(searchText);
    searchWindow.toFront();
    searchWindow.setVisible(true);
  }

  Border textAreaBorder() {
    return textAreaBorder;
  }

  /**
   * Used to make JMenuItems with and without icons lineup horizontally.
   * @author http://forum.java.sun.com/thread.jspa?threadID=303795&forumID=57
   */
  static class BlankIcon extends Object implements Icon {
    private final int h;
    private final int w;
    BlankIcon(final int h, final int w) {
      this.h = h;
      this.w = w;
    }
    public int getIconHeight() { return h; }
    public int getIconWidth() { return w; }
    public void paintIcon(Component c, Graphics g, int x, int y) { }
  } // end class BlankIcon

  private static void setSystemProperties() {
    //TODO move to preferences ?
    // these won't hurt anything on non OS X platforms
    // http://mindprod.com/jgloss/antialiasing.html#GOTCHAS
    // ? Java 5 option that may cause fonts to look worse ??
    System.setProperty("swing.aatext", "true");
    // Java 6 http://java.sun.com/javase/6/docs/technotes/guides/2d/flags.html#aaFonts
    System.setProperty("awt.useSystemAAFontSettings", "on");
    System.setProperty("apple.awt.textantialiasing", "on");
    System.setProperty("apple.laf.useScreenMenuBar", "true");
    System.setProperty("apple.awt.brushMetalLook", "true");
    System.setProperty("apple.awt.brushMetalRounded", "true");
    System.setProperty("apple.awt.showGrowBox", "false");
  }

  public static void main(final String[] args) {
    final long start = System.currentTimeMillis();
    PreferencesManager.setLookAndFeel();

    //final DictionaryDatabase dictionary;
    //String searchDir = null; // args[0]
    //if (searchDir != null) {
    //  dictionary = FileBackedDictionary.getInstance(searchDir);
    //} else {
    //  dictionary = FileBackedDictionary.getInstance();
    //}
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        new Browser().setVisible(true);
        final long guiLoadDone = System.currentTimeMillis();
        System.err.println("guiLoadTime: "+(guiLoadDone - start)+"ms");
      }
    });
  }

  //static void displayUsageError() {
  //  System.err.println("usage: Browser [searchDir]");
  //}
}
