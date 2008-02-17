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

/** A graphical interface to the WordNet online lexical database.
 *
 * <P>The GUI is loosely based on the interface to the Tcl/Tk version by David Slomin.
 *
 * <P>The browser can be invoked as follows:<DL>
 * <DT><CODE>java edu.brandeis.cs.steele.wn.Browser</CODE>
 * <DE>To invoke a browser on the local database.
 *
 * <DT><CODE>java edu.brandeis.cs.steele.wn.Browser <var>dir</dir></CODE>
 * <DE>To invoke a browser on a local database stored at <var>dir</dir>.
 *
 * <DT><CODE>java edu.brandeis.cs.steele.wn.Browser -hostname <var>hostname</var></CODE>
 * <DE>To invoke a browser that's served by a <code>RemoteFileManager</code> on <var>hostname</var>.
 *     See {@link edu.brandeis.cs.steele.wn.RemoteFileManager RemoteFileManager}.
 *
 * <DT><CODE>java edu.brandeis.cs.steele.wn.Browser -server</CODE>
 * <DE>To create a <code>RemoteFileManager</code> and connect to it using the RMI interface.
 *      This is useful for testing.
 * </DL>
 *
 * @author Oliver Steele, steele@cs.brandeis.edu
 * @version 1.0
 */
public class Browser extends Frame {
	protected MenuBar mainMenuBar;
	protected Menu fileMenu;
	protected MenuItem miSearch;
	protected MenuItem miExit;
	protected Menu editMenu;
	protected MenuItem miCut;
	protected MenuItem miCopy;
	protected MenuItem miPaste;
	protected Menu helpMenu;
	protected MenuItem miAbout;

	protected Frame searchWindow;
	
	public Browser(DictionaryDatabase dictionary) {
		super("WordNet Browser");
		setVisible(false);
		setLocation(50, 50);
		setSize(500, 400);
		
		final BrowserPanel browser = new BrowserPanel(dictionary);
		add(browser);
		
		mainMenuBar = new MenuBar();
		fileMenu = new Menu("File");
		miSearch = new MenuItem("Substring Search");
		fileMenu.add(miSearch);
		fileMenu.addSeparator();
		miExit = new MenuItem("Exit");
		fileMenu.add(miExit);
		
		mainMenuBar.add(fileMenu);
		editMenu = new Menu("Edit");
		miCut = new MenuItem("Cut");
		miCut.setShortcut(new MenuShortcut(KeyEvent.VK_X,false));
		editMenu.add(miCut);
		miCopy = new MenuItem("Copy");
		miCopy.setShortcut(new MenuShortcut(KeyEvent.VK_C,false));
		editMenu.add(miCopy);
		miPaste = new MenuItem("Paste");
		miPaste.setShortcut(new MenuShortcut(KeyEvent.VK_V,false));
		editMenu.add(miPaste);
		mainMenuBar.add(editMenu);
		helpMenu = new Menu("Help");
		mainMenuBar.setHelpMenu(helpMenu);
		miAbout = new java.awt.MenuItem("About..");
		helpMenu.add(miAbout);
		mainMenuBar.add(helpMenu);
		setMenuBar(mainMenuBar);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent event) {
				setVisible(false);
				dispose();
			}
		});

		ActionListener listener = new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				Object object = event.getSource();
				Frame parent = Browser.this;
				if (object == miAbout) {
					new AboutDialog(parent);
				} else if (object == miSearch) {
					if (searchWindow == null) {
						searchWindow = new SearchFrame(browser);
					}
					searchWindow.toFront();
					searchWindow.show();
				} else if (object == miExit) {
					new QuitDialog(parent, true).setVisible(true);
				}
			}
		};
		miSearch.addActionListener(listener);
		miAbout.addActionListener(listener);
		miExit.addActionListener(listener);

		//setSize(getPreferredSize().width, getPreferredSize().height);
		setVisible(true);
	}

	static public void main(String[] args) {
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
				dictionary = new FileBackedDictionary(RemoteFileManager.lookup(hostname));
			} catch (Exception e) {
				throw new RuntimeException(e.toString());
			}
		} else if (searchDir != null) {
 			dictionary = new FileBackedDictionary(searchDir);
 		} else {
 			dictionary = new FileBackedDictionary();
		}
		new Browser(dictionary).setVisible(true);
	}

	static protected void displayUsageError() {
		System.err.println("usage: Browser [-hostname | -server] [searchDir]");
	}
}
