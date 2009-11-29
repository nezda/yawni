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
package org.yawni.wn.browser;

import java.awt.Component;
import java.awt.event.KeyEvent;
import javax.swing.JPopupMenu;
import org.fest.swing.annotation.GUITest;
import org.junit.Test;
import org.fest.swing.fixture.FrameFixture;
import org.fest.swing.fixture.JButtonFixture;
import org.fest.swing.fixture.JPopupMenuFixture;
import org.fest.swing.fixture.JTextComponentFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import static org.fest.assertions.Assertions.assertThat;

public class BrowserFestTest {
  private FrameFixture window;
  private Browser browser;

  @Before
  public void setUp() {
    browser = new Browser();
    PreferencesManager.loadSettings(browser);
    window = new FrameFixture(browser);
    window.show();
  }

  @After
  public void tearDown() {
    window.cleanUp();
  }
  
  @GUITest
  @Test
  public void kittenKeyboardTest() {
    final JTextComponentFixture searchField = window.textBox("searchField");
    final Component searchFieldComponent = searchField.component();
    assertThat(searchFieldComponent).isNotNull();

    searchField.focus();
    assertThat(searchFieldComponent.hasFocus()).isTrue();

    window.textBox("searchField").enterText("kitten").pressAndReleaseKeys(KeyEvent.VK_ENTER);
    window.label("statusLabel").requireText("Overview of kitten");
//    window.robot.moveMouse(searchField);
    final JButtonFixture nounButton = window.button("RelationTypeComboBox::Noun");
    // triggers RelationTypeComboBox showing JPopupMenu
    //window.button("RelationTypeComboBox::Noun").focus().pressAndReleaseKeys(KeyEvent.VK_ENTER);
    // transfer focus from searchField to Noun button with keyboard
//    System.err.println("focusOwner: "+browser.getFocusOwner());
    window.textBox("searchField").pressAndReleaseKeys(KeyEvent.VK_TAB);
    // doesn't work
//    window.robot.type('\t');
    // not necessary
//    window.robot.waitForIdle();

//    System.err.println("focusOwner: "+browser.getFocusOwner());
    assertThat(nounButton.component().hasFocus()).isTrue();
    nounButton.pressAndReleaseKeys(KeyEvent.VK_ENTER);

//    System.err.println("moving mouse...");
//    window.robot.moveMouse(searchField);
//    System.err.println("mouse moved.");
//    System.err.println("focusOwner: "+browser.getFocusOwner());
    
    final JPopupMenu popupMenu = nounButton.component().getComponentPopupMenu();
    assertThat(popupMenu).isNotNull();
    final JPopupMenuFixture popup = new JPopupMenuFixture(window.robot, popupMenu);
//    popup.requireFocused();
//    popup.focus();
    assertThat('s' == KeyEvent.VK_S);
    assertThat('e' == KeyEvent.VK_E);
//    popup.pressAndReleaseKeys('d');
    window.robot.type('d');
//    popup.pressAndReleaseKeys('d');
//    popup.pressAndReleaseKeys('s', 'e');
  }

  @Ignore
  @GUITest
  @Test
  public void kittenMouseTest() {
    window.textBox("searchField").enterText("kitten");
    window.button("searchButton").click();
    window.label("statusLabel").requireText("Overview of kitten");
    // triggers RelationTypeComboBox showing JPopupMenu
    window.button("RelationTypeComboBox::Noun").click();
    final JPopupMenu popupMenu = window.button("RelationTypeComboBox::Noun").component().getComponentPopupMenu();
    assertThat(popupMenu).isNotNull();
    final JPopupMenuFixture popup = new JPopupMenuFixture(window.robot, popupMenu);
//    popup.pressAndReleaseKeys('s', 'e');
//    popupMenu.pressAndReleaseKeys('s', 'e');
//    window.menuItem("Senses").requireEnabled();
  }
}