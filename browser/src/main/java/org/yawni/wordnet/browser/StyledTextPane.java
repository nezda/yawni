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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

/**
 * Nice looking SansSerif HTML rendering JTextPane.
 * @see http://www.jroller.com/jnicho02/entry/using_css_with_htmleditorpane
 */
class StyledTextPane extends JTextPane {
  private static final int MENU_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

  static Map<Object, Action> actions;
  final Action biggerFont;
  final Action smallerFont;

  public void clear() {
    // NOTE: this doesn't work as expected; see JEditorPane.setText() javadoc
    // super.setText("");
//      try {
//        read(new ByteArrayInputStream(new byte[0]), "");
//      } catch (IOException ex) {
//        throw new RuntimeException(ex);
//      }
    // better alternative
    setDocument(getEditorKit().createDefaultDocument());
  }

  @Override
  public void setText(final String text) {
    if (text == null || text.length() == 0) {
      clear();
    } else {
      super.setText(text);
    }
  }

  @Override
  public void paintComponent(final Graphics g) {
    // bullets look better anti-aliased (still pretty big)
    final Graphics2D g2d = (Graphics2D) g;
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    super.paintComponent(g);
  }

  StyledTextPane() {
    super();
    //XXX final Action bigger = ACTIONS.get(HTMLEditorKit.FONT_CHANGE_BIGGER);
    //TODO move to StyledTextPane
    //TODO add Ctrl++ / Ctrl+- to Menu shortcuts (View?)
    // 1. define styles for various sizes (there are already Actions for this?)
    //
    // font-size-48
    // font-size-36
    // font-size-24
    // font-size-18
    // font-size-16
    // font-size-14
    // font-size-12
    // font-size-10
    // font-size-8
    //
    //TODO steps
    // bigger
    //   if size != max
    //     get next larger size and set its style
    //   else
    //     beep
    //
    // smaller
    //   if size != min
    //     get next smaller size and set its style
    //   else
    //     beep
    this.biggerFont = new StyledEditorKit.StyledTextAction("Bigger Font") {
      final int fake = init();

      private int init() {
        putValue(Action.SMALL_ICON, createFontScaleIcon(16, true));
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, MENU_MASK | InputEvent.SHIFT_MASK));
        return 0;
      }

      public void actionPerformed(final ActionEvent evt) {
        //System.err.println("bigger");//: "+evt);
        newFontSize(18);
        smallerFont.setEnabled(true);
        this.setEnabled(false);
      }
    };
    this.smallerFont = new StyledEditorKit.StyledTextAction("Smaller Font") {
      final int fake = init();

      private int init() {
        putValue(Action.SMALL_ICON, createFontScaleIcon(16, false));
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, MENU_MASK | InputEvent.SHIFT_MASK));
        return 0;
      }

      public void actionPerformed(final ActionEvent evt) {
        //System.err.println("smaller");//: "+evt);
        newFontSize(14);
        biggerFont.setEnabled(true);
        this.setEnabled(false);
      }
    };
    // this is the starting font size
    this.smallerFont.setEnabled(false);
  }

  private void newFontSize(int fontSize) {
    //XXX NOTE: not all sizes are defined, only
    // 48 36 24 18 16 14 12 10 8
    selectAll();
    getActionTable().get("font-size-" + fontSize).actionPerformed(new ActionEvent(StyledTextPane.this, 0, ""));
    setCaretPosition(0);
    // scroll to top
    final StyleSheet styleSheet = ((HTMLEditorKit) getStyledEditorKit()).getStyleSheet();
    // setting this style makes this font size change stick
    styleSheet.addRule("body {font-size: " + fontSize + ";}");
  }

  @Override
  protected HTMLEditorKit createDefaultEditorKit() {
    final HTMLEditorKit kit = new HTMLEditorKit();
    final StyleSheet styleSheet = kit.getStyleSheet();
    // add a CSS rule to force body tags to use the default label font
    // instead of the value in javax.swing.text.html.default.csss
    //XXX final Font font = UIManager.getFont("Label.font");
    //XXX String bodyRule = "body { font-family: " + font.getFamily() + "; " +
    //XXX          "font-size: " + font.getSize() + "pt; }";
    //XXX final String bodyRule = "body { font-family: " + font.getFamily() + "; }";
    //XXX styleSheet.addRule(bodyRule);
    styleSheet.addRule("body { font-family:sans-serif; }");
    styleSheet.addRule("li { margin-left:24px; margin-bottom:0px; }");
    styleSheet.addRule(".pos { font-family:serif; }");
    styleSheet.addRule(".focalWord {"+
//    "   color: red;" +
    "  font-weight: bold;" +
    "}");
    styleSheet.addRule(".summaryWord { font-weight: bold; }");
    styleSheet.addRule(".gloss {"+
    "  background-color: #F9F9F9;"+
    "  border: 1px solid #CCCCCC;"+ // not implemented in JDK
    "  padding-left: 5px;"+
    "  margin-top: 5px;"+
    "}");
    styleSheet.addRule(".definitions { }");
    styleSheet.addRule(".examples { font-style: italic; }");
    //FIXME text-indent:-10pt; causes the odd bolding bug
    //XXX styleSheet.addRule("ul {list-style-type:none; display:block; text-indent:-10pt;}");
    //XXX XXX styleSheet.addRule("ul {list-style-type:none; display:block;}");
    //XXX styleSheet.addRule("ul ul {list-style-type:circle };");
    //XXX XXX styleSheet.addRule("ul ul {list-style-type:circle };");
    styleSheet.addRule("ul {margin-left:12pt; margin-bottom:0pt;}");
    //getDocument().putProperty("multiByte", false);
    return kit;
  }

  private static Icon createFontScaleIcon(final int dimension, final boolean bold) {
    return new ImageIcon(createFontScaleImage(dimension, bold));
  }

  static BufferedImage createFontScaleImage(final int dimension, final boolean bold) {
    // new RGB image with transparency channel
    final BufferedImage image = new BufferedImage(dimension, dimension,
      BufferedImage.TYPE_INT_ARGB);
    // create new graphics and set anti-aliasing hints
    final Graphics2D graphics = (Graphics2D) image.getGraphics().create();
    // set completely transparent
    for (int col = 0; col < dimension; col++) {
      for (int row = 0; row < dimension; row++) {
        image.setRGB(col, row, 0x0);
      }
    }
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON);
    graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
      RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    final char letter = 'A';
    // Lucida Sans Regular 12pt Plain
    graphics.setFont(new Font(
      "Arial", //"Lucida Sans Regular", //"Serif",//"Arial",
      bold ? Font.BOLD : Font.PLAIN,
      dimension -
      (bold ? 1 : 3)));
    graphics.setPaint(Color.BLACK);
    final FontRenderContext frc = graphics.getFontRenderContext();
    final TextLayout mLayout = new TextLayout("" + letter, graphics.getFont(), frc);
    final float x = (float) (-.5 + (dimension - mLayout.getBounds().getWidth()) / 2);
    final float y = dimension - (float) ((dimension - mLayout.getBounds().getHeight()) / 2);
    graphics.drawString("" + letter, x, y);
    if (bold) {
      // overspray a little
      graphics.drawString("" + letter, x + 0.5f, y + 0.5f);
    }
    graphics.dispose();
    return image;
  }

  // The following method allows us to find an
  // action provided by the editor kit by its name.
  Map<Object, Action> getActionTable() {
    if (actions == null) {
      actions = new HashMap<>();
      final Action[] actionsArray = getStyledEditorKit().getActions();
      for (final Action a : actionsArray) {
        //System.err.println("a: "+a+" name: "+a.getValue(Action.NAME));
        actions.put(a.getValue(Action.NAME), a);
      }
    }
    return actions;
  }
}