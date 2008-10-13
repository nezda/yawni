package edu.brandeis.cs.steele.wn.browser;

import junit.framework.JUnit4TestAdapter;
import org.junit.*;
import static org.junit.Assert.*;

import java.io.*;
import java.util.*;
import java.util.prefs.*;

public class PreferencesManagerTest {
  /**
   * Normally <code>ignore</code> this test so we don't clear the user preferences
   * every time we run this test.
   */
  @Ignore
  @Test
  public void test1() throws BackingStoreException {
    Preferences prefs = Preferences.userNodeForPackage(PreferencesManagerTest.class);
    // this invalidates the prefs variable
    prefs.removeNode();
    //prefs.sync();
    List kids;
    //kids = Arrays.asList(prefs.childrenNames());
    //System.err.println("test1 before: "+prefs+" kids: "+kids);
    loadDefaults();
    prefs = Preferences.userNodeForPackage(PreferencesManagerTest.class);
    kids = Arrays.asList(prefs.childrenNames());
    //System.err.println("test1 after: "+prefs+" kids: "+kids);
    //System.err.println("k0: "+prefs.get("k0", "FAILED"));
    assertEquals("v0", prefs.get("k0", "FAILED"));
    //System.err.println("TestNode: "+prefs.get("/TestNode/k1", "FAILED"));
    //System.err.println("TestNode: "+prefs.get("TestKey/k1", "FAILED"));
    assertEquals("v1", prefs.get("TestKey/k1", "FAILED"));
    //System.err.println("TestNode: "+prefs.node("TestNode").get("k2", "FAILED"));
    assertEquals("v2", prefs.node("TestNode").get("k2", "FAILED"));
    // cannot get a child node AND its value directly with a get()
    assertFalse("v2".equals(prefs.get("TestNode.k2", "FAILED")));
    assertFalse("v2".equals(prefs.get("TestNode/k2", "FAILED")));
    assertFalse("v2".equals(prefs.get("/TestNode/k2", "FAILED")));
  }

  static void loadDefaults() {
    final InputStream is = PreferencesManagerTest.class.getResourceAsStream("testPrefs.xml");
    try {
      Preferences.importPreferences(is); 
      is.close();
    } catch(InvalidPreferencesFormatException ipfe) {
      throw new RuntimeException(ipfe);
    } catch(IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }
}
