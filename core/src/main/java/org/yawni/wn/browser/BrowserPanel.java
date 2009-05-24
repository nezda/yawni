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
/*
 * Copyright 1998 by Oliver Steele.  You can use this software freely so long as you preserve
 * the copyright notice.
 */
package org.yawni.wn.browser;

import org.yawni.util.ImmutableList;
import org.yawni.util.Utils;
import org.yawni.wn.DictionaryDatabase;
import org.yawni.wn.FileBackedDictionary;
import org.yawni.wn.POS;
import org.yawni.wn.Pointer;
import org.yawni.wn.PointerTarget;
import org.yawni.wn.PointerType;
import org.yawni.wn.Synset;
import org.yawni.wn.Word;
import org.yawni.wn.WordSense;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.geom.*;
import java.awt.font.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.undo.*;
import java.util.prefs.*;

/**
 * The main panel of browser.
 */
public class BrowserPanel extends JPanel {
  private static final Logger log = LoggerFactory.getLogger(BrowserPanel.class.getName());
  private static Preferences prefs = Preferences.userNodeForPackage(BrowserPanel.class).node(BrowserPanel.class.getSimpleName());
  private static final long serialVersionUID = 1L;
  private DictionaryDatabase dictionary;

  DictionaryDatabase dictionary() {
    return FileBackedDictionary.getInstance();
  }
  private final Browser browser;
//  private boolean hasFocus = false;

//  private class FocusWatcher implements WindowFocusListener {
//    public void windowGainedFocus(final WindowEvent evt) {
//      //DBG System.err.println("gained");
//      hasFocus = true;
//    }
//    public void windowLostFocus(final WindowEvent evt) {
//      //DBG System.err.println("lost");
//      hasFocus = false;
//    }
//  } // end class FocusWatcher
//
//  private class FocusWatcher2 extends WindowAdapter {
//    public void windowDeactivated(final WindowEvent evt) {
//      //DBG System.err.println("deactivated");
//    }
//  }
  private static final int MENU_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
  private JTextField searchField;
  // whenever this is true, the content of search field has changed
  // and is not synced with the display
//  private boolean searchFieldChanged;
  private String displayedValue;
  private final JButton searchButton;
  private final UndoManager undoManager;
  private final UndoAction undoAction;
  private final RedoAction redoAction;
  private final StyledTextPane resultEditorPane;
  private EnumMap<POS, PointerTypeComboBox> posBoxes;
  private final Action slashAction;
  private final JLabel statusLabel;
//  private final int pad = 5;

  public BrowserPanel(final Browser browser) {
    this.browser = browser;
    super.setLayout(new BorderLayout());

    //XXX DBG browser.addWindowFocusListener(new FocusWatcher());
    //XXX DBG browser.addWindowListener(new FocusWatcher2());

    //this.searchField = new JTextField() {
    //  private static final long serialVersionUID = 1L;
    //  protected Document createDefaultModel() {
    //    //String word = "['a-z.0-9]+";
    //    // regex has to support partial matches (as they're typed)
    //    //String regex = "(?i)("+word+"[ /_-]?)+";
    //    //return new RegexConstrainedDocument(regex);
    //
    //    //return new SearchFieldDocument();
    //  }
    //};
    //XXX doesn't seem to work!? ((AbstractDocument)searchField.getDocument()).setDocumentFilter(new SearchFieldDocumentFilter());
    this.searchField = new JTextField();
    SearchFrame.multiClickSelectAll(searchField);
    this.searchField.setDocument(new SearchFieldDocument());
    this.searchField.setBackground(Color.WHITE);
    this.searchField.putClientProperty("JTextField.variant", "search");

    this.searchField.getDocument().addDocumentListener(new DocumentListener() {
      public void changedUpdate(final DocumentEvent evt) {
        assert searchField.getDocument() == evt.getDocument();
//        searchFieldChanged = true;
      }

      public void insertUpdate(final DocumentEvent evt) {
        assert searchField.getDocument() == evt.getDocument();
//        searchFieldChanged = true;
      }

      public void removeUpdate(final DocumentEvent evt) {
        assert searchField.getDocument() == evt.getDocument();
//        searchFieldChanged = true;
      }

      String getModText(final DocumentEvent evt) {
        try {
          final String change = searchField.getDocument().getText(evt.getOffset(), evt.getLength());
          return change;
        } catch (BadLocationException ble) {
          throw new RuntimeException(ble);
        }
      }
    });

    this.undoManager = new UndoManager() {
      private static final long serialVersionUID = 1L;

      @Override public boolean addEdit(UndoableEdit ue) {
        //System.err.println("ue: "+ue);
        return super.addEdit(ue);
      }
    };
    this.undoAction = new UndoAction();
    this.redoAction = new RedoAction();

    this.searchField.getDocument().addUndoableEditListener(new UndoableEditListener() {
      public void undoableEditHappened(final UndoableEditEvent evt) {
        //System.err.println("undoableEditHappened: "+evt);
        // Remember the edit and update the menus.
        undoManager.addEdit(evt.getEdit());
        undoAction.updateUndoState();
        redoAction.updateRedoState();
      }
    });

    this.searchField.setInputVerifier(new InputVerifier() {
      @Override public boolean verify(JComponent input) {
        final JTextField searchField = (JTextField) input;
        // if the text in this field is different from the
        // text which the menus are currently for, need to
        // re-issue the search
        final String inputString = searchField.getText().trim();
        if (false == inputString.equals(displayedValue)) {
          // issue fresh search
          searchButton.doClick();
          // don't yield focus
          return false;
        } else {
          return true;
        }
      }
    });

    final Action searchAction = new AbstractAction("Search") {
      private static final long serialVersionUID = 1L;

      public void actionPerformed(final ActionEvent event) {
        if (event.getSource() == searchField) {
          // doClick() will generate another event
          // via searchButton
          searchButton.doClick();
          return;
        }
        displayOverview();
      }
    };
    this.searchButton = new JButton(searchAction);
    this.searchButton.setFocusable(false);
    this.searchButton.getActionMap().put("Search", searchAction);

    this.slashAction = new AbstractAction("Slash") {
      private static final long serialVersionUID = 1L;

      public void actionPerformed(final ActionEvent event) {
        searchField.grabFocus();
      }
    };

    makePOSComboBoxes();

    final JPanel searchAndPointersPanel = new JPanel(new GridBagLayout());
    final GridBagConstraints c = new GridBagConstraints();

    c.gridy = 0;
    c.gridx = 0;
    // T,L,B,R
    c.insets = new Insets(3, 3, 0, 3);
    c.fill = GridBagConstraints.HORIZONTAL;
    //c.anchor = GridBagConstraints.WEST;
    c.gridwidth = 4;
    final Box searchPanel = new Box(BoxLayout.X_AXIS);

    searchPanel.add(searchField);
    searchPanel.add(Box.createHorizontalStrut(3));
    searchPanel.add(searchButton);
    searchAndPointersPanel.add(searchPanel, c);

    c.fill = GridBagConstraints.NONE;
    c.gridwidth = 1;

    c.gridy = 1;
    c.gridx = 0;
    c.insets = new Insets(3, 3, 3, 3);
    searchAndPointersPanel.add(this.posBoxes.get(POS.NOUN), c);
    c.gridx = 1;
    searchAndPointersPanel.add(this.posBoxes.get(POS.VERB), c);
    c.gridx = 2;
    searchAndPointersPanel.add(this.posBoxes.get(POS.ADJ), c);
    c.gridx = 3;
    c.insets = new Insets(3, 0, 3, 3);
    searchAndPointersPanel.add(this.posBoxes.get(POS.ADV), c);

    // set width(pointerPanel) = width(searchPanel)

    this.add(searchAndPointersPanel, BorderLayout.NORTH);

    this.resultEditorPane = new StyledTextPane();
    this.resultEditorPane.setBorder(browser.textAreaBorder());
    this.resultEditorPane.setBackground(Color.WHITE);
    // http://www.groupsrv.com/computers/about179434.html
    // enables scrolling with arrow keys
    this.resultEditorPane.setEditable(false);
    final JScrollPane jsp = new JScrollPane(resultEditorPane);
    final JScrollBar jsb = jsp.getVerticalScrollBar();

    //TODO move to StyledTextPane (already an action for this?)
    final Action scrollDown = new AbstractAction() {
      private static final long serialVersionUID = 1L;

      public void actionPerformed(final ActionEvent event) {
        final int max = jsb.getMaximum();
        final int inc = resultEditorPane.getScrollableUnitIncrement(jsp.getViewportBorderBounds(), SwingConstants.VERTICAL, +1);
        final int vpos = jsb.getValue();
        final int newPos = Math.min(max, vpos + inc);
        if (newPos != vpos) {
          jsb.setValue(newPos);
        }
      }
    };

    //TODO move to StyledTextPane (already an action for this?)
    final Action scrollUp = new AbstractAction() {
      private static final long serialVersionUID = 1L;

      public void actionPerformed(final ActionEvent event) {
        //final int max = jsb.getMaximum();
        final int inc = resultEditorPane.getScrollableUnitIncrement(jsp.getViewportBorderBounds(), SwingConstants.VERTICAL, -1);
        final int vpos = jsb.getValue();
        final int newPos = Math.max(0, vpos - inc);
        if (newPos != vpos) {
          jsb.setValue(newPos);
        }
      }
    };

    // zoom support
    this.searchField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, MENU_MASK), resultEditorPane.biggerFont);
    this.searchField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, MENU_MASK | InputEvent.SHIFT_MASK), resultEditorPane.biggerFont);
    this.searchField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, MENU_MASK), resultEditorPane.smallerFont);
    this.searchField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, MENU_MASK | InputEvent.SHIFT_MASK), resultEditorPane.smallerFont);

    final String[] extraKeys = new String[]{
      "pressed",
      "shift pressed",
      "meta pressed",
      "shift meta",
    };
    for (final String extraKey : extraKeys) {
      this.searchField.getInputMap().put(KeyStroke.getKeyStroke(extraKey + " UP"), scrollUp);
      this.resultEditorPane.getInputMap().put(KeyStroke.getKeyStroke(extraKey + " UP"), scrollUp);

      this.searchField.getInputMap().put(KeyStroke.getKeyStroke(extraKey + " DOWN"), scrollDown);
      this.resultEditorPane.getInputMap().put(KeyStroke.getKeyStroke(extraKey + " DOWN"), scrollDown);

      for (final PointerTypeComboBox comboBox : this.posBoxes.values()) {
        comboBox.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(extraKey + " UP"), "scrollUp");
        comboBox.getActionMap().put("scrollUp", scrollUp);
        comboBox.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(extraKey + " DOWN"), "scrollDown");
        comboBox.getActionMap().put("scrollDown", scrollDown);

        // yea these don't use extraKey
        comboBox.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, MENU_MASK), "bigger");
        comboBox.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, MENU_MASK | InputEvent.SHIFT_MASK), "bigger");
        comboBox.getActionMap().put("bigger", resultEditorPane.biggerFont);
        comboBox.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, MENU_MASK), "smaller");
        comboBox.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, MENU_MASK | InputEvent.SHIFT_MASK), "smaller");
        comboBox.getActionMap().put("smaller", resultEditorPane.smallerFont);
      }
    }
    // search keyboard support
    jsp.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, 0, false), "Slash");
    jsp.getActionMap().put("Slash", slashAction);
    jsp.getVerticalScrollBar().setFocusable(false);
    jsp.getHorizontalScrollBar().setFocusable(false);
    // OS X usability guidelines recommend this
    jsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    jsp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    this.add(jsp, BorderLayout.CENTER);
    this.statusLabel = new JLabel();
    this.statusLabel.setBorder(BorderFactory.createEmptyBorder(0 /*top*/, 3 /*left*/, 3 /*bottom*/, 0 /*right*/));
    this.add(this.statusLabel, BorderLayout.SOUTH);
    updateStatusBar(Status.INTRO);

    this.searchField.addActionListener(searchAction);

    validate();
    preload();
  }

  private static Icon createUndoIcon() {
    final Icon icon = new ImageIcon(BrowserPanel.class.getResource("Undo.png"));
    assert icon.getIconWidth() > 0 && icon.getIconHeight() > 0;
    return icon;
  }

  private static Icon createRedoIcon() {
    final Icon icon = new ImageIcon(BrowserPanel.class.getResource("Redo.png"));
    assert icon.getIconWidth() > 0 && icon.getIconHeight() > 0;
    return icon;
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

  static ImageIcon createFindIcon(final int dimension, final boolean bold) {
    return new ImageIcon(createFindImage(dimension, bold));
  }

  static BufferedImage createFindImage(final int dimension, final boolean bold) {
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

    //XXX make magnifying glass
    // circle (upper left)
    // handle lower right @ 135% CW from TDC
    // drawOval(x,y,w,h)
    System.err.println("stroke: " + ((BasicStroke) graphics.getStroke()).getEndCap());
    System.err.printf("  caps: %s %d %s %d %s %d\n",
      "CAP_BUTT", BasicStroke.CAP_BUTT,
      "CAP_ROUND", BasicStroke.CAP_ROUND,
      "CAP_SQUARE", BasicStroke.CAP_SQUARE);
    graphics.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    int x = 1;
    int y = 1;
    int w = (int) ((dimension - (x * 2)) * .75);
    int h = w;
    System.err.println("w: " + w);
    // handle
    //int x1 = ;
    //int y1 = ;
    //int x2 = ;
    //int y2 = ;

    //XXX graphics.setPaint(new Color(0, 0, 196));
    graphics.setPaint(Color.BLACK);
    //graphics.drawOval(x, y, w, h);
    graphics.draw(new Ellipse2D.Double(1, 1, w, h));

    //final char letter = 'A';
    //graphics.setPaint(Color.BLACK);
    //final FontRenderContext frc = graphics.getFontRenderContext();
    //final TextLayout mLayout = new TextLayout("" + letter, graphics.getFont(), frc);
    //final float x = (float) (-.5 + (dimension - mLayout.getBounds()
    //      .getWidth()) / 2);
    //final float y = dimension
    //  - (float) ((dimension - mLayout.getBounds().getHeight()) / 2);


    //graphics.drawString("" + letter, x, y);
    //if(bold) {
    //  // overspray a little
    //  graphics.drawString("" + letter, x + 0.5f, y + 0.5f);
    //}
    graphics.dispose();
    return image;
  }

  void debug() {
    //System.err.println("searchField: "+searchField);
    //System.err.println();
    //System.err.println("searchButton: "+searchButton);
  }

  // Callback used by Browser so BrowserPanel can add menu items to File menu
  void addMenuItems(final Browser browser, final JMenu fileMenu) {
    fileMenu.addSeparator();
    JMenuItem item;
    item = fileMenu.add(undoAction);
    //TODO move this stuff UndoAction / RedoAction
    //XXX item.setIcon(browser.BLANK_ICON);
    // Command+Z and Ctrl+Z undo on OS X, Windows
    item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, MENU_MASK));
    item = fileMenu.add(redoAction);
    //XXX item.setIcon(browser.BLANK_ICON);
    // http://sketchup.google.com/support/bin/answer.py?hl=en&answer=70151
    // redo is Shift+Command+Z on OS X, Ctrl+Y on Windows (and everything else)
    if (MENU_MASK != java.awt.event.InputEvent.META_MASK) {
      item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, MENU_MASK));
    } else {
      item.setAccelerator(KeyStroke.getKeyStroke(
        KeyEvent.VK_Z,
        MENU_MASK | java.awt.event.InputEvent.SHIFT_MASK));
    }

    fileMenu.addSeparator();
    item = fileMenu.add(resultEditorPane.biggerFont);
    item = fileMenu.add(resultEditorPane.smallerFont);
  }

  void wireToFrame(final Browser browser) {
    assert browser.isFocusCycleRoot();
    final List<Component> components = new ArrayList<Component>();
    components.add(this.searchField);
    components.addAll(this.posBoxes.values());
    //for(final PointerTypeComboBox box : this.posBoxes.values()) {
    //  components.add(box.menu);
    //}
    browser.setFocusTraversalPolicy(new SimpleFocusTraversalPolicy(components));
  }

  @Override public void setVisible(final boolean visible) {
    super.setVisible(visible);
    if (visible) {
      searchField.requestFocusInWindow();
    }
  }

  synchronized String getSearchText() {
    return searchField.getText();
  }

  // non-static class UndoAction cross references RedoAction and
  // other non-static fields
  class UndoAction extends AbstractAction {
    private static final long serialVersionUID = 1L;

    UndoAction() {
      super("Undo");
      setEnabled(false);
      putValue(Action.SMALL_ICON, createUndoIcon());
    }

    public void actionPerformed(final ActionEvent evt) {
      try {
        searchField.requestFocusInWindow();
        BrowserPanel.this.undoManager.undo();
      } catch (final CannotUndoException ex) {
        System.err.println("Unable to undo: " + ex);
        ex.printStackTrace();
      }
      updateUndoState();
      BrowserPanel.this.redoAction.updateRedoState();
    }

    protected void updateUndoState() {
      if (BrowserPanel.this.undoManager.canUndo()) {
        setEnabled(true);
        //putValue(Action.NAME, BrowserPanel.this.undoManager.getUndoPresentationName());
        putValue(Action.NAME, "Undo");
      } else {
        setEnabled(false);
        putValue(Action.NAME, "Undo");
      }
    }
  } // end class UndoAction

  // non-static class RedoAction cross references UndoAction and
  // other non-static fields
  class RedoAction extends AbstractAction {
    private static final long serialVersionUID = 1L;

    RedoAction() {
      super("Redo");
      setEnabled(false);
      putValue(Action.SMALL_ICON, createRedoIcon());
    }

    public void actionPerformed(final ActionEvent evt) {
      try {
        searchField.requestFocusInWindow();
        BrowserPanel.this.undoManager.redo();
      } catch (final CannotRedoException ex) {
        System.err.println("Unable to redo: " + ex);
        ex.printStackTrace();
      }
      updateRedoState();
      BrowserPanel.this.undoAction.updateUndoState();
    }

    protected void updateRedoState() {
      if (BrowserPanel.this.undoManager.canRedo()) {
        setEnabled(true);
        //putValue(Action.NAME, BrowserPanel.this.undoManager.getRedoPresentationName());
        putValue(Action.NAME, "Redo");
      } else {
        setEnabled(false);
        putValue(Action.NAME, "Redo");
      }
    }
  } // end class RedoAction

  /**
   * Nice looking SansSerif HTML rendering JTextPane.
   * http://www.jroller.com/jnicho02/entry/using_css_with_htmleditorpane
   */
  private static class StyledTextPane extends JTextPane {
    private static final long serialVersionUID = 1L;
    Map<Object, Action> actions;
    final Action biggerFont;
    final Action smallerFont;

    @Override public void paintComponent(final Graphics g) {
      // bullets look better anti-aliased (still pretty big)
      final Graphics2D g2d = (Graphics2D) g;
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON);
      super.paintComponent(g);
    }

    StyledTextPane() {
      //XXX final Action bigger = actions.get(HTMLEditorKit.FONT_CHANGE_BIGGER);
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
        private static final long serialVersionUID = 1L;
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
        private static final long serialVersionUID = 1L;
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
      setCaretPosition(0); // scroll to top
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

      styleSheet.addRule("body {font-family:sans-serif;}");
      styleSheet.addRule("li {margin-left:12px; margin-bottom:0px;}");
      //FIXME text-indent:-10pt; causes the odd bolding bug
      //XXX styleSheet.addRule("ul {list-style-type:none; display:block; text-indent:-10pt;}");
      //XXX XXX styleSheet.addRule("ul {list-style-type:none; display:block;}");
      //XXX styleSheet.addRule("ul ul {list-style-type:circle };");
      //XXX XXX styleSheet.addRule("ul ul {list-style-type:circle };");
      styleSheet.addRule("ul {margin-left:12pt; margin-bottom:0pt;}");
      //getDocument().putProperty("multiByte", false);
      return kit;
    }

    // The following method allows us to find an
    // action provided by the editor kit by its name.
    Map<Object, Action> getActionTable() {
      if (actions == null) {
        actions = new HashMap<Object, Action>();
        final Action[] actionsArray = getStyledEditorKit().getActions();
        for (int i = 0; i < actionsArray.length; i++) {
          final Action a = actionsArray[i];
          //System.err.println("a: "+a+" name: "+a.getValue(Action.NAME));
          actions.put(a.getValue(Action.NAME), a);
        }
      }
      return actions;
    }
  } // end class StyledTextPane

  /**
   * Command button encapsulates a button (for a pos) which controls a
   * menu that is dynamically populated with PointerTypeAction(s).
   * - handles Slash (search)
   * - interactive updates via updateFor()
   */
  private class PointerTypeComboBox extends PopdownButton {
    // FIXME if user changes text field contents and selects menu, bad things will happen
    // FIXME text in HTML pane looks bold at line wraps
    private static final long serialVersionUID = 1L;
    private final POS pos;

    PointerTypeComboBox(final POS pos) {
      //super(Utils.capitalize(pos.getLabel())+" \u25BE\u25bc"); // large: \u25BC ▼ small: \u25BE ▾
      super(Utils.capitalize(pos.getLabel()));
      this.pos = pos;
      getPopupMenu().addMenuKeyListener(new MenuKeyListener() {
        public void menuKeyPressed(final MenuKeyEvent evt) {
        }

        public void menuKeyReleased(final MenuKeyEvent evt) {
        }

        public void menuKeyTyped(final MenuKeyEvent evt) {
          //System.err.println("menu evt: " + evt + " char: \"" + evt.getKeyChar() + "\"");
          switch (evt.getKeyChar()) {
            case '/':
              // if slash, hide menu, go back to searchField
              PointerTypeComboBox.this.doClick();
              slashAction.actionPerformed(null);
              break;
            case '\t':
              System.err.println("menu evt: tab");
              hidePopupMenu();
              break;
          }
        // if tab, move focus to next thing
        }
      });
    }

    /** populate with <code>PointerType</code>s which apply to pos+word */
    void updateFor(final POS pos, final Word word) {
      getPopupMenu().removeAll();
      getPopupMenu().add(new PointerTypeAction("Senses", pos, null));
      for (final PointerType pointerType : word.getPointerTypes()) {
        // use word+pos custom labels for drop downs
        final String label = String.format(pointerType.getFormatLabel(word.getPOS()), word.getLemma());
        //System.err.println("label: "+label+" word: "+word+" pointerType: "+pointerType);
        final JMenuItem item = getPopupMenu().add(new PointerTypeAction(label, pos, pointerType));
      }
      if (pos == POS.VERB) {
        // use word+pos custom labels for drop downs
        final String label = String.format("Sentence frames for verb %s", word.getLemma());
        //System.err.println("label: "+label+" word: "+word+" pointerType: "+pointerType);
        final JMenuItem item = getPopupMenu().add(new VerbFramesAction(label));
      }
    }
  } // end class PointerTypeComboBox

  /**
   * Displays information related to a given POS + PointerType
   */
  class PointerTypeAction extends AbstractAction {
    private static final long serialVersionUID = 1L;
    private final POS pos;
    private final PointerType pointerType;

    PointerTypeAction(final String label, final POS pos, final PointerType pointerType) {
      super(label);
      this.pos = pos;
      this.pointerType = pointerType;
    }

    public void actionPerformed(final ActionEvent evt) {
      //FIXME have to do morphstr logic here
      final String inputString = BrowserPanel.this.searchField.getText().trim();
      Word word = BrowserPanel.this.dictionary().lookupWord(inputString, pos);
      if (word == null) {
        final List<String> forms = dictionary().lookupBaseForms(inputString, pos);
        assert forms.isEmpty() == false : "searchField contents must have changed";
        word = BrowserPanel.this.dictionary().lookupWord(forms.get(0), pos);
        assert forms.isEmpty() == false;
      }
      if (pointerType == null) {
        //FIXME bad form to use stderr
        System.err.println(word);
        displaySenses(word);
      } else {
        displaySenseChain(word, pointerType);
      }
    }
  } // end class PointerTypeAction

  /**
   * Displays information related to a given POS + PointerType
   */
  class VerbFramesAction extends AbstractAction {
    private static final long serialVersionUID = 1L;

    VerbFramesAction(final String label) {
      super(label);
    }

    public void actionPerformed(final ActionEvent evt) {
      //FIXME have to do morphstr logic here
      final String inputString = BrowserPanel.this.searchField.getText().trim();
      Word word = BrowserPanel.this.dictionary().lookupWord(inputString, POS.VERB);
      if (word == null) {
        final List<String> forms = dictionary().lookupBaseForms(inputString, POS.VERB);
        assert forms.isEmpty() == false : "searchField contents must have changed";
        word = BrowserPanel.this.dictionary().lookupWord(forms.get(0), POS.VERB);
        assert forms.isEmpty() == false;
      }
      displayVerbFrames(word);
    }
  } // end class VerbFramesAction

  private void makePOSComboBoxes() {
    this.posBoxes = new EnumMap<POS, PointerTypeComboBox>(POS.class);

    for (final POS pos : POS.CATS) {
      final PointerTypeComboBox comboBox = new PointerTypeComboBox(pos);

      comboBox.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, 0, false), "Slash");
      comboBox.getActionMap().put("Slash", slashAction);

      this.posBoxes.put(pos, comboBox);
      comboBox.setEnabled(false);
    }
  }

  // used by substring search panel
  // FIXME synchronization probably insufficient
  synchronized void setWord(final Word word) {
    searchField.setText(word.getLemma());
    displayOverview();
  }

  // 
  private synchronized void preload() {
    final Runnable preloader = new Runnable() {
      public void run() {
        // issue search for word which occurs as all POS to
        // so all data files will be preloaded
        // some words in all 4 pos
        //   clear, down, fast, fine, firm, flush, foward, second, 
        // Note: lookupWord() only touches index.<pos> files
        final String inputString = "clear";
        for (final POS pos : POS.CATS) {
          final List<String> forms = dictionary().lookupBaseForms(inputString, pos);
          for (final String form : forms) {
            final Word word = dictionary().lookupWord(form, pos);
            word.toString();
          }
        }
      }
    };
    new Thread(preloader).start();
  }

  /**
   * Generic search and output generation code
   */
  private synchronized void displayOverview() {
    // TODO normalize internal space
    final String inputString = searchField.getText().trim();
    this.displayedValue = inputString;
    if (inputString.length() == 0) {
      resultEditorPane.setFocusable(false);
      updateStatusBar(Status.INTRO);
      for (final PointerTypeComboBox comboBox : this.posBoxes.values()) {
        comboBox.setEnabled(false);
      }
      resultEditorPane.setText("");
      return;
    }
    resultEditorPane.setFocusable(true);
    // generate overview output
    final StringBuilder buffer = new StringBuilder();
    boolean definitionExists = false;
    for (final POS pos : POS.CATS) {
      List<String> forms = dictionary().lookupBaseForms(inputString, pos);
      if (forms == null) {
        forms = ImmutableList.of(inputString);
      } else {
        //XXX debug crap
        boolean found = false;
        for (final String form : forms) {
          if (form.equals(inputString)) {
            found = true;
            break;
          }
        }
        if (forms.isEmpty() == false && found == false) {
          System.err.println("    BrowserPanel inputString: \"" + inputString +
            "\" not found in forms: " + forms);
        }
      }
      boolean enabled = false;
      //XXX System.err.println("  BrowserPanel forms: \""+Arrays.asList(forms)+"\" pos: "+pos);
      final SortedSet<String> noCaseForms = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
      for (final String form : forms) {
        if (noCaseForms.contains(form)) {
          // block no case dups ("hell"/"Hell", "villa"/"Villa")
          continue;
        }
        noCaseForms.add(form);
        final Word word = dictionary().lookupWord(form, pos);
        //XXX System.err.println("  BrowserPanel form: \""+form+"\" pos: "+pos+" Word found?: "+(word != null));
        enabled |= (word != null);
        appendSenses(word, buffer, false);
        if (word != null) {
          buffer.append("<hr>");
        }
        if (word != null) {
          posBoxes.get(pos).updateFor(pos, word);
        }
      }
      posBoxes.get(pos).setEnabled(enabled);
      definitionExists |= enabled;
    }

    if (definitionExists) {
      updateStatusBar(Status.OVERVIEW, inputString);
      resultEditorPane.setText(buffer.toString());
      resultEditorPane.setCaretPosition(0); // scroll to top
    } else {
      resultEditorPane.setText("");
      updateStatusBar(Status.NO_MATCHES);
    }
    searchField.selectAll();
  }

  /**
   * Function object used to show status of user interaction as text at the bottom
   * of the main window.
   */
  private enum Status {
    INTRO("Enter search word and press return"),
    OVERVIEW("Overview of %s"),
    SEARCHING("Searching..."),
    SYNONYMS("Synonyms search for %s \"%s\""),
    NO_MATCHES("No matches found."),
    POINTER("\"%s\" search for %s \"%s\""),
    VERB_FRAMES("Verb Frames search for verb \"%s\"");
    private final String formatString;

    private Status(final String formatString) {
      this.formatString = formatString;
    }

    String get(Object... args) {
      if (this == POINTER) {
        final PointerType pointerType = (PointerType) args[0];
        final POS pos = (POS) args[1];
        final String lemma = (String) args[2];
        return String.format(formatString,
          String.format(pointerType.getFormatLabel(pos), lemma),
          pos.getLabel(),
          lemma);
      } else {
        return String.format(formatString, args);
      }
    }
  } // end enum Status

  // TODO For PointerType searches, show same text as combo box (e.g., "running"
  // not "run" - lemma is clear)
  private void updateStatusBar(final Status status, final Object... args) {
    this.statusLabel.setText(status.get(args));
  }

  /** Overview for single {@code Word} */
  private synchronized void displaySenses(final Word word) {
    updateStatusBar(Status.SYNONYMS, word.getPOS().getLabel(), word.getLemma());
    final StringBuilder buffer = new StringBuilder();
    appendSenses(word, buffer, true);
    resultEditorPane.setText(buffer.toString());
    resultEditorPane.setCaretPosition(0); // scroll to top
  }

  /**
   * Core search routine; renders all information about {@code Word} into {@code buffer}
   * as HTML.
   * 
   * TODO
   * Factor out this logic into a result data structure like findtheinfo_ds() does
   * to separate logic from presentation.
   * Nice XML format would open up some nice possibilities for web services, commandline,
   * and this traditional GUI application.
   */
  private void appendSenses(final Word word, final StringBuilder buffer, final boolean verbose) {
    if (word == null) {
      return;
    }
    final List<Synset> senses = word.getSynsets();
    final int taggedCount = word.getTaggedSenseCount();
    buffer.append("The ").append(word.getPOS().getLabel()).
      append(" <b>").append(word.getLemma()).append("</b> has ").
      append(senses.size()).append(" sense").append(senses.size() == 1 ? "" : "s").
      append(' ').
      append('(');
    if (taggedCount == 0) {
      buffer.append("no senses from tagged texts");
    } else {
      buffer.append("first ").append(taggedCount).append(" from tagged texts");
    }
    buffer.append(")<br>\n");
    buffer.append("<ol>\n");
    for (final Synset sense : senses) {
      buffer.append("<li>");
      final int cnt = sense.getWordSense(word).getSensesTaggedFrequency();
      if (cnt != 0) {
        buffer.append('(');
        buffer.append(cnt);
        buffer.append(") ");
      }
      if (word.getPOS() != POS.ADJ) {
        buffer.append("&lt;");
        // strip POS off of lex cat (up to first period)
        String posFreeLexCat = sense.getLexCategory();
        final int periodIdx = posFreeLexCat.indexOf('.');
        assert periodIdx > 0;
        posFreeLexCat = posFreeLexCat.substring(periodIdx + 1);
        buffer.append(posFreeLexCat);
        buffer.append("&gt; ");
      }
      //XXX how do you get to/from the satellite
      //from http://wordnet.princeton.edu/man/wn.1WN:
      //  "if searchstr is in a head synset, all of the head synset's satellites"
      buffer.append(sense.getLongDescription(verbose));
      if (verbose) {
        final List<PointerTarget> similarTos = sense.getTargets(PointerType.SIMILAR_TO);
        if (similarTos.isEmpty() == false) {
          buffer.append("<br>\n");
          buffer.append("Similar to:");
          buffer.append("<ul>\n");
          for (final PointerTarget similarTo : similarTos) {
            buffer.append(listOpen());
            final Synset targetSynset = (Synset) similarTo;
            buffer.append(targetSynset.getLongDescription(verbose));
            buffer.append("</li>\n");
          }
          buffer.append("</ul>\n");
        }

        final List<PointerTarget> seeAlsos = sense.getTargets(PointerType.SEE_ALSO);
        if (seeAlsos.isEmpty() == false) {
          if (similarTos.isEmpty()) {
            buffer.append("<br>");
          }
          buffer.append("Also see: ");
          int seeAlsoNum = 0;
          for (final PointerTarget seeAlso : seeAlsos) {
            buffer.append(seeAlso.getDescription());
            for (final WordSense wordSense : seeAlso) {
              buffer.append('#');
              buffer.append(wordSense.getSenseNumber());
            }
            if (seeAlsoNum == 0) {
              buffer.append("; ");
            }
            seeAlsoNum++;
          }
        }
      }
      buffer.append("</li>\n");
    }
    buffer.append("</ol>\n");
  }

  /**
   * Renders single {@code Word + PointerType}.  Calls recursive {@linkplain #appSenseChain()} method for
   * each applicable sense.
   */
  private void displaySenseChain(final Word word, final PointerType pointerType) {
    updateStatusBar(Status.POINTER, pointerType, word.getPOS(), word.getLemma());
    final StringBuilder buffer = new StringBuilder();
    final List<Synset> senses = word.getSynsets();
    // count number of senses pointerType applies to
    int numApplicableSenses = 0;
    for (int i = 0, n = senses.size(); i < n; i++) {
      if (senses.get(i).getTargets(pointerType).isEmpty() == false) {
        numApplicableSenses++;
      }
    }
    buffer.append("Applies to ").append(numApplicableSenses).append(" of the ").
      append(senses.size()).append(" senses").//(senses.length > 1 ? "s" : "")+
      append(" of <b>").append(word.getLemma()).append("</b>\n");
    for (int i = 0, n = senses.size(); i < n; i++) {
      if (senses.get(i).getTargets(pointerType).isEmpty() == false) {
        buffer.append("<br><br>Sense ").append(i + 1).append('\n');

        // honestly, I don't even know why there are 2 PointerTypes here ??
//        PointerType inheritanceType = PointerType.HYPERNYM;
//        PointerType attributeType = pointerType;
//
//        if (pointerType.equals(inheritanceType) || pointerType.isSymmetricTo(inheritanceType)) {
//          // either pointerType == PointerType.HYPERNYM
//          // or pointerType is isSymmetricTo(PointerType.HYPERNYM) currently is only HYPONYM
//          inheritanceType = pointerType;
//          attributeType = null;
//        }
        // else inheritanceType remains hypernym and the sought type remains attributeType
        // maybe this is wrong if sought pointerType is
        // PointerType.INSTANCE_HYPONYM or PointerType.INSTANCE_HYPERNYM ??
        // neither of these are recursive

        //input: HYPONYM
        //inheritanceType: HYPONYM
        //attributeType: null

        //input: INSTANCE_HYPERNYM
        //inheritanceType:  * ideally, this would become HYPERNYM?
        //attributeType: null

        //take2
        PointerType inheritanceType = PointerType.HYPERNYM;
        PointerType attributeType = pointerType;
        switch (pointerType) {
          case HYPONYM:
          //case INSTANCE_HYPONYM:
          case HYPERNYM:
          //case INSTANCE_HYPERNYM:
            inheritanceType = pointerType;
            attributeType = null;
        }

        System.err.println(word + " inheritanceType: " + inheritanceType + 
          " attributeType: " + attributeType + " pointerType: " + pointerType);
        buffer.append("<ul>\n");
        appendSenseChain(buffer, senses.get(i).getWordSense(word), senses.get(i), inheritanceType, attributeType);
        buffer.append("</ul>\n");
      }
    }
    resultEditorPane.setText(buffer.toString());
    resultEditorPane.setCaretPosition(0); // scroll to top
  }

  /**
   * Adds information from {@code Pointer}s.  Base method signature of recursive method
   * {@linkplain #appendSenseChain()}.
   */
  void appendSenseChain(
    final StringBuilder buffer,
    final WordSense rootWordSense,
    final PointerTarget sense,
    final PointerType inheritanceType,
    final PointerType attributeType) {
    appendSenseChain(buffer, rootWordSense, sense, inheritanceType, attributeType, 0, null);
  }

  private String listOpen() {
    return "<li>";
  //return "<li>• ";
  //return "<li>\u2022 ";
  //XXX return "<li>* ";
  }

  /**
   * Recursivly adds information from {@code Pointer}s.
   */
  void appendSenseChain(
    final StringBuilder buffer,
    final WordSense rootWordSense,
    final PointerTarget sense,
    final PointerType inheritanceType,
    final PointerType attributeType,
    final int tab,
    Link ancestors) {
    buffer.append(listOpen());
    buffer.append(sense.getLongDescription());
    buffer.append("</li>\n");

    if (attributeType != null) {
      for (final Pointer pointer : sense.getPointers(attributeType)) {
        final PointerTarget target = pointer.getTarget();
        final boolean srcMatch;
        if (pointer.isLexical()) {
          srcMatch = pointer.getSource().equals(rootWordSense);
        } else {
          srcMatch = pointer.getSource().getSynset().equals(rootWordSense.getSynset());
        }
        if (srcMatch == false) {
          System.err.println("rootWordSense: " + rootWordSense +
            " inheritanceType: " + inheritanceType + " attributeType: " + attributeType);
          System.err.println(pointer);
          continue;
        }
        buffer.append("<li>");
        if (target instanceof WordSense) {
          assert pointer.isLexical();
          final WordSense wordSense = (WordSense) target;
          //FIXME RELATED TO label below only right for DERIVATIONALLY_RELATED
          buffer.append("RELATED TO → (").append(wordSense.getPOS().getLabel()).
            append(") ").append(wordSense.getLemma()).append('#').append(wordSense.getSenseNumber());
          //ANTONYM example:
          //Antonym of dissociate (Sense 2)
          buffer.append("<br>\n");
        } else {
          buffer.append("POINTER TARGET ");
        }
        buffer.append(target.getSynset().getLongDescription());
        buffer.append("</li>\n");
      }

      // Don't get ancestors for these relationships.
      if (NON_RECURSIVE_POINTER_TYPES.contains(attributeType)) {
        System.err.println("NON_RECURSIVE_POINTER_TYPES "+attributeType);
        return;
      }
    }
    if (ancestors == null || ancestors.contains(sense) == false) {
      System.err.println("ancestors == null || does not contain sense "+sense+
        " "+attributeType+" ancestors: "+ancestors);
      ancestors = new Link(sense, ancestors);
      for (final PointerTarget parent : sense.getTargets(inheritanceType)) {
        buffer.append("<ul>\n");
        appendSenseChain(buffer, rootWordSense, parent, inheritanceType, attributeType, tab + 1, ancestors);
        buffer.append("</ul>\n");
      }
    } else {
      System.err.println("ancestors != null || contains sense "+sense+" "+attributeType);
    }
  }
  //FIXME red DERIVATIONALLY_RELATED shows Sense 2 which has no links!?
  private static final EnumSet<PointerType> NON_RECURSIVE_POINTER_TYPES = EnumSet.of(
    PointerType.DERIVATIONALLY_RELATED,
    PointerType.MEMBER_OF_TOPIC_DOMAIN, PointerType.MEMBER_OF_USAGE_DOMAIN, PointerType.MEMBER_OF_REGION_DOMAIN,
    PointerType.DOMAIN_OF_TOPIC, PointerType.DOMAIN_OF_USAGE, PointerType.DOMAIN_OF_REGION,
    PointerType.ANTONYM);

  private void displayVerbFrames(final Word word) {
    updateStatusBar(Status.VERB_FRAMES, word.getLemma());
    final StringBuilder buffer = new StringBuilder();
    final List<Synset> senses = word.getSynsets();
    buffer.append(senses.size()).append(" sense").append((senses.size() > 1 ? "s" : "")).
      append(" of <b>").append(word.getLemma()).append("</b>\n");
    for (int i = 0, n = senses.size(); i < n; i++) {
      if (senses.get(i).getWordSense(word).getVerbFrames().isEmpty() == false) {
        buffer.append("<br><br>Sense ").append(i + 1).append('\n');
        //TODO show the synset ?
        buffer.append("<ul>\n");
        for (final String frame : senses.get(i).getWordSense(word).getVerbFrames()) {
          buffer.append(listOpen());
          buffer.append(frame);
          buffer.append("</li>\n");
        }
        buffer.append("</ul>\n");
      }
    }
    resultEditorPane.setText(buffer.toString());
    resultEditorPane.setCaretPosition(0); // scroll to top
  }

  //FIXME pretty old-fashioned and error prone.  List ? LinkedList ?
  private static class Link {
    private final PointerTarget pointerTarget;
    private final Link link;

    Link(final PointerTarget pointerTarget, final Link link) {
      this.pointerTarget = pointerTarget;
      this.link = link;
    }

    boolean contains(final PointerTarget object) {
      for (Link head = this; head != null; head = head.link) {
        if (head.pointerTarget.equals(object)) {
          return true;
        }
      }
      return false;
    }
  } // end class Link
}
