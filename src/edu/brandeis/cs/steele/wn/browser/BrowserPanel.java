/*
 * WordNet-Java
 *
 * Copyright 1998 by Oliver Steele.  You can use this software freely so long as you preserve
 * the copyright notice and this restriction, and label your changes.
 */
package edu.brandeis.cs.steele.wn.browser;

import edu.brandeis.cs.steele.wn.*;

import java.awt.event.*;
import java.awt.*;
import java.beans.*;
import java.util.*;
import java.util.logging.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.text.*;
import javax.swing.text.html.*;


public class BrowserPanel extends JPanel {
  private static final Logger log = Logger.getLogger(BrowserPanel.class.getName());

  private static final long serialVersionUID = 1L;

  final DictionaryDatabase dictionary;

  private JTextField searchField;
  private JButton searchButton;
  private JTextComponent resultEditorPane;
  private EnumMap<POS, PointerTypeComboBox> posBoxes;
  private Action slashAction;
  private JLabel statusLabel;

  public BrowserPanel(final DictionaryDatabase dictionary) {
    this.dictionary = dictionary;
    super.setLayout(new BorderLayout());

    this.searchField = new JTextField();

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
    
    //final JLabel searchLabel = new JLabel("Search Word:", SwingUtilities.LEFT);
    makePOSComboBoxes();
    
    final JPanel searchAndPointersPanel = new JPanel(new GridBagLayout());
    final GridBagConstraints c = new GridBagConstraints();

    c.gridy = 0;
    c.gridx = 0;
    c.insets = new Insets(3, 0, 0, 0);
    c.fill = GridBagConstraints.HORIZONTAL;
    c.anchor = GridBagConstraints.WEST;
    //c.gridwidth = GridBagConstraints.RELATIVE;
    //c.gridx = GridBagConstraints.RELATIVE;
    //c.gridx = GridBagConstraints.REMAINDER;
    c.gridwidth = 4;
    //searchAndPointersPanel.add(searchField, c);
    //JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    final Box searchPanel = new Box(BoxLayout.X_AXIS);

    searchPanel.add(searchField);
    searchPanel.add(Box.createHorizontalStrut(3));
    searchPanel.add(searchButton);
    searchAndPointersPanel.add(searchPanel, c);
    //XXX searchAndPointersPanel.add(searchField, c);
    //XXX c.gridx = GridBagConstraints.REMAINDER;
    //XXX searchAndPointersPanel.add(searchButton, c);


    //c.gridx = 1;
    //c.gridx = GridBagConstraints.RELATIVE;
    //c.gridx = GridBagConstraints.REMAINDER;
    c.fill = GridBagConstraints.NONE;
    c.gridwidth = 1;
    //searchAndPointersPanel.add(searchButton, c);

    c.gridy = 1;
    c.gridx = 0;
    c.insets = new Insets(3, 0, 3, 3);
    searchAndPointersPanel.add(this.posBoxes.get(POS.NOUN), c);
    c.gridx = 1;
    searchAndPointersPanel.add(this.posBoxes.get(POS.VERB), c);
    c.gridx = 2;
    searchAndPointersPanel.add(this.posBoxes.get(POS.ADJ), c);
    c.gridx = 3;
    c.insets = new Insets(3, 0, 3, 0);
    searchAndPointersPanel.add(this.posBoxes.get(POS.ADV), c);

    // set width(pointerPanel) = width(searchPanel)

    //final Box left = new Box(BoxLayout.X_AXIS);
    //left.add(searchAndPointersPanel);
    //this.add(Box.createHorizontalGlue());
    //this.add(left, BorderLayout.NORTH);
    this.add(searchAndPointersPanel, BorderLayout.NORTH);
    
    resultEditorPane = new StyledTextPane();
    //resultEditorPane.setContentType("text/html");
    // http://www.groupsrv.com/computers/about179434.html
    // enables scrolling with arrow keys
    resultEditorPane.setEditable(false);
    final JScrollPane jsp = new  JScrollPane(resultEditorPane);
    final JScrollBar jsb = jsp.getVerticalScrollBar();
    
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

    final String[] extraKeys = new String[] {
      "pressed",
        "shift pressed",
        "meta pressed",
        "shift meta",
        };
    for(final String extraKey : extraKeys) {
      this.searchField.getInputMap().put(KeyStroke.getKeyStroke(extraKey+" UP"), scrollUp); 
      this.resultEditorPane.getInputMap().put(KeyStroke.getKeyStroke(extraKey+" UP"), scrollUp); 
    }
    for(final String extraKey : extraKeys) {
      this.searchField.getInputMap().put(KeyStroke.getKeyStroke(extraKey+" DOWN"), scrollDown); 
      this.resultEditorPane.getInputMap().put(KeyStroke.getKeyStroke(extraKey+" DOWN"), scrollDown); 
    }

    jsp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, 0, false), "Slash");
    jsp.getActionMap().put("Slash", slashAction);
    jsp.getVerticalScrollBar().setFocusable(false); 
    jsp.getHorizontalScrollBar().setFocusable(false); 
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

  void wireToFrame(final JFrame frame) {
    assert frame.isFocusCycleRoot();
    final java.util.List<Component> components = new ArrayList<Component>();
    components.add(this.searchField);
    components.addAll(this.posBoxes.values());
    frame.setFocusTraversalPolicy(new SimpleFocusTraversalPolicy(components));
    // a little too aggressive - handles ALL enter key presses
    //getRootPane().setDefaultButton(searchButton);
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

  /** 
   * Simple FocusTraversalPolicy which cycles through provided components in sequential order
   * and defauls to the first component
   */
  private static class SimpleFocusTraversalPolicy extends FocusTraversalPolicy {
    private final Vector<Component> order;

    public SimpleFocusTraversalPolicy(final java.util.List<Component> order) {
      this.order = new Vector<Component>(order.size());
      this.order.addAll(order);
    }

    public Component getComponentAfter(final Container focusCycleRoot, final Component aComponent) {
      return getNextComponent(focusCycleRoot, aComponent, true);
    }

    public Component getComponentBefore(final Container focusCycleRoot, final Component aComponent) {
      return getNextComponent(focusCycleRoot, aComponent, false);
    }

    public Component getDefaultComponent(Container focusCycleRoot) {
      return order.get(0);
    }

    public Component getLastComponent(Container focusCycleRoot) {
      return order.lastElement();
    }

    public Component getFirstComponent(Container focusCycleRoot) {
      return order.get(0);
    }

    private Component getNextComponent(final Container focusCycleRoot, final Component aComponent, final boolean after) {
      final int idx = order.indexOf(aComponent);
      if(idx < 0) {
        return getDefaultComponent(focusCycleRoot);
      }
      for(int 
        n = order.size(), i = next(idx, n, after), cnt = 0;
        cnt < n;  
        i = next(i, n, after), cnt++) {
        final Component comp = order.get(i);
        if(comp.isEnabled() && comp.isFocusable()) {
          return order.get(i);
        }
      }
      return getDefaultComponent(focusCycleRoot);
    }

    private int next(final int i, final int n, final boolean after) {
      if(after) {
        // 0 1 2 0 1 2
        return (i + 1) % n;
      } else {
        assert i >= 0;
        // 0 2 1 0 2 1
        if(i - 1 == 0) {
          return n - 1;
        } else {
          return i - 1;
        }
      }
    }
  } // end class SimpleFocusTraversalPolicy

  /** 
   * Nice looking SansSerif HTML rendering JTextPane.
   * http://www.jroller.com/jnicho02/entry/using_css_with_htmleditorpane
   */
  private static class StyledTextPane extends JTextPane {
    private static final long serialVersionUID = 1L;
    public StyledTextPane() {
      final HTMLEditorKit kit = new HTMLEditorKit();
      setEditorKit(kit);
      final StyleSheet styleSheet = kit.getStyleSheet();
      styleSheet.addRule("body {font-family:sans-serif;}");
      //styleSheet.addRule("li {margin-left:12px; margin-bottom:0px;}");
      styleSheet.addRule("ul {list-style-type:none; display:block; text-indent:-10pt;}");
      //styleSheet.addRule("ul ul {list-style-type:circle };");
      styleSheet.addRule("ul {margin-left:12px; margin-bottom:0px;}");
      setDocument(kit.createDefaultDocument());
      //XXX getDocument().putProperty("multiByte", false);
    }
  } // end class StyledTextPane
  
  /** 
   * Class which encapsulates a button (for a pos) which controls a
   * JPopupMenu that is dynamically populated with a PointerTypeComboBoxModel.
   * Key feature is popup width is as wide as the contents across platforms which is
   * deceptively difficult using JComboBox on most platforms (except OS X).
   */
  private class PointerTypeComboBox extends JButton /* implements ActionListener */ {
    // TODO Keyboard focus change doesn't hide menu
    // FIXME if mouse inButton and menu keyboard activated, takes double click!
    // TODO add down arrow to indicate combo box-like behavior
    private static final long serialVersionUID = 1L;
    private final POS pos;
    private final PointerTypeMenu menu;
    private boolean showing;
    private boolean inButton;

    PointerTypeComboBox(final POS pos) {
      super(capitalize(pos.getLabel()));
      this.pos = pos;
      this.menu = new PointerTypeMenu(this);
      this.showing = false;
      this.inButton = false;
      //final Insets margins = getMargin();
      //System.err.println("  "+margins);
      //setMargin(null);
      //System.err.println("  alt: "+getMargin());

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
          //System.err.println("evt: "+evt+" char: "+((int)evt.getKeyChar()));
          //System.err.println("evt.isActionKey(): "+evt.isActionKey());
          switch(evt.getKeyChar()) {
            case '\n': 
            case ' ':
              doClick();
              togglePopup();
              break;
            default:
              break;
          }
        }
      });
    }

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
        menu.setVisible(false);
        showing = false;
      }
      // keep focus to allow keyboard toggle
      //XXX requestFocusInWindow();
    }

    /** populate with pointer types which apply to pos+word */
    void updateFor(final POS pos, final IndexWord word) { 
      menu.removeAll();
      menu.add(new PointerTypeAction("Senses", pos, null));
      for (final PointerType pointerType : word.getPointerTypes()) {
        // use word+pos custom labels for drop downs
        final String label = String.format(pointerType.getFormatLabel(word.getPOS()), word.getLemma());
        menu.add(new PointerTypeAction(label, pos, pointerType));
      }
    }
    
    private class PointerTypeMenu extends JPopupMenu implements ChangeListener {
      private static final long serialVersionUID = 1L;
      final PointerTypeComboBox comboBox;
      PointerTypeMenu(final PointerTypeComboBox comboBox) {
        // DefaultComboBoxModel
        this.comboBox = comboBox;
      }

      public void stateChanged(final ChangeEvent evt) {
        //System.err.println("stateChanged: "+evt);
        //JMenuItem item = (JMenuItem)evt.getSource();
        //System.err.println("isArmed: "+item.isArmed()+" isSelected: "+item.isSelected()+" labelText: "+item.getText());
      }

      @Override protected  void firePopupMenuWillBecomeVisible() {
        //System.err.println("firePopupMenuWillBecomeVisible()");
        // if any differences between model and menu, clear menu and rebuild from model
        //XXX boolean rebuild = true;
        //XXX for(int i = 0, n = comboBox.posBoxModel.getSize(); i < n; i++) {
        //XXX   final Component comp = getComponent(i);
        //XXX   if(comp == null) {
        //XXX     break;
        //XXX   }
        //XXX   System.err.println("comp: "+comp);
        //XXX }
        //removeAll();
        //for(int i = 0, n = comboBox.posBoxModel.size(); i < n; i++) {
        //  final JMenuItem item = comboBox.posBoxModel.getElementAt(i);
        //  //item.setArmed(true);
        //  this.add(item);
        //  //item.addChangeListener(this);
        //}
        //pack();
      }

      @Override protected  void firePopupMenuWillBecomeInvisible() {
        //System.err.println("firePopupMenuWillBecomeInvisible()");
        if(false == comboBox.inButton) {
          comboBox.showing = false;
        }
      }

      //@Override protected void firePopupMenuCanceled() {
      //  System.err.println("firePopupMenuCanceled()");
      //  //comboBox.show = false;
      //}
    }
  } // end class PointerTypeMenu

  /** 
   * Model for PointerType+POS.
   * FIXME change into List<PointerTypeJMenuItem> to prevent reinstantiating
   * them over and over again - just update the labels (text) and
   * enabled/disabled property - each should also remember its seletected/deselected state
   */
  private class PointerTypeComboBoxModel {
    private static final long serialVersionUID = 1L;

    private final POS pos;
    private final Vector<JMenuItem> items;
    PointerTypeComboBoxModel(final POS pos) {
      this.pos = pos;
      this.items = new Vector<JMenuItem>();
    }
    
    /** populate with pointer types which apply to pos+word */
    void updateFor(final POS pos, final IndexWord word) { 
      assert this.pos == pos;
      items.clear();
      items.add(new JMenuItem(new PointerTypeAction("Senses", pos, null)));
      for (final PointerType pointerType : word.getPointerTypes()) {
        // use word+pos custom labels for drop downs
        final String label = String.format(pointerType.getFormatLabel(word.getPOS()), word.getLemma());
        items.add(new JMenuItem(new PointerTypeAction(label, pos, pointerType)));
      }
    }
    int size() {
      return items.size();
    }
    JMenuItem getElementAt(int i) {
      return items.get(i);
    }
  } // end class PointerTypeComboBoxModel

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
      final String inputString = searchField.getText().trim();
      IndexWord word = dictionary.lookupIndexWord(pos, inputString);
      if(word == null) {
        final String[] forms = dictionary.lookupBaseForms(pos, inputString);
        assert forms.length > 0;
        word = dictionary.lookupIndexWord(pos, forms[0]);
        assert forms.length > 0;
      }
      if (pointerType == null) {
        System.err.println("word: "+word);
        displaySenses(word);
      } else {
        displaySenseChain(word, pointerType);
      }
    }
  } // end class PointerTypeAction

  private void makePOSComboBoxes() {
    this.posBoxes = new EnumMap<POS, PointerTypeComboBox>(POS.class);

    for (final POS pos : POS.CATS) {
      final PointerTypeComboBox comboBox = new PointerTypeComboBox(pos);
      final Insets margins = comboBox.getMargin();
      final Border border = comboBox.getBorder();
      final Insets borderInsets = border.getBorderInsets(comboBox);
      //System.err.println("  "+margins);
      //System.err.println("  "+border);
      //System.err.println("  "+borderInsets);
      
      comboBox.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, 0, false), "Slash");
      comboBox.getActionMap().put("Slash", slashAction);
      
      this.posBoxes.put(pos, comboBox);
      comboBox.setEnabled(false);
    }
  }

  // used by substring search panel
  // FIXME synchronization probably insufficient
  synchronized void setWord(final IndexWord word) {
    searchField.setText(word.getLemma());
    displayOverview();
  }

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
        final IndexWord word = dictionary.lookupIndexWord(pos, form);
        //System.err.println("  BrowserPanel form: \""+form+"\" pos: "+pos+" IndexWord found?: "+(word != null));
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
  
  private enum Status {
    INTRO("Enter search word and press return"),
    OVERVIEW("Overview of %s"),
    SEARCHING("Searching..."),
    SYNONYMS("Synonyms search for %s \"%s\""),
    NO_MATCHES("No matches found."),
    POINTER("\"%s\" search for %s \"%s\""),
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
  private synchronized void displaySenses(final IndexWord word) {
    updateStatusBar(Status.SYNONYMS, word.getPOS().getLabel(), word.getLemma());
    final StringBuilder buffer = new StringBuilder();
    appendSenses(word, buffer, true);
    resultEditorPane.setText(buffer.toString());
    resultEditorPane.setCaretPosition(0); // scroll to top
  }

  private void appendSenses(final IndexWord word, final StringBuilder buffer, final boolean verbose) {
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
        final int cnt = sense.getWord(word).getSensesTaggedFrequency();
        if (cnt != 0) {
          buffer.append("(");
          buffer.append(cnt);
          buffer.append(") ");
        }
        buffer.append("&lt;");
        buffer.append(sense.getLexCategory());
        buffer.append("&gt; ");
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
              for (final Word wordSense : seeAlso) {
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

  // render single IndexWord + PointerType
  private void displaySenseChain(final IndexWord word, final PointerType pointerType) {
    updateStatusBar(Status.POINTER, pointerType, word.getPOS(), word.getLemma());
    final StringBuilder buffer = new StringBuilder();
    final Synset[] senses = word.getSynsets();
    //TODO if pointerType doesn't apply to all senses, change to say
    //<number it applies to> of <senses.length> senses of <word>
    buffer.append(senses.length + " sense" +
        (senses.length > 1 ? "s" : "")+
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
        appendSenseChain(buffer, senses[i].getWord(word), senses[i], inheritanceType, attributeType);
        buffer.append("</ul>\n");
      }
    }
    resultEditorPane.setText(buffer.toString());
    resultEditorPane.setCaretPosition(0); // scroll to top
  }

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
    final Word rootWordSense,
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
      final Word rootWordSense,
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
        //  System.err.println("rootWordSense: "+rootWordSense+
        //      " inheritanceType: "+inheritanceType+" attributeType: "+attributeType);
        //  System.err.println("pointer: "+pointer);
          continue;
        }
        buffer.append("<li>");
        if(target instanceof Word) {
          assert pointer.isLexical();
          final Word wordSense = (Word)target;
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
