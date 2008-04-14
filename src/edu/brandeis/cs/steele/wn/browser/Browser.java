/*
 * WordNet-Java
 *
 * Copyright 1998 by Oliver Steele.  You can use this software freely so long as you preserve
 * the copyright notice and this restriction, and label your changes.
 */
package edu.brandeis.cs.steele.wn.browser;

import edu.brandeis.cs.steele.wn.DictionaryDatabase;
import edu.brandeis.cs.steele.wn.RemoteFileManager;
import edu.brandeis.cs.steele.wn.FileBackedDictionary;

import java.util.logging.*;
import java.awt.*;
import java.awt.event.*;
import java.rmi.RMISecurityManager;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import javax.swing.*;

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

  private static final long serialVersionUID = 1L;

  private final JMenuBar mainMenuBar;
  private final JMenu fileMenu;
  private final JMenuItem miSearch;
  private final JMenuItem miQuit;
  private final JMenu helpMenu;
  private final JMenuItem miAbout;
  private final BrowserPanel browserPanel;

  private JFrame searchWindow;

  public Browser(final DictionaryDatabase dictionary) {
    super("JWordNet Browser");
    this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    this.setVisible(false);
    this.setLocation(50, 50);
    this.setSize(640, 480);

    this.browserPanel = new BrowserPanel(dictionary);
    this.add(browserPanel);
    this.browserPanel.wireToFrame(this);

    final int metaKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    this.mainMenuBar = new JMenuBar();
    this.fileMenu = new JMenu("File");
    this.miSearch = new JMenuItem("Substring Search");
    this.miSearch.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, metaKey));
    this.fileMenu.add(miSearch);
    this.browserPanel.addMenuItems(this.fileMenu);
    fileMenu.addSeparator();
    miQuit = new JMenuItem("Quit");
    miSearch.setMnemonic(KeyEvent.VK_Q);
    miQuit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, metaKey));
    fileMenu.add(miQuit);

    mainMenuBar.add(fileMenu);
    mainMenuBar.add(Box.createHorizontalGlue());
    helpMenu = new JMenu("Help");
    mainMenuBar.add(helpMenu);
    miAbout = new JMenuItem("About");
    helpMenu.add(miAbout);
    mainMenuBar.add(helpMenu);
    setJMenuBar(mainMenuBar);

    final ActionListener listener = new ActionListener() {
      public void actionPerformed(final ActionEvent event) {
        final Object object = event.getSource();
        if (object == miAbout) {
          final String[] options = new String[] {      
            "Dismiss"
          };
          JOptionPane.showOptionDialog(
              Browser.this,
              "<html>"+
              "<h2>JWordNet Browser</h2>"+
                "A graphical interface to the<br>"+
                "WordNet online lexical database.<br>"+
                "<br>"+
                "This Java version by Oliver Steele.<br>"+
                "The GUI is loosely based on the interface<br>"+
                "to the Tcl/Tk version by David Slomin and Randee Tengi.",
              "About JWordNet Browser",
              JOptionPane.DEFAULT_OPTION,
              JOptionPane.PLAIN_MESSAGE,
              null,
              options,
              options[0]);
        } else if (object == miSearch) {
          showSearchWindow();
        } else if (object == miQuit) {
          System.exit(0);
        } else {
          log.log(Level.SEVERE, "unhandled object: {0}", object);
        }
      }
    };
    miSearch.addActionListener(listener);
    miAbout.addActionListener(listener);
    miQuit.addActionListener(listener);
    
    //pack();
    setVisible(true);
  }

  private void showSearchWindow() {
    if (searchWindow == null) {
      searchWindow = new SearchFrame(browserPanel);
    }
    searchWindow.toFront();
    searchWindow.setVisible(true);
  }

  public static void main(final String[] args) {
    //System.setProperty("apple.awt.brushMetalLook", "true");
    //System.setProperty("apple.awt.brushMetalRounded", "true");
    //System.setProperty("apple.laf.useScreenMenuBar", "true");
    try {
      //UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      //UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
    } catch(Exception e) {
      System.err.println("Error setting native LAF: " + e);
    }

    DictionaryDatabase dictionary;
    String searchDir = null;
    String hostname = null;
    boolean isServer = false;

    // parse the arguments
    for (int i = 0; i < args.length; ++i) {
      if (args[i].equals("-hostname")) {
        hostname = args[++i];
      } else if (args[i].equals("-server")) {
        isServer = true;
      } else if (args[i].startsWith("-") || searchDir != null) {
        displayUsageError();
        return;
      } else {
        searchDir = args[i];
      }
    }
    if (hostname != null && isServer) {
      displayUsageError();
      return;
    }

    // create or lookup the server
    if (isServer) {
      // Install a security manager and create a registry, but ignore any errors --
      // one may already exist, and we'll just fall through to work with that one
      // (and propogate the exception that RemoteFileManager.bind throws, if it
      // doesn't exist).
      try {
        System.setSecurityManager(new RMISecurityManager());
        LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
      } catch (Exception e) { }
      try {
        if (searchDir != null) {
          new RemoteFileManager(searchDir).bind();
        } else {
          new RemoteFileManager().bind();
        }
      } catch (Exception e) {
        throw new RuntimeException(e.toString());
      }
      // Connect to that server.
      hostname = "127.0.0.1";
    }
    if (hostname != null) {
      try {
        dictionary = FileBackedDictionary.getInstance(RemoteFileManager.lookup(hostname));
      } catch (Exception e) {
        throw new RuntimeException(e.toString());
      }
    } else if (searchDir != null) {
      dictionary = FileBackedDictionary.getInstance(searchDir);
    } else {
      dictionary = FileBackedDictionary.getInstance();
    }
    new Browser(dictionary).setVisible(true);
  }

  static void displayUsageError() {
    System.err.println("usage: Browser [-hostname | -server] [searchDir]");
  }
}
