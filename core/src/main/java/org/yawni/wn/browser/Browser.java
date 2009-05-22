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
package org.yawni.wn.browser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.*;
import java.util.prefs.*;

/**
 * A graphical interface to the WordNet online lexical database.
 *
 * <p> The GUI is loosely based on the interface to the Tcl/Tk version by David Slomin.
 *
 * <p> The browser can be invoked as follows:
 * <ul>
 *   <li> {@code java org.yawni.wn.Browser}<br>
 *        To invoke a browser on the local database. </li>
 *
 *   <li> {@code java org.yawni.wn.Browser <dir>}<br>
 *        To invoke a browser on a local database stored at {@code <dir>}. </li>
 * </ul>
 */
public class Browser extends JFrame {
  private static final Logger log = LoggerFactory.getLogger(Browser.class.getName());
  private static Preferences prefs = Preferences.userNodeForPackage(Browser.class);
  static {
    setSystemProperties();
  }

  private static final long serialVersionUID = 1L;
  // Check that we are on Mac OS X.  This is crucial to loading and using the OSXAdapter class.
  static final boolean IS_MAC_OS_X = System.getProperty("os.name").toLowerCase().startsWith("mac os x");
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
    super(Application.getInstance().getName() + " Browser");
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
      // configuration items represented as System Properties
      private String nonStandardSystemProperties() {
        StringBuilder buffer = new StringBuilder();
        for (final Object key_: System.getProperties().keySet()) {
          final String key = (String) key_;
          if (key.startsWith("java.")) {
            continue;
          }
          if (key.startsWith("user.")) {
            continue;
          }
          if (key.startsWith("os.")) {
            continue;
          }
          if (key.endsWith(".separator")) {
            continue;
          }
          if (key.indexOf("awt.") >= 0) {
            continue;
          }
          if (key.indexOf("sun.") >= 0) {
            continue;
          }
          if (key.indexOf("apple.") >= 0) {
            continue;
          }
          if (key.equals("gopherProxySet")) {
            continue;
          }
          if (key.equals("file.encoding.pkg")) {
            continue;
          }
          final String value = System.getProperty(key);
          buffer.append(key);
          buffer.append('=');
          buffer.append(value);
          buffer.append(";<br> ");
        }
        return buffer.toString();
      }
      private String systemProperties() {
        final StringBuilder buffer = new StringBuilder();
        //FIXME most of this logic should be in FileManager
        // check WNHOME (env and System Property)
        // check WNSEARCHDIR (env and System Property)
        buffer.append("<br> ");
        return buffer.toString();
      }
      public void actionPerformed(final ActionEvent evt) {
        final String description =
           "<html>"+
            "<h2>"+app.getName()+" Browser</h2>"+
            "A graphical interface to the "+
            "WordNet online lexical database.<br>"+ //TODO would be cool if this were a live hyperlink
            "<br>" +
            "This Java version is by Luke Nezda and Oliver Steele.<br>"+
            "The GUI is loosely based on the Tcl/Tk interface<br>"+
            "by David Slomin and Randee Tengi.<br>"+
            "<br>";
        // JLabel text cannot be selected with the mouse, so we use JEditorPane
        // format with table mainly so copy + paste will include newlines between rows
        // FIXME increase white space/padding around the edges
        final JEditorPane info = new JEditorPane("text/html",
            "<table cellpadding=\"1\">" +
            "<tr><td>"+
              "<b>Version:</b> "+app.getVersion()+" (Build "+app.getBuildNumber()+/*", "+app.getFormattedBuildDate()+*/")"+
            "</td></tr>"+
            "<tr><td>"+
              "<b>WNHOME:</b> env: "+System.getenv("WNHOME")+
              " prop: "+System.getProperty("WNHOME")+ //TODO turn red if this is set but doesn't exist
            "</td></tr>"+
            "<tr><td>"+
              "<b>WNSEARCHDIR:</b> env: "+System.getenv("WNSEARCHDIR")+
                " prop: "+System.getProperty("WNSEARCHDIR")+ //TODO turn red if this is set but doesn't exist
            "</td></tr>"+
            //TODO report WNSEARCHDIR (including red if set but doesn't exist like WNHOME)
            //TODO indicate if we loaded the data from a jar
            "<tr><td>"+
              "<b>Java:</b> "+System.getProperty("java.version")+"; "+
                System.getProperty("java.vm.name")+" "+System.getProperty("java.vm.version")+
            "</td></tr>"+
            "<tr><td>"+
              "<b>System:</b> "+System.getProperty("os.name")+" version "+System.getProperty("os.version")+
                " running on "+System.getProperty("os.arch")+
            // CLASSPATH is typically really long - would require scroll capability
//            "<b>Classpath:</b> "+System.getProperty("java.class.path")+"<br>"+
              // which properties do we care about anyway?
//            "<b>System properties:</b> "+nonStandardSystemProperties()+"<br>"+ // TODO almost justifies a JScrollPane
            "</td></td>"+
            "</table>"
          );
        //info.setFont(new JLabel(" ").getFont());
        info.setEditable(false);
        info.setBackground(Color.WHITE);
        info.setOpaque(true);
        info.setBorder(BorderFactory.createEtchedBorder());
        final String[] options = new String[] { "Dismiss" };
          JOptionPane.showOptionDialog(
            Browser.this,
            new Object[] { description, info }, // message
            "About", // title
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
}
