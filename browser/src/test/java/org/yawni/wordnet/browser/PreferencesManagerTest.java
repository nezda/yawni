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
    assertNotEquals("v2", prefs.get("TestNode.k2", "FAILED"));
    assertNotEquals("v2", prefs.get("TestNode/k2", "FAILED"));
    assertNotEquals("v2", prefs.get("/TestNode/k2", "FAILED"));
  }

  private static void loadDefaults() {
    final InputStream is = PreferencesManagerTest.class.getResourceAsStream("testPrefs.xml");
    try {
      Preferences.importPreferences(is); 
      is.close();
    } catch(InvalidPreferencesFormatException | IOException ipfe) {
      throw new RuntimeException(ipfe);
    }
  }
}
