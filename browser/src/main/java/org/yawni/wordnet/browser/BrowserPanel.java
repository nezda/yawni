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

import org.yawni.util.Utils;
import org.yawni.wordnet.WordNetInterface;
import org.yawni.wordnet.WordNet;
import org.yawni.wordnet.POS;
import org.yawni.wordnet.Relation;
import org.yawni.wordnet.RelationTarget;
import org.yawni.wordnet.RelationType;
import org.yawni.wordnet.Synset;
import org.yawni.wordnet.Word;
import org.yawni.wordnet.WordSense;
import static org.yawni.wordnet.RelationType.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.undo.*;
import org.yawni.util.WordCaseUtils;
//import java.util.prefs.*;

/**
 * The main panel of browser.
 */
public class BrowserPanel extends JPanel {
  private static final Logger log = LoggerFactory.getLogger(BrowserPanel.class.getName());
//  private static Preferences prefs = Preferences.userNodeForPackage(BrowserPanel.class).node(BrowserPanel.class.getSimpleName());
  WordNetInterface wordNet() {
    return WordNet.getInstance();
  }
  private final Browser browser;
  private String currentlyDisplayedValue;

  private static final int MENU_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
  private final JTextField searchField;
  private final JButton searchButton;
  private final UndoManager undoManager;
  private final UndoAction undoAction;
  private final RedoAction redoAction;
  private final StyledTextPane resultEditorPane;
  private final EnumMap<POS, RelationTypeComboBox> posBoxes;
  private final Action slashAction;
  private final TextPrompt textPrompt;
  private final JLabel statusLabel;

  BrowserPanel(final Browser browser) {
    this.browser = browser;
    this.setName(getClass().getName());
    super.setLayout(new BorderLayout());

    this.searchField = new JTextField();
    this.searchField.setName("searchField");
    SearchFrame.multiClickSelectAll(searchField);
    this.searchField.setDocument(new SearchFieldDocument());
    this.searchField.setBackground(Color.WHITE);

    this.searchField.putClientProperty("JTextField.variant", "search");
    this.searchField.putClientProperty("JTextField.Search.CancelAction",
      ActionHelper.clear()
      );

    this.undoManager = new UndoManager() {
      @Override
      public boolean addEdit(UndoableEdit ue) {
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
      @Override
      public boolean verify(JComponent input) {
        final JTextField searchField = (JTextField) input;
        // if the text in this field is different from the
        // text which the menus are currently for, need to
        // re-issue the search
        final String inputString = searchField.getText().trim();
        if (! inputString.equals(currentlyDisplayedValue)) {
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
      public void actionPerformed(final ActionEvent event) {
        if (event.getSource() == searchField) {
          // doClick() will generate another event via searchButton
          searchButton.doClick();
          return;
        }
        displayOverview();
      }
    };
    this.searchButton = new JButton(searchAction);
    this.searchButton.setName("searchButton");
    this.searchButton.setFocusable(false);
    this.searchButton.getActionMap().put("Search", searchAction);

    this.slashAction = new AbstractAction("Slash") {
      public void actionPerformed(final ActionEvent event) {
        searchField.grabFocus();
      }
    };

    this.posBoxes = makePOSComboBoxes();

    final JPanel searchAndRelationsPanel = new JPanel(new GridBagLayout());
    final GridBagConstraints c = new GridBagConstraints();

    c.gridy = 0;
    c.gridx = 0;
    // Top,Left,Bottom,Right
    c.insets = new Insets(3, 3, 0, 3);
    c.fill = GridBagConstraints.HORIZONTAL;
    //c.anchor = GridBagConstraints.WEST;
    c.gridwidth = 4;
    final Box searchPanel = new Box(BoxLayout.X_AXIS);

    searchPanel.add(searchField);
    searchPanel.add(Box.createHorizontalStrut(3));
    searchPanel.add(searchButton);
    searchAndRelationsPanel.add(searchPanel, c);

    c.fill = GridBagConstraints.NONE;
    c.gridwidth = 1;

    c.gridy = 1;
    c.gridx = 0;
    c.insets = new Insets(3, 3, 3, 3);
    searchAndRelationsPanel.add(this.posBoxes.get(POS.NOUN), c);
    c.gridx = 1;
    searchAndRelationsPanel.add(this.posBoxes.get(POS.VERB), c);
    c.gridx = 2;
    searchAndRelationsPanel.add(this.posBoxes.get(POS.ADJ), c);
    c.gridx = 3;
    c.insets = new Insets(3, 0, 3, 3);
    searchAndRelationsPanel.add(this.posBoxes.get(POS.ADV), c);

    // set width(relationPanel) = width(searchPanel)

    this.add(searchAndRelationsPanel, BorderLayout.NORTH);

    this.resultEditorPane = new StyledTextPane();
    this.resultEditorPane.setName("resultEditorPane");

    textPrompt = new TextPrompt(
      "Type a word to lookup in WordNet…", searchField, resultEditorPane);
    //textPrompt.changeAlpha(0.5f);
    final Color disabledControlTextColor = new Color(108, 108, 108);
    textPrompt.setForeground(disabledControlTextColor);
    textPrompt.setName("textPrompt");

    this.resultEditorPane.setBorder(browser.textAreaBorder());
    this.resultEditorPane.setBackground(Color.WHITE);
    // http://www.groupsrv.com/computers/about179434.html
    // enables scrolling with arrow keys
    this.resultEditorPane.setEditable(false);
    final JScrollPane jsp = new JScrollPane(resultEditorPane);
    final JScrollBar jsb = jsp.getVerticalScrollBar();

    //TODO move to StyledTextPane (already an action for this?)
    final Action scrollDown = new AbstractAction() {
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

    final String[] extraKeys = new String[] {
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

      for (final RelationTypeComboBox comboBox : this.posBoxes.values()) {
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
//    jsp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    this.add(jsp, BorderLayout.CENTER);
    this.statusLabel = new JLabel();
    this.statusLabel.setName("statusLabel");
    // default value
//    this.statusLabel.setVerticalAlignment(SwingConstants.CENTER);
    this.statusLabel.setBorder(BorderFactory.createEmptyBorder(0 /*top*/, 3 /*left*/, 3 /*bottom*/, 0 /*right*/));
    this.add(this.statusLabel, BorderLayout.SOUTH);
    updateStatusBar(Status.INTRO);

    this.searchField.addActionListener(searchAction);

    validate();
    preload();
  }

  private static Icon createUndoIcon() {
    final ImageIcon icon = new ImageIcon(BrowserPanel.class.getResource("Undo.png"));
    assert icon.getImageLoadStatus() == MediaTracker.COMPLETE;
    assert icon.getIconWidth() > 0 && icon.getIconHeight() > 0;
    final int height = 18;
    final int width = -1; // maintains aspect ratio
    final Image img = icon.getImage();
    return new ImageIcon(img.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH));
  }

  private static Icon createRedoIcon() {
    final ImageIcon icon = new ImageIcon(BrowserPanel.class.getResource("Redo.png"));
    assert icon.getImageLoadStatus() == MediaTracker.COMPLETE;
    assert icon.getIconWidth() > 0 && icon.getIconHeight() > 0;
    final int height = 18;
    final int width = -1; // maintains aspect ratio
    final Image img = icon.getImage();
    return new ImageIcon(img.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH));
  }

  static ImageIcon createFindIcon(final int dimension) {
    final ImageIcon icon = new ImageIcon(BrowserPanel.class.getResource("FindIcon.png"));
    assert icon.getImageLoadStatus() == MediaTracker.COMPLETE;
    assert icon.getIconWidth() > 0 && icon.getIconHeight() > 0;
    final int height = dimension;
    final int width = -1; // maintains aspect ratio
    final Image img = icon.getImage();
    return new ImageIcon(img.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH));
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
    browser.setFocusTraversalPolicy(new SimpleFocusTraversalPolicy(components));
  }

  // causes problems on GTK + Linux
//  @Override
//  public void setVisible(final boolean visible) {
//    super.setVisible(visible);
//    if (visible) {
//      final boolean gotFocus = searchField.requestFocusInWindow();
//      if (! gotFocus) {
//        log.error("searchField.requestFocusInWindow() failed!");
//      }
//    }
//  }

  synchronized String getSearchText() {
    return searchField.getText();
  }

  // non-static class UndoAction cross references RedoAction and
  // other non-static fields
  class UndoAction extends AbstractAction {
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
        log.error("Unable to undo: {}", ex, ex);
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
        log.error("Unable to redo: {}", ex, ex);
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
   * Encapsulates a button (for a POS) which controls a
   * menu that is dynamically populated with {@link RelationTypeAction}(s).
   * - handles Slash (search)
   * - interactive updates via updateFor()
   */
  private class RelationTypeComboBox extends PopdownButton {
    // FIXME if user changes text field contents and selects menu, bad things will happen
    // FIXME text in HTML pane looks bold at line wraps
    private final POS pos;

    RelationTypeComboBox(final POS pos) {
      //super(Utils.capitalize(pos.getLabel())+" \u25BE\u25bc"); // large: \u25BC ▼ small: \u25BE ▾
      super(Utils.capitalize(pos.getLabel()));
      this.setName("RelationTypeComboBox::"+getText());
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
              RelationTypeComboBox.this.doClick();
              slashAction.actionPerformed(null);
              break;
            case '\t':
              //System.err.println("menu evt: tab");
              hidePopupMenu();
              break;
          }
        // if tab, move focus to next thing
        }
      });
    }

    /** populate with {@code RelationType}s which apply to pos+word */
    void updateFor(final POS pos, final Word word) {
      getPopupMenu().removeAll();
      getPopupMenu().add(new RelationTypeAction("Senses", pos, null));
      for (final RelationType relationType : word.getRelationTypes()) {
        // use word+pos custom labels for drop downs
        final String label = String.format(relationType.getFormatLabel(word.getPOS()), word.getLowercasedLemma());
        //System.err.println("label: "+label+" word: "+word+" relationType: "+relationType);
        final JMenuItem item = getPopupMenu().add(new RelationTypeAction(label, pos, relationType));
      }
      if (pos == POS.VERB) {
        // use word+pos custom labels for drop downs
        final String label = String.format("Sentence frames for verb %s", word.getLowercasedLemma());
        //System.err.println("label: "+label+" word: "+word+" relationType: "+relationType);
        final JMenuItem item = getPopupMenu().add(new VerbFramesAction(label));
      }
    }
  } // end class RelationTypeComboBox

  /**
   * Displays information related to a given {@linkplain POS} + {@linkplain RelationType}
   */
  private class RelationTypeAction extends AbstractAction {
    private final POS pos;
    private final RelationType relationType;

    RelationTypeAction(final String label, final POS pos, final RelationType relationType) {
      super(label);
      this.pos = pos;
      this.relationType = relationType;
    }

    @Override
    public String toString() {
      return "[RelationTypeAction "+relationType+" "+pos+"]";
    }

    public void actionPerformed(final ActionEvent evt) {
      final SwingWorker worker = new SwingWorker<Void, Void>() {
        @Override
        public Void doInBackground() {
          final String inputString = BrowserPanel.this.searchField.getText().trim();
          Word word = BrowserPanel.this.wordNet().lookupWord(inputString, pos);
          if (word == null) {
            final List<String> forms = wordNet().lookupBaseForms(inputString, pos);
            assert ! forms.isEmpty() : "searchField contents must have changed";
            word = BrowserPanel.this.wordNet().lookupWord(forms.get(0), pos);
           }
          if (relationType == null) {
            //FIXME bad form to use stderr
            System.err.println(word);
            log.info("{}", word);
            displaySenses(word);
          } else {
            displaySenseChain(word, relationType);
          }
          return null;
        }

        @Override
        protected void done() {
          try {
            get();
          } catch (InterruptedException ignore) {
          } catch (java.util.concurrent.ExecutionException ee) {
            throw new RuntimeException(ee);
          }
        }
      };
      worker.execute();
    }
  } // end class RelationTypeAction
  
  /**
   * Displays information related to a given {@linkplain POS} + {@linkplain RelationType}
   */
  class VerbFramesAction extends AbstractAction {
    VerbFramesAction(final String label) {
      super(label);
    }

    public void actionPerformed(final ActionEvent evt) {
      //FIXME have to do morphstr logic here
      final String inputString = BrowserPanel.this.searchField.getText().trim();
      Word word = BrowserPanel.this.wordNet().lookupWord(inputString, POS.VERB);
      if (word == null) {
        final List<String> forms = wordNet().lookupBaseForms(inputString, POS.VERB);
        assert ! forms.isEmpty() : "searchField contents must have changed";
        word = BrowserPanel.this.wordNet().lookupWord(forms.get(0), POS.VERB);
        assert ! forms.isEmpty();
      }
      displayVerbFrames(word);
    }
  } // end class VerbFramesAction

  void dismissPOSComboBoxPopup() {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        for (final POS pos : POS.CATS) {
          final RelationTypeComboBox comboBox = BrowserPanel.this.posBoxes.get(pos);
          comboBox.getPopupMenu().setVisible(false);
        }
      }
    });
  }

  private EnumMap<POS, RelationTypeComboBox> makePOSComboBoxes() {
    final EnumMap<POS, RelationTypeComboBox> newPOSBoxes = new EnumMap<POS, RelationTypeComboBox>(POS.class);
    for (final POS pos : POS.CATS) {
      final RelationTypeComboBox comboBox = new RelationTypeComboBox(pos);
      comboBox.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, 0, false), "Slash");
      comboBox.getActionMap().put("Slash", slashAction);
      newPOSBoxes.put(pos, comboBox);
      comboBox.setEnabled(false);
    }
    return newPOSBoxes;
  }

  // used by substring search panel
  // FIXME synchronization probably insufficient
  synchronized void setWord(final Word word) {
//    searchField.setText(word.getLowercasedLemma());
    searchField.setText(WordCaseUtils.getDominantCasedLemma(word));
    displayOverview();
  }

  private synchronized void preload() {
    final SwingWorker preloadWorker = new SwingWorker<Void, Void>() {
      @Override
      public Void doInBackground() {
        // issue search for word which occurs as all POS to
        // so all data files will be preloaded
        // some words in all 4 pos
        //   clear, down, fast, fine, firm, flush, foward, second,
        // Note: lookupWord() only touches index.<pos> files
        final String inputString = "clear";
        for (final POS pos : POS.CATS) {
          final List<String> forms = wordNet().lookupBaseForms(inputString, pos);
          for (final String form : forms) {
            final Word word = wordNet().lookupWord(form, pos);
            word.toString();
          }
        }
        return null;
      }

      @Override
      protected void done() {
        try {
          get();
        } catch (InterruptedException ignore) {
        } catch (java.util.concurrent.ExecutionException ee) {
          throw new RuntimeException(ee);
        }
      }
    };
    preloadWorker.execute();
  }

  /**
   * Generic search and output generation code
   */

  /**
   * Renders high-level description of all {@linkplain Word}s
   * for all {@linkplain POS} with forms compatible with
   * the current input string.  Includes a short per-{@code POS}
   * summary, deivision between {@code POS} sections, and activating
   * the appropriate {@code POS} relation menu buttons.
   */
  private synchronized void displayOverview() {
    // TODO normalize internal space
    final String inputString = searchField.getText().trim();
    this.currentlyDisplayedValue = inputString;
    if (inputString.length() == 0) {
      resultEditorPane.setFocusable(false);
      updateStatusBar(Status.INTRO);
      for (final RelationTypeComboBox comboBox : this.posBoxes.values()) {
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
      List<String> forms = wordNet().lookupBaseForms(inputString, pos);
      assert forms != null;
      //XXX debug crap
      boolean found = false;
      for (final String form : forms) {
        if (form.equals(inputString)) {
          found = true;
          break;
        }
      }
      if (! forms.isEmpty() && ! found) {
//        System.err.println("    BrowserPanel inputString: \"" + inputString +
//          "\" not found in forms: " + forms);
        log.error("    BrowserPanel inputString: \"{}\" not found in forms: {}",
          inputString, forms);
      }
      boolean enabled = false;
      //XXX System.err.println("  BrowserPanel forms: \""+Arrays.asList(forms)+"\" pos: "+pos);
      final SortedSet<String> noCaseForms = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
      for (final String form : forms) {
        if (noCaseForms.contains(form)) {
          // block no case duplicates ("hell"/"Hell", "villa"/"Villa")
          continue;
        }
        noCaseForms.add(form);
        final Word word = wordNet().lookupWord(form, pos);
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
    } // end POS loop

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
//    INTRO("Enter search word and press return"),
    INTRO(" "), // space
    OVERVIEW("Overview of “%s”"),
    SEARCHING("Searching..."),
    SEARCHING4("Searching...."),
    SEARCHING5("Searching....."),
    SEARCHING6("Searching......"),
    SYNONYMS("Synonyms search for %s \"%s\""),
    NO_MATCHES("No matches found."),
    RELATION("\"%s\" search for %s \"%s\""),
    VERB_FRAMES("Verb Frames search for verb \"%s\"");
    private final String formatString;

    private Status(final String formatString) {
      this.formatString = formatString;
    }

    String get(Object... args) {
      if (this == RELATION) {
        final RelationType relationType = (RelationType) args[0];
        final POS pos = (POS) args[1];
        final String lemma = (String) args[2];
        return String.format(formatString,
          String.format(relationType.getFormatLabel(pos), lemma),
          pos.getLabel(),
          lemma);
      } else {
        return String.format(formatString, args);
      }
    }
  } // end enum Status

  // TODO For RelationType searches, show same text as combo box (e.g., "running"
  // not "run" - lemma is clear)
  private void updateStatusBar(final Status status, final Object... args) {
    final String text = status.get(args);
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
//        if (status == Status.NO_MATCHES) {
//          textPrompt.setText(text);
//          textPrompt.setVisible(true);
//          BrowserPanel.this.statusLabel.setText(" ");
//          return;
//        }
        BrowserPanel.this.statusLabel.setText(text);
      }
    });
  }

  /** Overview for single {@code Word} */
  private synchronized void displaySenses(final Word word) {
    updateStatusBar(Status.SYNONYMS, word.getPOS().getLabel(), word.getLowercasedLemma());
    final StringBuilder buffer = new StringBuilder();
    appendSenses(word, buffer, true);
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        resultEditorPane.setText(buffer.toString());
        resultEditorPane.setCaretPosition(0); // scroll to top
      }
    });
  }

  /**
   * Core search routine; renders all information about {@code Word} into {@code buffer}
   * as HTML.
   * 
   * <h3>TODO</h3>
   * Factor out this logic into a "results" data structure like findtheinfo_ds() does
   * to separate logic from presentation.
   * A nice XML format would open up some nice possibilities for web services, commandline,
   * and this traditional GUI application.
   */
  private void appendSenses(final Word word, final StringBuilder buffer, final boolean verbose) {
    if (word == null) {
      return;
    }
    final List<Synset> senses = word.getSynsets();
    final int taggedCount = word.getTaggedSenseCount();
    buffer.append("The ").append("<span class=\"pos\">").append(word.getPOS().getLabel()).append("</span>").
      append(" <span class=\"summaryWord\">").append(WordCaseUtils.getDominantCasedLemma(word)).append("</span> has ").
      append(senses.size()).append(" sense").append(senses.size() == 1 ? "" : "s").
      append(' ').
      append('(');
    if (taggedCount == 0) {
      buffer.append("none from tagged texts");
    } else {
      if (taggedCount == senses.size()) {
        if (taggedCount == 2) {
          buffer.append("both");
        } else {
          buffer.append("all");
        }
      } else {
        buffer.append("first ").append(taggedCount);
      }
      buffer.append(" from tagged texts");
    }
    buffer.append(")<br>\n");
    buffer.append("<ol>\n");
    for (final Synset sense : senses) {
      buffer.append("<li>");
      final int coreRank = sense.getWordSense(word).getCoreRank();
      if (coreRank > 0) {
        buffer.append('[');
        buffer.append(coreRank);
        buffer.append("] ");
      }
//      buffer.append(' ');
//      buffer.append(sense.getWordSense(word).getSenseKey());
//      buffer.append(" ");
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
      buffer.append(sense.getLongDescription(verbose));
      if (verbose) {
        final List<RelationTarget> similarTos = sense.getRelationTargets(SIMILAR_TO);
        if (! similarTos.isEmpty()) {
          buffer.append("<br>\n");
          buffer.append("Similar to:");
          buffer.append("<ul>\n");
          for (final RelationTarget similarTo : similarTos) {
            buffer.append(listOpen());
            final Synset targetSynset = (Synset) similarTo;
            buffer.append(targetSynset.getLongDescription(verbose));
            buffer.append("</li>\n");
          }
          buffer.append("</ul>\n");
        }

        final List<RelationTarget> seeAlsos = sense.getRelationTargets(SEE_ALSO);
        if (! seeAlsos.isEmpty()) {
          if (similarTos.isEmpty()) {
            buffer.append("<br>");
          }
          buffer.append("Also see: ");
          int seeAlsoNum = 0;
          for (final RelationTarget seeAlso : seeAlsos) {
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
   * Renders single {@code Word + RelationType}; calls recursive {@link #appendSenseChain()} method for
   * each applicable sense.
   */
  private void displaySenseChain(final Word word, final RelationType relationType) {
    final StringBuilder buffer = new StringBuilder();
    // some relationTypes only apply to WordSenses, not Synsets (e.g., RelationType.DERIVATIONALLY_RELATED)
//    final List<Synset> senses = word.getSynsets();
    final List<WordSense> senses = word.getWordSenses();
    // count number of senses relationType applies to
    int numApplicableSenses = 0;
    for (int i = 0, n = senses.size(); i < n; i++) {
      if (! senses.get(i).getRelationTargets(relationType).isEmpty()) {
        numApplicableSenses++;
      }
    }
    assert numApplicableSenses > 0 : "numApplicableSenses == 0 "+"word: "+word+" relationType: "+relationType;
    buffer.append("Applies to ");
    final boolean appliesTooAllSenses = numApplicableSenses == senses.size();
    if (appliesTooAllSenses) {
      if (senses.size() == 1) {
        buffer.append("the only");
      } else if (senses.size() == 2) {
        buffer.append("both");
      } else {
        buffer.append("all ").append(numApplicableSenses);
      }
    } else {
      buffer.append(numApplicableSenses).append(" of the ").append(senses.size());
    }
    buffer.append(" sense").append(senses.size() > 1 ? "s" : "");
    buffer.append(" of <span class=\"summaryWord\">").append(WordCaseUtils.getDominantCasedLemma(word)).append("</span>\n");
    for (int i = 0, n = senses.size(); i < n; i++) {
      if (! senses.get(i).getRelationTargets(relationType).isEmpty()) {
        buffer.append("<br><br>Sense ").append(i + 1).append('\n');

        RelationType inheritanceType = HYPERNYM;
        RelationType attributeType = relationType;
        switch (relationType) {
          case HYPERNYM:
          case INSTANCE_HYPERNYM:
            inheritanceType = HYPERNYM;
            attributeType = INSTANCE_HYPERNYM;
            break;
          case HYPONYM:
          case INSTANCE_HYPONYM:
            inheritanceType = HYPONYM;
            attributeType = INSTANCE_HYPONYM;
            break;
        }
//        if (relationType.equals(inheritanceType) || relationType.isSymmetricTo(inheritanceType)) {
//          // either relationType == RelationType.HYPERNYM
//          // or relationType is isSymmetricTo(RelationType.HYPERNYM) currently is only HYPONYM
//          inheritanceType = relationType;
//          attributeType = null;
//        }

//        System.err.println(word + " inheritanceType: " + inheritanceType +
//          " attributeType: " + attributeType + " relationType: " + relationType);
        buffer.append("<ul>\n");
        appendSenseChain(buffer, senses.get(i).getWordSense(word), senses.get(i), inheritanceType, attributeType);
        buffer.append("</ul>\n");
      }
    }
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        resultEditorPane.setText(buffer.toString());
        resultEditorPane.setCaretPosition(0); // scroll to top
        updateStatusBar(Status.RELATION, relationType, word.getPOS(), word.getLowercasedLemma());
      }
    });
  }

  /**
   * Adds information from {@linkplain Relation}s; base method signature of recursive method
   * {@linkplain #appendSenseChain()}.
   */
  private void appendSenseChain(
    final StringBuilder buffer,
    final WordSense rootWordSense,
    final RelationTarget sense,
    final RelationType inheritanceType,
    final RelationType attributeType) {
    updateStatusBar(Status.SEARCHING);
    counter.set(0);
    appendSenseChain(buffer, rootWordSense, sense, inheritanceType, attributeType, 0, null);
  }

  private String listOpen() {
    return "<li>";
//  return "<li>• ";
//  return "<li>\u2022 ";
//  XXX return "<li>* ";
  }

  private static final AtomicInteger counter = new AtomicInteger();

  /**
   * Recursively adds information from {@code Relation}s to {@code buffer}.
   */
  private void appendSenseChain(
    final StringBuilder buffer,
    final WordSense rootWordSense,
    final RelationTarget sense,
    final RelationType inheritanceType,
    final RelationType attributeType,
    final int tab,
    Link ancestors) {

    // could go with spinner
    // \|/-\|/-\|/
    // TODO just go with standard indeterminate progress indicator
    final int currCount = counter.incrementAndGet();
    if (currCount == 10) {
      updateStatusBar(Status.SEARCHING4);
    } else if (currCount == 20) {
      updateStatusBar(Status.SEARCHING5);
    } else if (currCount == 30) {
      updateStatusBar(Status.SEARCHING6);
      counter.set(0);
    }

    buffer.append(listOpen());
    buffer.append(sense.getLongDescription());
    buffer.append("</li>\n");

    if (attributeType != null) {
      for (final Relation relation : sense.getRelations(attributeType)) {
        final RelationTarget target = relation.getTarget();
        final boolean srcMatch;
        if (relation.isLexical()) {
          srcMatch = relation.getSource().equals(rootWordSense);
        } else {
          srcMatch = relation.getSource().getSynset().equals(rootWordSense.getSynset());
        }
        if (! srcMatch) {
//          System.err.println("rootWordSense: " + rootWordSense +
//            " inheritanceType: " + inheritanceType + " attributeType: " + attributeType);
          System.err.println(">"+relation);
          //continue;
        }
        buffer.append("<li>");
        if (target instanceof WordSense) {
          assert relation.isLexical();
          final WordSense wordSense = (WordSense) target;
          //FIXME RELATED TO label below only right for DERIVATIONALLY_RELATED
          buffer.append("RELATED TO → (").append(wordSense.getPOS().getLabel()).
            append(") ").append(wordSense.getLemma()).append('#').append(wordSense.getSenseNumber());
          //ANTONYM example:
          //Antonym of dissociate (Sense 2)
          buffer.append("<br>\n");
        } else {
          buffer.append("RELATION TARGET ");
        }
        buffer.append(target.getSynset().getLongDescription());
        buffer.append("</li>\n");
      }

      // Don't get ancestors for these relationships.
      if (NON_RECURSIVE_RELATION_TYPES.contains(attributeType)) {
        System.err.println("NON_RECURSIVE_RELATION_TYPES "+attributeType);
        return;
      }
    }
    if (ancestors == null || ! ancestors.contains(sense)) {
//      System.err.println("ancestors == null || does not contain sense "+sense+
//        " "+attributeType+" ancestors: "+ancestors);
      ancestors = new Link(sense, ancestors);
      for (final RelationTarget parent : sense.getRelationTargets(inheritanceType)) {
        buffer.append("<ul>\n");
        appendSenseChain(buffer, rootWordSense, parent, inheritanceType, attributeType, tab + 1, ancestors);
        buffer.append("</ul>\n");
      }
    } else {
//      System.err.println("ancestors != null || contains sense "+sense+" "+attributeType);
    }
  }

  //FIXME red DERIVATIONALLY_RELATED shows Sense 2 which has no links!?
  private static final EnumSet<RelationType> NON_RECURSIVE_RELATION_TYPES = EnumSet.of(
    DERIVATIONALLY_RELATED,
    MEMBER_OF_TOPIC_DOMAIN, MEMBER_OF_USAGE_DOMAIN, MEMBER_OF_REGION_DOMAIN,
    DOMAIN_OF_TOPIC, DOMAIN_OF_USAGE, DOMAIN_OF_REGION,
    ANTONYM);

  private void displayVerbFrames(final Word word) {
    updateStatusBar(Status.VERB_FRAMES, word.getLowercasedLemma());
    final StringBuilder buffer = new StringBuilder();
    final List<Synset> senses = word.getSynsets();
    buffer.append(senses.size()).append(" sense").append((senses.size() > 1 ? "s" : "")).
      append(" of <span class=\"summaryWord\">").append(word.getLowercasedLemma()).append("</span>\n");
    for (int i = 0, n = senses.size(); i < n; i++) {
      if (! senses.get(i).getWordSense(word).getVerbFrames().isEmpty()) {
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
    private final RelationTarget relationTarget;
    private final Link link;

    Link(final RelationTarget relationTarget, final Link link) {
      this.relationTarget = relationTarget;
      this.link = link;
    }

    boolean contains(final RelationTarget object) {
      for (Link head = this; head != null; head = head.link) {
        if (head.relationTarget.equals(object)) {
          return true;
        }
      }
      return false;
    }
  } // end class Link
}