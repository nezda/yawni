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

import java.awt.event.KeyEvent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.text.JTextComponent;
import org.fest.swing.annotation.GUITest;
import org.junit.Test;
import org.fest.swing.fixture.FrameFixture;
import org.fest.swing.fixture.JButtonFixture;
import org.fest.swing.fixture.JLabelFixture;
import org.fest.swing.fixture.JPopupMenuFixture;
import org.fest.swing.fixture.JTextComponentFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import static org.fest.assertions.Assertions.assertThat;

//@Ignore
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
  public void textPromptTest() {
    final JLabelFixture textPrompt = window.label("textPrompt");
    final JTextComponentFixture resultEditorPane = window.textBox("resultEditorPane");
    final JTextComponentFixture searchField = window.textBox("searchField");
    final JTextComponent searchFieldComponent = searchField.component();
    final JTextComponent resultEditorPaneComponent = resultEditorPane.component();
    textPrompt.requireVisible();
    for (int i = 0; i < 3; i++) {
      searchField.enterText("kitten").pressAndReleaseKeys(KeyEvent.VK_ENTER);
      textPrompt.requireNotVisible();
      window.label("statusLabel").requireText("Overview of kitten");
      searchField.pressAndReleaseKeys(KeyEvent.VK_BACK_SPACE).pressAndReleaseKeys(KeyEvent.VK_ENTER);
      assertThat(searchFieldComponent.getDocument().getLength()).isEqualTo(0);
      assertThat(resultEditorPaneComponent.getDocument().getLength()).isEqualTo(0);
      textPrompt.requireVisible();
    }
  }

  @GUITest
  @Test
  public void noMatchTest() {
    final JTextComponentFixture searchField = window.textBox("searchField");
    searchField.focus().requireFocused();
    searchField.enterText("performant").pressAndReleaseKeys(KeyEvent.VK_ENTER);
    window.label("statusLabel").requireText("No matches found.");
    window.button("RelationTypeComboBox::Noun").requireDisabled();
    window.button("RelationTypeComboBox::Verb").requireDisabled();
    window.button("RelationTypeComboBox::Adjective").requireDisabled();
    window.button("RelationTypeComboBox::Adverb").requireDisabled();
  }

  @GUITest
  @Test
  public void unfortunateGerbilTestKeyboardTest() {
    final JTextComponentFixture searchField = window.textBox("searchField");
    searchField.focus().requireFocused();
    searchField.enterText("gerbil").pressAndReleaseKeys(KeyEvent.VK_ENTER);
    window.label("statusLabel").requireText("Overview of gerbil");
    final JButtonFixture nounButton = window.button("RelationTypeComboBox::Noun");
    // clear searchField
    searchField.enterText(" ").pressAndReleaseKeys(KeyEvent.VK_ENTER);

    // since Noun triggers an action that looks at the searchField's text
    // and expects it to be compatible with itself, it must be disabled
    nounButton.requireDisabled();
  }
  
  @GUITest
  @Test
  public void kittenKeyboardTest() {
    final JTextComponentFixture searchField = window.textBox("searchField");
    final JTextComponent searchFieldComponent = searchField.component();
    assertThat(searchFieldComponent).isNotNull();
    searchField.requireFocused(); // defaults to focused
//    searchField.focus().requireFocused();
    searchField.enterText("kitten").pressAndReleaseKeys(KeyEvent.VK_ENTER);
    window.label("statusLabel").requireText("Overview of kitten");
    final JButtonFixture nounButton = window.button("RelationTypeComboBox::Noun");
    // transfer focus from searchField to Noun button with keyboard
    searchField.pressAndReleaseKeys(KeyEvent.VK_TAB);
    nounButton.requireFocused();
    // triggers Noun RelationTypeComboBox to show JPopupMenu
    nounButton.pressAndReleaseKeys(KeyEvent.VK_ENTER);
    // key stroke goes to popup
    window.robot.type('s');

    // only need popup to verify correctness
    final JPopupMenu popupMenu = nounButton.component().getComponentPopupMenu();
    assertThat(popupMenu).isNotNull();
    final JPopupMenuFixture popup = new JPopupMenuFixture(window.robot, popupMenu);
    window.robot.waitForIdle();
    popup.requireVisible();
    final JMenuItem sensesItem = popup.menuItemWithPath("Senses").component();
    assertThat(sensesItem.isArmed()).isTrue();
    assertThat(sensesItem.hasFocus()).isFalse();
//    assertThat(sensesItem.isSelected()).isFalse();
    
    assertThat(popup.menuItemWithPath("Derivationally related forms").component().isArmed()).isFalse();

    // key stroke goes to popup
    window.robot.type('s');
    window.robot.type('e');
    assertThat(sensesItem.isArmed()).isTrue();

    // hit enter
    window.robot.pressAndReleaseKeys(KeyEvent.VK_ENTER);
    window.label("statusLabel").requireText("Synonyms search for noun \"kitten\"");
  }

  @Ignore
  @GUITest
  @Test
  public void mouseTest() {
    window.textBox("searchField").enterText("puppy");
    window.button("searchButton").click();
    window.label("statusLabel").requireText("Overview of puppy");
    // triggers Noun RelationTypeComboBox to show JPopupMenu
    window.button("RelationTypeComboBox::Noun").click();
//    final JPopupMenu popupMenu = window.button("RelationTypeComboBox::Noun").component().getComponentPopupMenu();
//    assertThat(popupMenu).isNotNull();
//    final JPopupMenuFixture popup = new JPopupMenuFixture(window.robot, popupMenu);
//    popup.pressAndReleaseKeys('s', 'e');
//    popupMenu.pressAndReleaseKeys('s', 'e');
//    window.menuItem("Senses").requireEnabled();
  }

  @GUITest
  @Test
  public void popdownButtonTortureTest() {
    window.textBox("searchField").enterText("calf");
    window.button("searchButton").click();
    window.label("statusLabel").requireText("Overview of calf");
    // triggers Noun RelationTypeComboBox to show JPopupMenu
    final JButtonFixture nounButton = window.button("RelationTypeComboBox::Noun");
    nounButton.click();
    final JPopupMenu popupMenu = nounButton.component().getComponentPopupMenu();
    final JPopupMenuFixture popup = new JPopupMenuFixture(window.robot, popupMenu);
    popup.requireVisible();
    nounButton.click();
    popup.requireNotVisible();
    nounButton.pressAndReleaseKeys(KeyEvent.VK_ENTER);
    popup.requireVisible();
    nounButton.click(); // 1
    nounButton.pressAndReleaseKeys(KeyEvent.VK_SPACE); // 2
    nounButton.click(); // 3
    nounButton.pressAndReleaseKeys(KeyEvent.VK_ENTER); // 4
    nounButton.click(); // 5
    // odd number of "clicks" should leave popup not visible
    popup.requireNotVisible();
  }

  @GUITest
  @Test
  public void hyponymsThenNoMatchTest() {
    final JTextComponentFixture searchField = window.textBox("searchField");
    searchField.enterText("kid").pressAndReleaseKeys(KeyEvent.VK_ENTER);
    final JButtonFixture nounButton = window.button("RelationTypeComboBox::Noun");
    nounButton.click();
    final JPopupMenu popupMenu = nounButton.component().getComponentPopupMenu();
    final JPopupMenuFixture popup = new JPopupMenuFixture(window.robot, popupMenu);
//    popup.menuItemWithPath("Hypernyms (kid is a kind of...)").click();

    // key stroke goes to popup
    window.robot.type('h');
    window.robot.type('y');
    window.robot.type('p');
    window.robot.type('e');
    window.robot.pressAndReleaseKeys(KeyEvent.VK_ENTER);
    
    searchField.enterText("performant").pressAndReleaseKeys(KeyEvent.VK_ENTER);
    window.label("statusLabel").requireText("No matches found.");
    searchField.enterText("").pressAndReleaseKeys(KeyEvent.VK_ENTER);
//    searchField.enterText(" ").pressAndReleaseKeys(KeyEvent.VK_ENTER);
    window.label("statusLabel").requireText("No matches found.");
//    window.label("statusLabel").requireText("Enter search word and press return");
  }
}