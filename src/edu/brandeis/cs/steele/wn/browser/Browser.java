/*
 * WordNet-Java
 *
 * Copyright 1998 by Oliver Steele.  You can use this software freely so long as you preserve
 * the copyright notice and this restriction, and label your changes.
 */
//package edu.brandeis.cs.steele.wn.browser;
package browser;

import edu.brandeis.cs.steele.wn.DictionaryDatabase;
import edu.brandeis.cs.steele.wn.RemoteFileManager;
import edu.brandeis.cs.steele.wn.FileBackedDictionary;

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
  protected JMenuBar mainMenuBar;
  protected JMenu fileMenu;
  protected JMenuItem miSearch;
  protected JMenuItem miQuit;
  protected JMenu editMenu;
  protected JMenuItem miCut;
  protected JMenuItem miCopy;
  protected JMenuItem miPaste;
  protected JMenu helpMenu;
  protected JMenuItem miAbout;

  protected JFrame searchWindow;

  public Browser(DictionaryDatabase dictionary) {
    super("JWordNet Browser");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setVisible(false);
    setLocation(50, 50);
    setSize(640, 480);

    final BrowserPanel browser = new BrowserPanel(dictionary);
    add(browser);

    mainMenuBar = new JMenuBar();
    fileMenu = new JMenu("File");
    miSearch = new JMenuItem("Substring Search");
    miSearch.setMnemonic(KeyEvent.VK_F);
    fileMenu.add(miSearch);
    fileMenu.addSeparator();
    miQuit = new JMenuItem("Quit");
    miQuit.setMnemonic(KeyEvent.VK_Q);
    fileMenu.add(miQuit);

    mainMenuBar.add(fileMenu);
    editMenu = new JMenu("Edit");
    miCut = new JMenuItem("Cut");
    miCut.setMnemonic(KeyEvent.VK_X);
    editMenu.add(miCut);
    miCopy = new JMenuItem("Copy");
    miCopy.setMnemonic(KeyEvent.VK_C);
    editMenu.add(miCopy);
    miPaste = new JMenuItem("Paste");
    miPaste.setMnemonic(KeyEvent.VK_V);
    editMenu.add(miPaste);
    mainMenuBar.add(editMenu);
    mainMenuBar.add(Box.createHorizontalGlue());
    helpMenu = new JMenu("Help");
    mainMenuBar.add(helpMenu);
    miAbout = new JMenuItem("About");
    helpMenu.add(miAbout);
    mainMenuBar.add(helpMenu);
    setJMenuBar(mainMenuBar);

    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent event) {
        setVisible(false);
        dispose();
      }
     });

    ActionListener listener = new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        Object object = event.getSource();
        JFrame parent = Browser.this;
        if (object == miAbout) {
          new AboutDialog(parent);
        } else if (object == miSearch) {
          if (searchWindow == null) {
            searchWindow = new SearchFrame(browser);
          }
          searchWindow.toFront();
          searchWindow.setVisible(true);
        } else if (object == miQuit) {
          new QuitDialog(parent, true).setVisible(true);
        }
      }
    };
    miSearch.addActionListener(listener);
    miAbout.addActionListener(listener);
    miQuit.addActionListener(listener);

    //setSize(getPreferredSize().width, getPreferredSize().height);
    setVisible(true);
  }

  public static void main(final String[] args) {
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

  static protected void displayUsageError() {
    System.err.println("usage: Browser [-hostname | -server] [searchDir]");
  }

}
