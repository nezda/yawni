/*
 * WordNet-Java
 *
 * Copyright 1998 by Oliver Steele.  You can use this software freely so long as you preserve
 * the copyright notice and this restriction, and label your changes.
 */
//package edu.brandeis.cs.steele.wn.browser;
package browser;

import java.applet.Applet;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import edu.brandeis.cs.steele.wn.DictionaryDatabase;
import edu.brandeis.cs.steele.wn.FileBackedDictionary;
import edu.brandeis.cs.steele.wn.RemoteFileManager;

public class BrowserApplet extends Applet {

    private Log log = LogFactory.getLog(this.getClass());

	public void init() {
		URL url = getCodeBase();
        if (log.isDebugEnabled()) {
            log.debug("url = "+url);
        }
        String hostname = url.getHost();
		if (url.getPort() != -1) {
			hostname += ":" + url.getPort();
		}
        if (log.isDebugEnabled()) {
            log.debug("hostname = "+hostname);
        }
        DictionaryDatabase dictionary;
		try {
			dictionary = new FileBackedDictionary(RemoteFileManager.lookup(hostname));
		} catch (Exception e) {
			throw new RuntimeException(e.toString());
		}
        if (log.isDebugEnabled()) {
            log.debug("dictionary = "+dictionary);
        }
        add(new BrowserPanel(dictionary));
	}
}
