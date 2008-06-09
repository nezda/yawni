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
import java.rmi.RMISecurityManager;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.*;
import java.util.prefs.*;
import java.lang.reflect.*;

/** A graphical interface to the WordNet online lexical database.
 *
 * <p>The GUI is loosely based on the interface to the Tcl/Tk version by David Slomin.
 *
 * <p>The browser can be invoked as follows:
 * <dl>
 *   <dt><code>java edu.brandeis.cs.steele.wn.Browser</code>
 *   <dt>To invoke a browser on the local database.
 *
 *   <dt><code>java edu.brandeis.cs.steele.wn.Browser <var>dir</dir></code>
 *   <dt>To invoke a browser on a local database stored at <var>dir</dir>.
 *
 *   <dt><code>java edu.brandeis.cs.steele.wn.Browser -hostname <var>hostname</var></code>
 *   <dt>To invoke a browser that's served by a <code>RemoteFileManager</code> on <var>hostname</var>.
 *       See {@link edu.brandeis.cs.steele.wn.RemoteFileManager RemoteFileManager}.
 *
 *   <dt><code>java edu.brandeis.cs.steele.wn.Browser -server</code>
 *   <dt>To create a <code>RemoteFileManager</code> and connect to it using the RMI interface.
 *       This is useful for testing.
 * </dl>
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
  final Border textAreaBorder;

  public Browser() {
    super("JWordNet Browser");
    // ⌾ \u233e APL FUNCTIONAL SYMBOL CIRCLE JOT
    // ⊚ \u229a CIRCLED RING OPERATOR
    // ◎ \u25ce BULLSEYE
    this.setName(Browser.class.getName());
    //FIXME this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    //XXX this.setVisible(false);
    //XXX this.setSize(640, 480);
    //XXX Toolkit.getDefaultToolkit().setDynamicLayout(true);
    //XXX System.err.println("dynLayout: "+Toolkit.getDefaultToolkit().isDynamicLayoutActive());
    
    this.textAreaBorder = new BasicBorders.MarginBorder() {
      private static final long serialVersionUID = 1L;
      private final Insets insets = new Insets(pad, pad, pad, pad);
      @Override public Insets getBorderInsets(final Component c) {
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
        putValue(Action.MNEMONIC_KEY, KeyEvent.VK_F);
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F, MENU_MASK));
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
        WindowSaver.saveSettings(Browser.this);
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
            "This Java version is by Luke Nezda and Oliver Steele.<br>"+
            "The GUI is loosely based on Tcl/Tk interface<br>"+
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
      @Override public void windowClosing(final WindowEvent evt) {
        System.err.println("closing");
        quitAction.actionPerformed(null);
      }
    };
    this.addWindowListener(closer);

    validate();

    this.minSize = new Dimension(getPreferredSize().width, getMinimumSize().height);
    setMinimumSize(minSize);
    addComponentListener(new ComponentAdapter() {
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
    WindowSaver.loadSettings(this);
    //setVisible(true);

    browserPanel.debug();
  }

  private void showSearchWindow(final String searchText) {
    if (searchWindow == null) {
      searchWindow = new SearchFrame(browserPanel);
      searchWindowMouseListener = new MoveMouseListener(searchWindow.searchPanel);
      searchWindow.addMouseListener(searchWindowMouseListener);
      searchWindow.addMouseMotionListener(searchWindowMouseListener);
    }
    searchWindow.reposition();
    searchWindow.setSearchText(searchText);
    searchWindow.toFront();
    searchWindow.setVisible(true);
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
    System.setProperty("swing.aatext", "true");
    System.setProperty("awt.useSystemAAFontSettings", "on");
    System.setProperty("apple.awt.textantialiasing", "on");
    System.setProperty("apple.laf.useScreenMenuBar", "true");
    System.setProperty("apple.awt.brushMetalLook", "true");
    System.setProperty("apple.awt.brushMetalRounded", "true"); 
    System.setProperty("apple.awt.showGrowBox", "false");
  }

  public static void main(final String[] args) {
    final long start = System.currentTimeMillis();
    WindowSaver.setLookAndFeel();

    //final DictionaryDatabase dictionary;
    //String searchDir = null;
    //String hostname = null;
    //boolean isServer = false;

    //// parse the arguments
    //for (int i = 0; i < args.length; ++i) {
    //  if (args[i].equals("-hostname")) {
    //    hostname = args[++i];
    //  } else if (args[i].equals("-server")) {
    //    isServer = true;
    //  } else if (args[i].startsWith("-") || searchDir != null) {
    //    displayUsageError();
    //    return;
    //  } else {
    //    searchDir = args[i];
    //  }
    //}
    //if (hostname != null && isServer) {
    //  displayUsageError();
    //  return;
    //}

    //// create or lookup the server
    //if (isServer) {
    //  // Install a security manager and create a registry, but ignore any errors --
    //  // one may already exist, and we'll just fall through to work with that one
    //  // (and propagate the exception that RemoteFileManager.bind throws, if it
    //  // doesn't exist).
    //  try {
    //    System.setSecurityManager(new RMISecurityManager());
    //    LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
    //  } catch (Exception e) { }
    //  try {
    //    if (searchDir != null) {
    //      new RemoteFileManager(searchDir).bind();
    //    } else {
    //      new RemoteFileManager().bind();
    //    }
    //  } catch (Exception e) {
    //    throw new RuntimeException(e.toString());
    //  }
    //  // Connect to that server.
    //  hostname = "127.0.0.1";
    //}
    //if (hostname != null) {
    //  try {
    //    dictionary = FileBackedDictionary.getInstance(RemoteFileManager.lookup(hostname));
    //  } catch (Exception e) {
    //    throw new RuntimeException(e.toString());
    //  }
    //} else if (searchDir != null) {
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
  //  System.err.println("usage: Browser [-hostname | -server] [searchDir]");
  //}
}
