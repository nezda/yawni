/*
 * WordNet-Java
 *
 * Copyright 1998 by Oliver Steele.  You can use this software freely so long as you preserve
 * the copyright notice and this restriction, and label your changes.
 */
package edu.brandeis.cs.steele.wn.browser;

import edu.brandeis.cs.steele.wn.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.logging.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.undo.*;


public class BrowserPanel extends JPanel {
  private static final Logger log = Logger.getLogger(BrowserPanel.class.getName());

  private static final long serialVersionUID = 1L;

  final DictionaryDatabase dictionary;

  private final int metaKey;
  private JTextField searchField;
  // when ever this is false, the content of search field has changed
  private boolean searchFieldChanged;
  private final JButton searchButton;
  private final UndoManager undoManager;
  private final UndoAction undoAction;
  private final RedoAction redoAction;
  private final StyledTextPane resultEditorPane;
  private EnumMap<POS, PointerTypeComboBox> posBoxes;
  private final Action slashAction;
  private final JLabel statusLabel;

  public BrowserPanel(final DictionaryDatabase dictionary) {
    this.dictionary = dictionary;
    super.setLayout(new BorderLayout());

    this.metaKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    //TODO assert metaKey is-a power of 2
    //java.awt.event.InputEvent.SHIFT_MASK, CTRL_MASK, META_MASK, ALT_MASK
    this.searchField = new JTextField();
    this.searchField.setBackground(Color.WHITE);

    //this.searchField.getDocument().addDocumentListener(new DocumentListener() {
    //  public void changedUpdate(final DocumentEvent evt) {
    //    assert searchField.getDocument() == evt.getDocument();
    //    System.err.println("changedUpdate: "+evt);
    //    searchFieldChanged = true;
    //  }
    //  public void insertUpdate(final DocumentEvent evt) { 
    //    assert searchField.getDocument() == evt.getDocument();
    //    System.err.println("insertUpdate: "+evt);
    //    System.err.println(getModText(evt));
    //    searchFieldChanged = true;
    //  }
    //  public void removeUpdate(final DocumentEvent evt) { 
    //    assert searchField.getDocument() == evt.getDocument();
    //    System.err.println("removeUpdate: "+evt);
    //    searchFieldChanged = true;
    //  }
    //  String getModText(final DocumentEvent evt) {
    //    try {
    //      final String change = searchField.getDocument().getText(evt.getOffset(), evt.getLength());
    //      return change;
    //    } catch(BadLocationException ble) {
    //      throw new RuntimeException(ble);
    //    }
    //  }
    //});
    
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
        //return "pass".equals(tf.getText());
        System.err.println("verifying input");
        // TODO
        // if the text in this field is different from the
        // text which the menus are currently for, need to
        // re-issue the search
        return true;
      }
    });

    final Action searchAction = new AbstractAction("Search") {
      private static final long serialVersionUID = 1L;
      public void actionPerformed(final ActionEvent event) {
        if(event.getSource() == searchField) {
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
    c.insets = new Insets(3, 3, 0, 3);
    c.fill = GridBagConstraints.HORIZONTAL;
    c.anchor = GridBagConstraints.WEST;
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
    this.resultEditorPane.setBackground(Color.WHITE);
    // http://www.groupsrv.com/computers/about179434.html
    // enables scrolling with arrow keys
    this.resultEditorPane.setEditable(false);
    final JScrollPane jsp = new  JScrollPane(resultEditorPane);
    final JScrollBar jsb = jsp.getVerticalScrollBar();
    
    //TODO move to StyledTextPane (already an action for this?)
    final Action scrollDown = new AbstractAction() {
      private static final long serialVersionUID = 1L;
      public void actionPerformed(final ActionEvent event) {
        final int max = jsb.getMaximum();
        final int inc = resultEditorPane.getScrollableUnitIncrement(jsp.getViewportBorderBounds(), SwingConstants.VERTICAL, +1);
        final int vpos = jsb.getValue();
        final int newPos = Math.min(max, vpos + inc);
        if(newPos != vpos) {
          jsb.setValue(newPos);
        }
      }
    };

    //TODO move to StyledTextPane (already an action for this?)
    final Action scrollUp = new AbstractAction() {
      private static final long serialVersionUID = 1L;
      public void actionPerformed(final ActionEvent event) {
        final int max = jsb.getMaximum();
        final int inc = resultEditorPane.getScrollableUnitIncrement(jsp.getViewportBorderBounds(), SwingConstants.VERTICAL, -1);
        final int vpos = jsb.getValue();
        final int newPos = Math.max(0, vpos - inc);
        if(newPos != vpos) {
          jsb.setValue(newPos);
        }
      }
    };

    final Map<Object, Action> actions = this.resultEditorPane.getActionTable();
    //XXX final Map<Object, Action> actions = new HashMap<Object, Action>();
    //XXX final Integer[] fontSizes = new Integer[]{ 10, 12, 14, 18, 20 };
    //XXX for(final Integer fontSize : fontSizes) {
    //XXX   actions.put(fontSize, new StyledEditorKit.FontSizeAction(String.valueOf(fontSize), fontSize));
    //XXX }

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
    
    //TODO move to StyledTextPane
    final Action bigger = new StyledEditorKit.StyledTextAction("18pts") {
      private static final long serialVersionUID = 1L;
      public void actionPerformed(final ActionEvent evt) {
        System.err.println("bigger");//: "+evt);
        resultEditorPane.selectAll();
        actions.get("font-size-18").actionPerformed(new ActionEvent(resultEditorPane, 0, ""));
        resultEditorPane.setCaretPosition(0); // scroll to top
      }
    };
    //TODO move to StyledTextPane
    final Action smaller = new StyledEditorKit.StyledTextAction("14pts") {
      private static final long serialVersionUID = 1L;
      public void actionPerformed(final ActionEvent evt) {
        System.err.println("smaller");//: "+evt);
        resultEditorPane.selectAll();
        actions.get("font-size-14").actionPerformed(new ActionEvent(resultEditorPane, 0, ""));
        resultEditorPane.setCaretPosition(0); // scroll to top
      }
    };
    this.searchField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, metaKey), bigger);
    this.searchField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, metaKey | InputEvent.SHIFT_MASK), bigger);
    this.searchField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, metaKey), smaller);
    this.searchField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, metaKey | InputEvent.SHIFT_MASK), smaller);

    final String[] extraKeys = new String[] {
      "pressed",
        "shift pressed",
        "meta pressed",
        "shift meta",
        };
    for(final String extraKey : extraKeys) {
      this.searchField.getInputMap().put(KeyStroke.getKeyStroke(extraKey+" UP"), scrollUp); 
      this.resultEditorPane.getInputMap().put(KeyStroke.getKeyStroke(extraKey+" UP"), scrollUp); 
      
      this.searchField.getInputMap().put(KeyStroke.getKeyStroke(extraKey+" DOWN"), scrollDown); 
      this.resultEditorPane.getInputMap().put(KeyStroke.getKeyStroke(extraKey+" DOWN"), scrollDown); 

      for(final PointerTypeComboBox comboBox : this.posBoxes.values()) {
        comboBox.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(extraKey+" UP"), "scrollUp");
        comboBox.getActionMap().put("scrollUp", scrollUp);
        comboBox.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(extraKey+" DOWN"), "scrollDown");
        comboBox.getActionMap().put("scrollDown", scrollDown);

        // yea these don't use extraKey
        comboBox.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, metaKey), "bigger");
        comboBox.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, metaKey | InputEvent.SHIFT_MASK), "bigger");
        comboBox.getActionMap().put("bigger", bigger);
        comboBox.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, metaKey), "smaller");
        comboBox.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, metaKey | InputEvent.SHIFT_MASK), "smaller");
        comboBox.getActionMap().put("smaller", smaller);
      }
    }

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
  }

  // Callback used by Browser so BrowserPanel can add menu items to File menu
  void addMenuItems(final JMenu fileMenu) {
    fileMenu.addSeparator();
    JMenuItem item;
    item = fileMenu.add(undoAction);
    // Command+Z and Ctrl+Z undo on OS X, Windows
    item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, metaKey));
    item = fileMenu.add(redoAction);
    // http://sketchup.google.com/support/bin/answer.py?hl=en&answer=70151
    // TODO Shift+Command+Z on OS X, Ctrl+Y on Windows
    item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, metaKey));
  }

  void wireToFrame(final Browser frame) {
    assert frame.isFocusCycleRoot();
    final java.util.List<Component> components = new ArrayList<Component>();
    components.add(this.searchField);
    components.addAll(this.posBoxes.values());
    //for(final PointerTypeComboBox box : this.posBoxes.values()) {
    //  components.add(box.menu);
    //}
    frame.setFocusTraversalPolicy(new SimpleFocusTraversalPolicy(components));
  }

  @Override public void setVisible(final boolean visible) {
    if(visible) {
      searchField.requestFocusInWindow();
    }
    super.setVisible(visible);
  }

  static String capitalize(final String str) {
    return Character.toUpperCase(str.charAt(0))+str.substring(1);
  }

  class UndoAction extends AbstractAction {
    private static final long serialVersionUID = 1L;
    UndoAction() {
      super("Undo");
      setEnabled(false);
    }

    public void actionPerformed(final ActionEvent evt) {
      try {
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

  class RedoAction extends AbstractAction {
    private static final long serialVersionUID = 1L;
    RedoAction() {
      super("Redo");
      setEnabled(false);
    }

    public void actionPerformed(final ActionEvent evt) {
      try {
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

    @Override protected EditorKit createDefaultEditorKit() {
      final HTMLEditorKit kit = new HTMLEditorKit();
      final StyleSheet styleSheet = kit.getStyleSheet();
      styleSheet.addRule("body {font-family:sans-serif;}");
      //styleSheet.addRule("li {margin-left:12px; margin-bottom:0px;}");
      //FIXME text-indent:-10pt; causes the odd bolding bug
      styleSheet.addRule("ul {list-style-type:none; display:block; text-indent:-10pt;}");
      //styleSheet.addRule("ul ul {list-style-type:circle };");
      styleSheet.addRule("ul {margin-left:12pt; margin-bottom:0pt;}");
      //getDocument().putProperty("multiByte", false);
      return kit;
    }

    // The following two methods allow us to find an
    // action provided by the editor kit by its name.
    Map<Object, Action> getActionTable() {
      final Map<Object, Action> actions = new HashMap<Object, Action>();
      final Action[] actionsArray = getStyledEditorKit().getActions();
      for (int i = 0; i < actionsArray.length; i++) {
        final Action a = actionsArray[i];
        //System.err.println("a: "+a+" name: "+a.getValue(Action.NAME));
        actions.put(a.getValue(Action.NAME), a);
      }
      return actions;
    }
  } // end class StyledTextPane
  
  /** 
   * Class which encapsulates a button (for a pos) which controls a
   * JPopupMenu that is dynamically populated with a PointerTypeActions.
   * Key feature is popup width is as wide as the contents across platforms which is
   * deceptively difficult using JComboBox on most platforms (except OS X).
   */
  private class PointerTypeComboBox extends JButton /* implements ActionListener */ {
    // FIXME if mouse inButton and menu keyboard activated, takes double keyboard action to hide menu
    // FIXME if user changes text field contents and selects menu, bad things will happen
    // FIXME tab doesn't work from popup menu
    // FIXME text in HTML pane looks bold at line wraps
    // + arrows, Ctrl++/Ctrl+-, slash doesn't work from popup menu
    // TODO type-to-navigate popup menu (like JComboBox - code can be copied from there)
    // TODO add down arrow to indicate combo box-like behavior
    private static final long serialVersionUID = 1L;
    private final POS pos;
    final PointerTypeMenu menu;
    private boolean showing;
    private boolean inButton;

    PointerTypeComboBox(final POS pos) {
      //super(capitalize(pos.getLabel())+" \u25BE\u25bc"); // large: \u25BC ▼ small: \u25BE ▾
      super(capitalize(pos.getLabel()));
      this.pos = pos;
      this.menu = new PointerTypeMenu(this);
      this.setComponentPopupMenu(menu);
      this.showing = false;
      this.inButton = false;

      this.addMouseListener(new MouseAdapter() {
        public void mouseEntered(final MouseEvent evt) {
          //System.err.println("mouseEntered");
          inButton = true;
        }
        public void mouseExited(final MouseEvent evt) {
          //System.err.println("mouseExited");
          inButton = false;
        }
        //public void mouseReleased(final MouseEvent evt) {
        //  System.err.println("mouseReleased");
        //}
        public void mousePressed(final MouseEvent evt) {
          if(false == isEnabled()) {
            return;
          }
          assert inButton;
          //System.err.println("mousePressed "+menu.isPopupTrigger(evt));
          assert evt.getComponent() instanceof JButton;
          togglePopup();
          //System.err.println("mousePressed done");
        }
      });

      this.addKeyListener(new KeyAdapter() {
        public void keyTyped(final KeyEvent evt) {
          switch(evt.getKeyChar()) {
            case '\n': 
            case ' ':
              // doClick() just to show the button press
              doClick();
              // the button will only get key strokes if the popup 
              // is NOT showing (though the variable could be
              // out of sync due to app focus loss popup hide)
              showing = false;
              togglePopup();
              break;
            default:
              break;
          }
        }
      });
    }

    /** if popup is showing, hide it, else show it */
    private void togglePopup() {
      if(false == showing) {
        System.err.println("SHOW");
        final Insets margins = getMargin();
        final int px = 5;
        final int py = 1 + this.getHeight() - margins.bottom;        
        menu.show(this, px, py);
        showing = true;
      } else {
        System.err.println("HIDE");
        //menu.setVisible(false);
        showing = false;
      }
      // keep focus to allow keyboard toggle
      //XXX requestFocusInWindow();
    }

    /** populate with pointer types which apply to pos+word */
    void updateFor(final POS pos, final Word word) { 
      menu.removeAll();
      menu.add(new PointerTypeAction("Senses", pos, null));
      for (final PointerType pointerType : word.getPointerTypes()) {
        // use word+pos custom labels for drop downs
        final String label = String.format(pointerType.getFormatLabel(word.getPOS()), word.getLemma());
        //System.err.println("label: "+label+" word: "+word+" pointerType: "+pointerType);
        final JMenuItem item = menu.add(new PointerTypeAction(label, pos, pointerType));
      }
      if(pos == POS.VERB) {
        // use word+pos custom labels for drop downs
        final String label = String.format("Sentence frames for verb %s", word.getLemma());
        //System.err.println("label: "+label+" word: "+word+" pointerType: "+pointerType);
        final JMenuItem item = menu.add(new VerbFramesAction(label));
      }
    }
    
    // ultimately, a minimal subclass of JPopupMenu
    private class PointerTypeMenu extends JPopupMenu implements MenuKeyListener {
      private static final long serialVersionUID = 1L;
      final PointerTypeComboBox comboBox;
      PointerTypeMenu(final PointerTypeComboBox comboBox) {
        this.comboBox = comboBox;
        //XXX setLightWeightPopupEnabled(true);
        addMenuKeyListener(this);
        //XXX System.err.println("getFocusTraversalKeys(): "+getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
        //XXX System.err.println("areFocusTraversalKeysSet(): "+areFocusTraversalKeysSet(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
      }

      public void menuKeyPressed(final MenuKeyEvent evt) {
      }
      public void menuKeyReleased(final MenuKeyEvent evt) {
      }
      public void menuKeyTyped(final MenuKeyEvent evt) {
        System.err.println("menu evt: "+evt+" char: \""+evt.getKeyChar()+"\"");
        switch(evt.getKeyChar()) {
          case '/': 
            // if slash, go-back to searchField
            setVisible(false);
            slashAction.actionPerformed(null); 
            break;
        }
        // if tab, move focus to next thing
      }

      @Override public void setVisible(final boolean show) {
        super.setVisible(show);
        if(show) {
          //System.err.println("V requestFocus(): "+requestFocus(false));
          //System.err.println("requestFocus(): "+requestFocus(true));
          //System.err.println("requestDefaultFocus(): "+requestDefaultFocus());
          //showing = true;
        } else {
          //showing = false;
        }
        //System.err.println("V setVisible: "+show);
        //System.err.println("V isFocusable(): "+isFocusable());
        //System.err.println("V isDisplayable(): "+isDisplayable());
      }

      @Override protected  void firePopupMenuWillBecomeVisible() {
        //System.err.println("firePopupMenuWillBecomeVisible() "+isLightweightComponent(this)+
        //    " isLightWeightPopupEnabled(): "+isLightWeightPopupEnabled());
        //System.err.println("isFocusable(): "+isFocusable());
        //System.err.println("requestFocus(): "+requestFocus(false));
        //System.err.println("requestFocus(): "+requestFocus(true));
        //requestFocus();
        //System.err.println("isDisplayable(): "+isDisplayable());
        //System.err.println("vis focused: "+KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());
      }

      @Override protected  void firePopupMenuWillBecomeInvisible() {
        //System.err.println("firePopupMenuWillBecomeInvisible()");
        //System.err.println("isFocusable(): "+isFocusable());
        //System.err.println("invis focused: "+KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());
        if(false == comboBox.inButton) {
          comboBox.showing = false;
        }
      }

      @Override protected void firePopupMenuCanceled() {
        //System.err.println("firePopupMenuCanceled()");
        //comboBox.showing = false;
      }
    } // end class PointerTypeMenu
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
      Word word = BrowserPanel.this.dictionary.lookupWord(pos, inputString);
      if(word == null) {
        final String[] forms = dictionary.lookupBaseForms(pos, inputString);
        assert forms.length > 0 : "searchField contents must have changed";
        word = BrowserPanel.this.dictionary.lookupWord(pos, forms[0]);
        assert forms.length > 0;
      }
      if (pointerType == null) {
        //FIXME bad form to use stderr
        System.err.println("word: "+word);
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
      Word word = BrowserPanel.this.dictionary.lookupWord(POS.VERB, inputString);
      if(word == null) {
        final String[] forms = dictionary.lookupBaseForms(POS.VERB, inputString);
        assert forms.length > 0 : "searchField contents must have changed";
        word = BrowserPanel.this.dictionary.lookupWord(POS.VERB, forms[0]);
        assert forms.length > 0;
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

  /**
   * Generic search and output generation code
   **/

  private synchronized void displayOverview() {
    // TODO normalize internal space
    final String inputString = searchField.getText().trim(); 
    if (inputString.length() == 0) {
      updateStatusBar(Status.INTRO);
      resultEditorPane.setText("");
      return;
    }
    final StringBuilder buffer = new StringBuilder();
    boolean definitionExists = false;
    for (final POS pos : POS.CATS) {
      String[] forms = dictionary.lookupBaseForms(pos, inputString);
      if (forms == null) {
        forms = new String[]{ inputString };
      } else {
        boolean found = false;
        for (final String form : forms) {
          if (form.equals(inputString)) {
            found = true;
            break;
          }
        }
        if (forms != null && forms.length > 0 && found == false) {
          System.err.println("    BrowserPanel inputString: \"" + inputString +
              "\" not found in forms: "+Arrays.toString(forms));
        }
      }
      boolean enabled = false;
      //System.err.println("  BrowserPanel forms: \""+Arrays.asList(forms)+"\" pos: "+pos);
      final SortedSet<String> noCaseForms = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
      for (final String form : forms) {
        if(noCaseForms.contains(form)) {
          // block no case dups ("hell"/"Hell", "villa"/"Villa")
          continue;
        }
        noCaseForms.add(form);
        final Word word = dictionary.lookupWord(pos, form);
        //System.err.println("  BrowserPanel form: \""+form+"\" pos: "+pos+" Word found?: "+(word != null));
        enabled |= (word != null);
        appendSenses(word, buffer, false);
        //FIXME adds extra HR at the end
        buffer.append("<hr>");
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
  private static enum Status {
    INTRO("Enter search word and press return"),
    OVERVIEW("Overview of %s"),
    SEARCHING("Searching..."),
    SYNONYMS("Synonyms search for %s \"%s\""),
    NO_MATCHES("No matches found."),
    POINTER("\"%s\" search for %s \"%s\""),
    VERB_FRAMES("Verb Frames search for verb \"%s\"")
    ;

    private final String formatString;
    
    private Status(final String formatString) {
      this.formatString = formatString;
    }
    
    String get(Object... args) {
      if (this == POINTER) {
        final PointerType pointerType = (PointerType)args[0];
        final POS pos = (POS) args[1];
        final String lemma = (String)args[2];
        return
          String.format(formatString,
            String.format(pointerType.getFormatLabel(pos), lemma),
            pos.getLabel(),
            lemma);
      } else {
        return String.format(formatString, args);
      }
    }
  } // end enum Status
  
  // TODO For PointerType searches, show Same text as combo box (e.g. 
  private void updateStatusBar(final Status status, final Object... args) {
    this.statusLabel.setText(status.get(args));
  }

  // overview for single pos+word
  private synchronized void displaySenses(final Word word) {
    updateStatusBar(Status.SYNONYMS, word.getPOS().getLabel(), word.getLemma());
    final StringBuilder buffer = new StringBuilder();
    appendSenses(word, buffer, true);
    resultEditorPane.setText(buffer.toString());
    resultEditorPane.setCaretPosition(0); // scroll to top
  }

  /** 
   * Core search routine which renders its results as HTML. 
   * TODO 
   * Factor out this logic into a result data structure like findtheinfo_ds() does
   * to separate logic from presentation.
   * Nice XML format would open up some nice possibilities for web services, commandline,
   * and this traditional GUI application.
   */
  private void appendSenses(final Word word, final StringBuilder buffer, final boolean verbose) {
    if (word != null) {
      final Synset[] senses = word.getSynsets();
      final int taggedCount = word.getTaggedSenseCount();
      buffer.append("The " + word.getPOS().getLabel() + " <b>" + 
        word.getLemma() + "</b> has " + senses.length + " sense" + (senses.length == 1 ? "" : "s") + " ");
      buffer.append("(");
      if (taggedCount == 0) {
        buffer.append("no senses from tagged texts");
      } else {
        buffer.append("first " + taggedCount + " from tagged texts");
      }
      buffer.append(")<br>\n");
      buffer.append("<ol>\n");
      for (final Synset sense : senses) {
        buffer.append("<li>");
        final int cnt = sense.getWordSense(word).getSensesTaggedFrequency();
        if (cnt != 0) {
          buffer.append("(");
          buffer.append(cnt);
          buffer.append(") ");
        }
        if(word.getPOS() != POS.ADJ) {
          buffer.append("&lt;");
          // strip POS off of lex cat (up to first period)
          String posFreeLexCat = sense.getLexCategory();
          final int periodIdx = posFreeLexCat.indexOf(".");
          assert periodIdx > 0;
          posFreeLexCat = posFreeLexCat.substring(periodIdx + 1);
          buffer.append(posFreeLexCat);
          buffer.append("&gt; ");
        }
        //XXX how do you get to/from the satellite
        buffer.append(sense.getLongDescription(verbose));
        if (verbose) {
          final PointerTarget[] similar = sense.getTargets(PointerType.SIMILAR_TO);
          if (similar.length > 0) {
            if (verbose) {
              buffer.append("<br>\n");
              buffer.append("Similar to:");
            }
            buffer.append("<ul>\n");
            for (final PointerTarget target : similar) {
              buffer.append(listOpen());
              final Synset targetSynset = (Synset)target;
              buffer.append(targetSynset.getLongDescription(verbose));
              buffer.append("</li>\n");
            }
            buffer.append("</ul>\n");
          }

          final PointerTarget[] targets = sense.getTargets(PointerType.SEE_ALSO);
          if (targets.length > 0) {
            if (similar.length == 0) {
              buffer.append("<br>");
            }
            buffer.append("Also see: ");
            int seeAlsoNum = 0;
            for (final PointerTarget seeAlso : targets) {
              buffer.append(seeAlso.getDescription());
              for (final WordSense wordSense : seeAlso) {
                buffer.append("#");
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
  }

  // render single Word + PointerType
  private void displaySenseChain(final Word word, final PointerType pointerType) {
    updateStatusBar(Status.POINTER, pointerType, word.getPOS(), word.getLemma());
    final StringBuilder buffer = new StringBuilder();
    final Synset[] senses = word.getSynsets();
    // count number of senses pointerType applies to
    int numApplicableSenses = 0;
    for (int i = 0; i < senses.length; ++i) {
      if (senses[i].getTargets(pointerType).length > 0) {
        numApplicableSenses++;
      }
    }
    buffer.append("Applies to " + numApplicableSenses + " of the " + senses.length + " senses" +
        //(senses.length > 1 ? "s" : "")+
        " of <b>" + word.getLemma() + "</b>\n");
    for (int i = 0; i < senses.length; ++i) {
      if (senses[i].getTargets(pointerType).length > 0) {
        buffer.append("<br><br>Sense " + (i + 1) + "\n");

        PointerType inheritanceType = PointerType.HYPERNYM;
        PointerType attributeType = pointerType;

        if (pointerType.equals(inheritanceType) || pointerType.symmetricTo(inheritanceType)) {
          inheritanceType = pointerType;
          attributeType = null;
        }
        System.err.println("word: "+word+" inheritanceType: "+inheritanceType+" attributeType: "+attributeType);
        buffer.append("<ul>\n");
        appendSenseChain(buffer, senses[i].getWordSense(word), senses[i], inheritanceType, attributeType);
        buffer.append("</ul>\n");
      }
    }
    resultEditorPane.setText(buffer.toString());
    resultEditorPane.setCaretPosition(0); // scroll to top
  }

  private void displayVerbFrames(final Word word) {
    updateStatusBar(Status.VERB_FRAMES, word.getLemma());
    final StringBuilder buffer = new StringBuilder();
    final Synset[] senses = word.getSynsets();
    buffer.append(senses.length + " sense" +
        (senses.length > 1 ? "s" : "")+
        " of <b>" + word.getLemma() + "</b>\n");
    for (int i = 0; i < senses.length; ++i) {
      if (senses[i].getWordSense(word).getVerbFrames().isEmpty() == false) {
        buffer.append("<br><br>Sense " + (i + 1) + "\n");
        //TODO show the synset ?
        buffer.append("<ul>\n");
        for(final String frame : senses[i].getWordSense(word).getVerbFrames()) {
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

  //FIXME this seems pretty damn old-fashioned.  List ? LinkedList ?
  private static class Link {
    private final Object object;
    private final Link link;

    Link(final Object object, final Link link) {
      this.object = object;
      this.link = link;
    }

    boolean contains(Object object) {
      for (Link head = this; head != null; head = head.link) {
        if (head.object.equals(object)) {
          return true;
        }
      }
      return false;
    }
  } // end class Link

  // add information from pointers
  void appendSenseChain(
    final StringBuilder buffer, 
    final WordSense rootWordSense,
    final PointerTarget sense, 
    final PointerType inheritanceType, 
    final PointerType attributeType) {
    appendSenseChain(buffer, rootWordSense, sense, inheritanceType, attributeType, 0, null);
  }

  private String listOpen() {
    //return "<li>";
    //return "<li>• ";
    //return "<li>\u2022 ";
    return "<li>* ";
  }

  // add information from pointers (recursive)
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
          //System.err.println("rootWordSense: "+rootWordSense+
          //    " inheritanceType: "+inheritanceType+" attributeType: "+attributeType);
          //System.err.println("pointer: "+pointer);
          continue;
        }
        buffer.append("<li>");
        if(target instanceof WordSense) {
          assert pointer.isLexical();
          final WordSense wordSense = (WordSense)target;
          //FIXME RELATED TO label below only right for DERIVATIONALLY_RELATED
          buffer.append("RELATED TO → ("+wordSense.getPOS().getLabel()+") "+wordSense.getLemma()+"#"+wordSense.getSenseNumber());
          //ANTONYM example:
          //Antonym of dissociate (Sense 2)
          buffer.append("<br>\n");
        } else {
          buffer.append("POINTER TARGET ");
        }
        buffer.append(target.getSynset().getLongDescription());
        buffer.append("</li>\n");
      }
    }
    if (attributeType != null) {
      String key = attributeType.getKey();
      assert key != null : "attributeType: "+attributeType;
      if (key == null) {
        key = "";
      }
      // Don't get ancestors for derived or category relationships.
      // FIXME or Antonym ?
      if (key.startsWith("+") || key.startsWith("-") || key.startsWith(";")) {
        return;
      }
    }
    if (ancestors == null || ancestors.contains(sense) == false) {
      ancestors = new Link(sense, ancestors);
      for (final PointerTarget parent : sense.getTargets(inheritanceType)) {
        buffer.append("<ul>\n");
        appendSenseChain(buffer, rootWordSense, parent, inheritanceType, attributeType, tab + 4, ancestors);
        buffer.append("</ul>\n");
      }
    }
  }
}
