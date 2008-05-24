/*
 * WordNet-Java
 *
 * Copyright 1998 by Oliver Steele.  You can use this software freely so long as you preserve
 * the copyright notice and this restriction, and label your changes.
 */
package edu.brandeis.cs.steele.wn.browser;

import javax.swing.JApplet;
import java.util.logging.*;
import java.net.URL;

import edu.brandeis.cs.steele.wn.DictionaryDatabase;
import edu.brandeis.cs.steele.wn.FileBackedDictionary;
import edu.brandeis.cs.steele.wn.RemoteFileManager;


public class BrowserApplet extends JApplet {
private static final long serialVersionUID = 1L;
private static final Logger log = Logger.getLogger(BrowserApplet.class.getName());

  public void init() {
    final URL url = getCodeBase();
    if (log.isLoggable(Level.FINEST)) {
      log.finest("url = "+url);
    }
    String hostname = url.getHost();
    if (url.getPort() != -1) {
      hostname += ":" + url.getPort();
    }
    if (log.isLoggable(Level.FINEST)) {
      log.finest("hostname = "+hostname);
    }
    final DictionaryDatabase dictionary;
    try {
      dictionary = FileBackedDictionary.getInstance(RemoteFileManager.lookup(hostname));
    } catch (Exception e) {
      throw new RuntimeException(e.toString());
    }
    if (log.isLoggable(Level.FINEST)) {
      log.finest("dictionary = "+dictionary);
    }
    throw new UnsupportedOperationException("FIXME");
    //FIXME add(new BrowserPanel(dictionary));
  }
}
