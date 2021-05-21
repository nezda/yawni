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

import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yawni.wordnet.WordNetInterface.WordNetVersion;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.security.AccessControlException;
import java.util.Vector;
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.HyperlinkEvent;
import javax.swing.plaf.basic.BasicBorders;

/**
 * A graphical interface to the WordNet online lexical database.
 *
 * <p> The GUI is loosely based on the interface to the Tcl/Tk version by David Slomin.
 *
 * <p> The browser can be invoked as follows:
 * <ul>
 *   <li> {@code java org.yawni.wordnet.browser.Browser}<br>
 *        To invoke a browser on the local database. </li>
 *
 *   <li> {@code java org.yawni.wordnet.browser.Browser <dir>}<br>
 *        To invoke a browser on a local database stored at {@code <dir>}. </li>
 * </ul>
 */
class Browser extends JFrame implements Thread.UncaughtExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(Browser.class.getName());
  //private static Preferences prefs = Preferences.userNodeForPackage(Browser.class);
  static {
    setSystemProperties();
  }

  // see if we're Mac OS X; crucial to loading and using the OSXAdapter class
  static final boolean IS_MAC_OS_X = System.getProperty("os.name").toLowerCase().startsWith("mac os x");
  static final int MENU_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

  private final Application app = Application.getInstance();

  private final Dimension minSize;
  private final JMenuBar mainMenuBar;
  private final JMenu fileMenu;
  private JMenu viewMenu;
  private final JMenu helpMenu;
  private final BrowserPanel browserPanel;
  private final MoveMouseListener browserFrameMouseListener;
  private MoveMouseListener searchWindowMouseListener;
  private SearchFrame searchWindow;

  // FIXME ditch this magic number
  private static final Icon BLANK_ICON = new BlankIcon(14, 14);
  private final int pad = 5;
  private final Border textAreaBorder;

  Browser() {
    this(0);
  }

  Browser(int browserNumber) {
    super(Application.getInstance().getName() + " Browser");
    this.setName(super.getName() + "-" + BROWSERS.size());
    // ⌾ \u233e APL FUNCTIONAL SYMBOL CIRCLE JOT
    // ⊚ \u229a CIRCLED RING OPERATOR
    // ◎ \u25ce BULLSEYE
    this.setName(getClass().getName());
    // this is the preferred way to set brushMetalRounded
    // http://lists.apple.com/archives/Java-dev/2007/Nov/msg00081.html
    this.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
    // allows drags to switch OS X Spaces, but also makes whole Window draggable which
    // is weird; discussed here (esp. the comments):
    // https://explodingpixels.wordpress.com/2008/05/03/sexy-swing-app-the-unified-toolbar-now-fully-draggable/
    //getRootPane().putClientProperty("apple.awt.draggableWindowBackground", Boolean.TRUE);

    if (getAppIcon() != null) {
      this.setIconImage(getAppIcon().getImage());
    }

    this.textAreaBorder = new BasicBorders.MarginBorder() {
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

    final Action newWindowAction = new AbstractAction("New Window") {
      private final int fake = init();
      int init() {
        final KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_N, MENU_MASK);
        putValue(Action.MNEMONIC_KEY, keyStroke.getKeyCode());
        putValue(Action.ACCELERATOR_KEY, keyStroke);
        putValue(Action.SMALL_ICON, BLANK_ICON);
        return 0;
      }
      public void actionPerformed(final ActionEvent evt) {
        newWindow();
      }
    };
    this.fileMenu.add(new JMenuItem(newWindowAction));

    final Action searchAction = new AbstractAction("Substring Search") {
      private final int fake = init();
      int init() {
        final KeyStroke keyStroke = KeyStroke.getKeyStroke(
          KeyEvent.VK_F,
          KeyEvent.SHIFT_DOWN_MASK | MENU_MASK);
        putValue(Action.MNEMONIC_KEY, keyStroke.getKeyCode());
        putValue(Action.ACCELERATOR_KEY, keyStroke);
        putValue(Action.SMALL_ICON, BLANK_ICON);
        putValue(Action.SMALL_ICON, BrowserPanel.createFindIcon(14));
        return 0;
      }
      public void actionPerformed(final ActionEvent evt) {
        showSearchWindow(browserPanel.getSearchText());
      }
    };
    this.fileMenu.add(new JMenuItem(searchAction));

    this.browserPanel.addMenuItems(this, this.fileMenu);
    this.fileMenu.addSeparator();

    final Action closeWindowAction = new AbstractAction("Close") {
      private final int fake = init();
      int init() {
        //putValue(Action.MNEMONIC_KEY, KeyEvent.VK_W);
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_W, MENU_MASK));
        putValue(Action.SMALL_ICON, BLANK_ICON);
        return 0;
      }
      public void actionPerformed(final ActionEvent evt) {
        quitAction(Browser.this);
      }
    };
    this.fileMenu.add(new JMenuItem(closeWindowAction));

    // on non-OS X, quit goes on File menu, About goes on Help menu

    final Action quitAction = new AbstractAction("Quit") {
      private final int fake = init();
      int init() {
        putValue(Action.MNEMONIC_KEY, KeyEvent.VK_Q);
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Q, MENU_MASK));
        putValue(Action.SMALL_ICON, BLANK_ICON);
        return 0;
      }
      public void actionPerformed(final ActionEvent evt) {
        quitAction(Browser.this);
      }
    };
    this.fileMenu.add(new JMenuItem(quitAction));
    this.mainMenuBar.add(fileMenu);

//    installViewMenu();

    this.mainMenuBar.add(Box.createHorizontalGlue());

    this.helpMenu = new JMenu("Help");
    this.mainMenuBar.add(helpMenu);

    final Action aboutAction = new AbstractAction("About") {
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
          if (key.contains("awt.")) {
            continue;
          }
          if (key.contains("sun.")) {
            continue;
          }
          if (key.contains("apple.")) {
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
            "<br>"+
            "This Java version is by Luke Nezda and Oliver Steele.<br>"+
            "The GUI is loosely based on the Tcl/Tk interface<br>"+
            "by David Slomin and Randee Tengi.<br>"+
            "<br>"+
            "Learn more at <a href=\"https://www.yawni.org\">https://www.yawni.org</a><br>"+
            "<br>";
        final JEditorPane descriptionPane = new JEditorPane("text/html", description);
        descriptionPane.setEditable(false);
        descriptionPane.setOpaque(false);
        descriptionPane.addHyperlinkListener(e -> {
          if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            if (Desktop.isDesktopSupported()) {
              try {
                Desktop.getDesktop().browse(e.getURL().toURI());
              } catch (IOException | URISyntaxException ex) {
                log.error("broken url: {}", e.getURL(), ex);
              }
            }
          }
        });
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
              " WordNetVersion: "+WordNetVersion.detect()+
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
            new Object[] { descriptionPane, info }, // message
            "About", // title
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            getAppIcon(),
            options,
            options[0]);
      }
    };
    this.helpMenu.add(new JMenuItem(aboutAction));
    this.mainMenuBar.add(helpMenu);
    SwingUtilities.invokeLater(() -> setJMenuBar(mainMenuBar));

    final WindowAdapter closer = new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent evt) {
        quitAction.actionPerformed(null);
      }
    };
    this.addWindowListener(closer);
    final WindowFocusListener windowFocusListener = new WindowAdapter() {
      @Override
      public void windowLostFocus(final WindowEvent evt) {
        // tell BrowserPanel to close any open popups
        // work around Sun bug http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4812585
        // effecting heavyweight JPopupMenus
        log.debug("evt: {}", evt);
        // almost works except if Window focus goes from:
        //   BrowserPanel → SearchFrame → <another app>
        // popup stays visible; ideally we'd have some notion of "application" focus
//        if (evt.getOppositeWindow() == null) {
//          browserPanel.dismissPOSComboBoxPopup();
//        }
        browserPanel.dismissPOSComboBoxPopup();
      }
    };
    this.addWindowFocusListener(windowFocusListener);

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

    browserPanel.debug();
  }

  //move to Application
  private static ImageIcon APP_ICON;

  private static ImageIcon getAppIcon() {
    if (APP_ICON == null) {
      try {
        //APP_ICON = new ImageIcon(Browser.class.getResource("yawni_57x64_icon.png"));
        APP_ICON = new ImageIcon(Browser.class.getResource("yawni_115x128_icon.png"));
      } catch (NullPointerException npe) {
        log.warn("can't find icon", npe);
      }
    }
    return APP_ICON;
  }

  private static final Vector<Browser> BROWSERS = new Vector<>();

  private static synchronized void newWindow() {
    final Browser browser = new Browser(BROWSERS.size());
    Thread.setDefaultUncaughtExceptionHandler(browser);
    BROWSERS.add(browser);
    PreferencesManager.loadSettings(browser);
    browser.setVisible(true);
  }

  private static synchronized void quitAction(final Browser thiz) {
    final boolean removed = BROWSERS.remove(thiz);
    assert removed;
    PreferencesManager.saveSettings(thiz);
    thiz.setVisible(false);
    thiz.dispose();
    if (BROWSERS.isEmpty()) {
      System.exit(0);
    }
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

//  public void uncaughtException(Thread t, Throwable e) {
//    System.err.format("caught %s on %s. rethrowing...\n", t, e);
//    log.error("caught {} on {}. rethrowing...", t, e);
//    throw new RuntimeException(e);
//  }

  // Exception that has been thrown. This is used to track if an exception
  // is thrown while alerting the user to the current exception.
  private Throwable throwable = null;

  /**
   * Invoked when an uncaught exception is encountered.  This will
   * show a modal dialog alerting the user, and exit the app. This does
   * <b>not</b> invoke {@code exit}.
   *
   * @param thread the thread the exception was thrown on
   * @param throwable the thrown exception
   * @see #getUncaughtExceptionDialog
   */
  public void uncaughtException(final Thread thread, final Throwable throwable) {
    log.error("uncaughtException on {}", thread, throwable);
    synchronized (this) {
      if (this.throwable != null) {
        log.error("doh! An exception was thrown while reporting an earlier one. exiting immediately...",
          throwable);
        System.exit(1);
      } else {
        this.throwable = throwable;
      }
    }
    uncaughtException0();
  }

  /**
   * Returns the dialog that is shown when an uncaught exception is
   * encountered.
   *
   * @see #uncaughtException
   * @return dialog to show when an uncaught exception is encountered
   */
  private JDialog getUncaughtExceptionDialog(final Throwable t) {
    final JOptionPane optionPane = new JOptionPane(
      new Object[] {
        "An unrecoverable error has occurred.",
        getName() + " will now exit.",
        scrollableStackTrace(t)
      },
      JOptionPane.ERROR_MESSAGE);
    return optionPane.createDialog(this, "Error");
  }

  private static JComponent scrollableStackTrace(final Throwable t) {
    final JTextArea messagePane = new JTextArea();
    messagePane.setLineWrap(false);
    messagePane.setEditable(false);
    final StringWriter stringWriter =  new StringWriter();
    final PrintWriter printWriter = new PrintWriter(stringWriter, true);
    t.printStackTrace(printWriter);
    messagePane.setText(stringWriter.toString());
    messagePane.setCaretPosition(0); // scroll to top
    return new JScrollPane(messagePane);
  }

  private void uncaughtException0() {
    Throwable throwable;
    synchronized (this) {
      throwable = this.throwable;
    }
    log.error("uncaughtException0() caught", throwable);
    final JDialog dialog = getUncaughtExceptionDialog(throwable);
    dialog.setSize(new Dimension(600, 400));
    dialog.setResizable(true);
    dialog.setLocationRelativeTo(null);
    dialog.setVisible(true);
    System.exit(1);
  }

  // TODO implement these as bound properties & initialize variables from saved Preferences
  private void installViewMenu() {
    final JMenu viewMenu = new JMenu("View");

    viewMenu.add(new JMenuItem("Show frequency counts"));
    final AbstractButton freqShow = viewMenu.add(new JRadioButtonMenuItem("Show"));
    final AbstractButton freqHide = viewMenu.add(new JRadioButtonMenuItem("Hide"));
    final ButtonGroup freqGroup = new ButtonGroup();
    freqGroup.add(freqShow);
    freqGroup.add(freqHide);


    this.viewMenu.addSeparator();
    final AbstractButton lexLabel = this.viewMenu.add(new JMenuItem("Lexical file information"));
//    UIManager.put("Menu.font", XXX);
//    lexLabel.setSelected(false);
//    lexLabel.setFocusPainted(false);
//    lexLabel.setBorderPainted(false);
//    lexLabel.setContentAreaFilled(false);
    final AbstractButton lexShow = this.viewMenu.add(new JRadioButtonMenuItem("Show"));
    final AbstractButton lexHide = this.viewMenu.add(new JRadioButtonMenuItem("Hide"));
    final ButtonGroup lexGroup = new ButtonGroup();
    lexGroup.add(lexShow);
    lexGroup.add(lexHide);


    this.viewMenu.addSeparator();
    this.viewMenu.add(new JMenuItem("Synset database locations"));
    final AbstractButton locShow = this.viewMenu.add(new JRadioButtonMenuItem("Show"));
    final AbstractButton locHide = this.viewMenu.add(new JRadioButtonMenuItem("Hide"));
    final ButtonGroup locGroup = new ButtonGroup();
    locGroup.add(locShow);
    locGroup.add(locHide);


    this.viewMenu.addSeparator();
    this.viewMenu.add(new JMenuItem("Sense numbers"));
    final AbstractButton numShow = this.viewMenu.add(new JRadioButtonMenuItem("Show"));
    final AbstractButton numHide = this.viewMenu.add(new JRadioButtonMenuItem("Hide"));
    final ButtonGroup numGroup = new ButtonGroup();
    numGroup.add(numShow);
    numGroup.add(numHide);

    this.mainMenuBar.add(viewMenu);
  }

  /**
   * Used to make JMenuItems with and without icons lineup horizontally.
   * @author https://forum.java.sun.com/thread.jspa?threadID=303795&forumID=57
   */
  static class BlankIcon implements Icon {
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
    try {
      //TODO move to preferences ?
      // these won't hurt anything on non OS X platforms
      // https://mindprod.com/jgloss/antialiasing.html#GOTCHAS
      // ? Java 5 option that may cause fonts to look worse ??
      System.setProperty("swing.aatext", "true");
      // Java 6 https://java.sun.com/javase/6/docs/technotes/guides/2d/flags.html#aaFonts
      System.setProperty("awt.useSystemAAFontSettings", "on");
      System.setProperty("apple.awt.textantialiasing", "on");
      System.setProperty("apple.laf.useScreenMenuBar", "true");
      System.setProperty("apple.awt.brushMetalLook", "true");
      System.setProperty("apple.awt.brushMetalRounded", "true");
      System.setProperty("apple.awt.showGrowBox", "false");
    } catch (AccessControlException ace) {
      log.warn("can't set system properties :(", ace);
    }
  }

  public static void main(final String[] args) {
    try {
      System.setProperty("java.security.debug", "all");
    } catch (AccessControlException ace) {
      log.warn("can't set system properties :(", ace);
    }

    final long start = System.currentTimeMillis();
    try {
      PreferencesManager.setLookAndFeel();
    } catch (AccessControlException ace) {
      log.warn("can't PreferencesManager.setLookAndFeel() :(", ace);
    }

    //final WordNetInterface wn;
    //String searchDir = null; // args[0]
    //if (searchDir != null) {
    //  wn = WordNet.getInstance(searchDir);
    //} else {
    //  wn = WordNet.getInstance();
    //}
    SwingUtilities.invokeLater(() -> {
      newWindow();
      final long guiLoadDone = System.currentTimeMillis();
      System.err.println("guiLoadTime: "+(guiLoadDone - start)+"ms");
    });
  }
}