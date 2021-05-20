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
package org.yawni.wordnet.browser;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

/**
 * The {@code TextPrompt} class will display a prompt over top of a text component when
 * the {@link Document} of the text component is empty. The Show property is used to
 * determine the visibility of the prompt.
 *
 * <p> The {@link Font} and foreground {@link Color} of the prompt will default to those properties
 * of the parent text component. You are free to change the properties after
 * class construction.
 *
 * <p> Courtesy <a href="http://tips4java.wordpress.com/2009/11/29/text-prompt/">
 *   http://tips4java.wordpress.com/2009/11/29/text-prompt/</a>
 */
public class TextPrompt extends JLabel implements FocusListener, DocumentListener, PropertyChangeListener {
  public enum Show {
    // show prompt independent of focus (disappears on typing into textComponent though)
    ALWAYS,
    FOCUS_GAINED,
    FOCUS_LOST,
  }

  private final JTextComponent sourceTextComponent;
  private final JTextComponent targetTextComponent;
  
  private Show show;
  private boolean showPromptOnce;
  private int focusLost;

  // default configuration:
  //   Listen for content/Document AND focus changes on textComponent (typically a JTextField)
  //   and render self as child component of textComponent based on this

  // desired configuration:
  //   Listen for content/Document changes on  textComponent and change label content.
  //   Position self (label) horizontally centered and about 20% of the from the top
  //
  //   sourceTextComponent: listened to for content/focus changes
  //   targetTextComponent: where TextPrompt is installed
  //
  //   bugs:
  //   - BOTH source and target text components need to be empty to trigger prompt
  //   - consider property change listener enable/disable and disabling resultEditorPane when searchField changes
  //   - tests are dead locking!?
  //   - label should have larger font
  //   - maybe label should be in italics or a serif font
  //   - use this to replace Status.INTRO functionality ("Enter search word...")
  //     and maybe No search results
  //   - fade in/out
  public TextPrompt(final String promptText,
                    final JTextComponent sourceTextComponent,
                    final JTextComponent targetTextComponent) {
    this.sourceTextComponent = sourceTextComponent;
    this.targetTextComponent = targetTextComponent;
    
    setShow(Show.ALWAYS);

    setText(promptText);
    setHorizontalTextPosition(CENTER);
    setAlignmentX(CENTER_ALIGNMENT);
    setAlignmentY(0.2f);
//    setFont(targetTextComponent.getFont().deriveFont(Font.ITALIC, 24.0f));
//    setFont(new Font("Serif", Font.ITALIC, 24));
    setFont(new Font("Serif", Font.PLAIN, 24));
    setForeground(targetTextComponent.getForeground());

    sourceTextComponent.addFocusListener(this);
    sourceTextComponent.getDocument().addDocumentListener(this);
//    sourceTextComponent.addPropertyChangeListener("document", this);
//    targetTextComponent.getDocument().addDocumentListener(this);
    targetTextComponent.addPropertyChangeListener("document", this);

    targetTextComponent.setLayout(new BoxLayout(targetTextComponent, BoxLayout.Y_AXIS));
    // put space above equal to 1 blank line of text
    targetTextComponent.add(Box.createVerticalStrut(getFontMetrics(getFont()).getHeight()));
    targetTextComponent.add(this);
    checkForPrompt();
  }

  /**
   * Convenience method to change the alpha value of the current foreground
   * Color to the specific value.
   *
   * @param alpha value in the range of 0 - 1.0.
   */
  @SuppressWarnings("unused")
  public void changeAlpha(float alpha) {
    changeAlpha((int) (alpha * 255));
  }

  /**
   * Convenience method to change the alpha value of the current foreground
   *  Color to the specific value.
   *
   * @param alpha value in the range of 0 - 255.
   */
  public void changeAlpha(int alpha) {
    alpha = alpha > 255 ? 255 : Math.max(alpha, 0);

    final Color foreground = getForeground();
    final int red = foreground.getRed();
    final int green = foreground.getGreen();
    final int blue = foreground.getBlue();

    final Color withAlpha = new Color(red, green, blue, alpha);
    super.setForeground(withAlpha);
  }

//  /**
//   * Convenience method to change the style of the current Font. The style
//   * values are found in the Font class. Common values might be:
//   * Font.BOLD, Font.ITALIC and Font.BOLD + Font.ITALIC.
//   *
//   * @param style value representing the the new style of the Font.
//   */
//  public void changeStyle(int style, float size) {
//    setFont(getFont().deriveFont(style, size));
//  }

//  /**
//   * Get the Show property
//   *
//   * @return the Show property.
//   */
//  public Show getShow() {
//    return show;
//  }

  /**
   * Set the prompt Show property to control when the promt is shown;
   * valid values are:
   *
   * {@link Show#ALWAYS} (default) - always show the prompt
   * {@link Show#FOCUS_GAINED} - show the prompt when the component gains focus
   *      (and hide the prompt when focus is lost)
   * {@link Show#FOCUS_LOST}- show the prompt when the component loses focus
   *     (and hide the prompt when focus is gained)
   *
   * @param show a valid Show enum
   */
  public void setShow(Show show) {
    this.show = show;
  }

  /**
   * Get the showPromptOnce property
   *
   * @return the showPromptOnce property.
   */
  @SuppressWarnings("unused")
  public boolean getShowPromptOnce() {
    return showPromptOnce;
  }

  /**
   * Show the prompt once. Once the component has gained/lost focus
   * once, the prompt will not be shown again.
   *
   * @param showPromptOnce  when true the prompt will only be shown once,
   *                        otherwise it will be shown repeatedly.
   */
  @SuppressWarnings("unused")
  public void setShowPromptOnce(boolean showPromptOnce) {
    this.showPromptOnce = showPromptOnce;
  }

  /**
   * Check whether the prompt should be visible or not. The visibility
   * will change on updates to the Document and on focus changes.
   */
  private void checkForPrompt() {
    //  Text has been entered, remove the prompt
    if (sourceTextComponent.getDocument().getLength() > 0 ||
        targetTextComponent.getDocument().getLength() > 0) {
      setVisible(false);
      return;
    }
    //  Prompt has already been shown once, remove it
    if (showPromptOnce && focusLost > 0) {
      setVisible(false);
      return;
    }
    //  Check the Show property and component focus to determine if the
    //  prompt should be displayed.
    if (sourceTextComponent.hasFocus()) {
      setVisible(show == Show.ALWAYS || show == Show.FOCUS_GAINED);
    } else {
      setVisible(show == Show.ALWAYS || show == Show.FOCUS_LOST);
    }
  }

  public void focusGained(FocusEvent evt) {
    checkForPrompt();
  }

  public void focusLost(FocusEvent evt) {
    focusLost++;
    checkForPrompt();
  }

  public void insertUpdate(DocumentEvent evt) {
    checkForPrompt();
    // NOTE: if document is replaced via setDocument(), 
    // no insert, remove, or change updates events are fired!
    // However, the "document" property is changed and this can
    // be listened for
  }

  public void removeUpdate(final DocumentEvent evt) {
    checkForPrompt();
  }

  public void changedUpdate(final DocumentEvent evt) {
  }

  public void propertyChange(final PropertyChangeEvent evt) {
    // triggered by targetTextComponent.setDocument() calls
    if ("document".equals(evt.getPropertyName())) {
      checkForPrompt();
    }
  }
}