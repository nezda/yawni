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
import javax.swing.text.*;
import javax.swing.text.html.*;

public class BrowserPanel extends JPanel {
  private static final Logger log = Logger.getLogger(BrowserPanel.class.getName());

  private static final long serialVersionUID = 1L;

  final DictionaryDatabase dictionary;

  private JTextField searchField;
  private JTextPane resultEditorPane;
  private EnumMap<POS, JComboBox> posBoxes;
  private EnumMap<POS, PointerTypeComboBoxModel> posBoxModels;
  private JLabel statusLabel;

  // http://www.jroller.com/jnicho02/entry/using_css_with_htmleditorpane
  private static class StyledTextPane extends JTextPane {
    private static final long serialVersionUID = 1L;
    public StyledTextPane() {
      final HTMLEditorKit kit = new HTMLEditorKit();
      setEditorKit(kit);
      final StyleSheet styleSheet = kit.getStyleSheet();
      styleSheet.addRule("body {font-family:sans-serif;}");
      styleSheet.addRule("li {margin-left:12px; margin-bottom:0px;}");
      styleSheet.addRule("ul {margin-left:12px; margin-bottom:0px;}");
      setDocument(kit.createDefaultDocument());
    }
  } // end class StyledTextPane
  
  public BrowserPanel(final DictionaryDatabase dictionary) {
    this.dictionary = dictionary;
    super.setLayout(new BorderLayout());
    final JPanel searchAndPointersPanel = new JPanel(new BorderLayout());
    final JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    searchAndPointersPanel.add(searchPanel);
    final JLabel searchLabel = new JLabel("Search Word:");
    searchPanel.add(searchLabel);
    this.searchField = new JTextField("", 20);
    searchPanel.add(searchField);
    searchAndPointersPanel.add(searchPanel, BorderLayout.NORTH);

    final JPanel pointerPanel = makePointerPanel();
    searchAndPointersPanel.add(pointerPanel, BorderLayout.SOUTH);
    this.add(searchAndPointersPanel, BorderLayout.NORTH);

    resultEditorPane = new StyledTextPane();
    resultEditorPane.setContentType("text/html");
    resultEditorPane.setEditable(false);
    final JScrollPane jsp = new  JScrollPane(resultEditorPane);
    jsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    jsp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    this.add(jsp, BorderLayout.CENTER);
    this.statusLabel = new JLabel();
    this.statusLabel.setBorder(BorderFactory.createEmptyBorder(0 /*top*/, 3 /*left*/, 3 /*bottom*/, 0 /*right*/));
    this.add(this.statusLabel, BorderLayout.SOUTH);
    updateStatusBar(Status.INTRO);

    searchField.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent event) {
        displayOverview();
      }
    });

    validate();
  }

  @Override
  public void setVisible(final boolean visible) {
    if(visible) {
      searchField.requestFocus();
    }
    super.setVisible(visible);
  }

  static String capitalize(final String str) {
    return Character.toUpperCase(str.charAt(0))+str.substring(1);
  }

  private JPanel makePointerPanel() {
    final JPanel pointerPanel = new JPanel();
    pointerPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
    
    this.posBoxes = new EnumMap<POS, JComboBox>(POS.class);
    this.posBoxModels = new EnumMap<POS, PointerTypeComboBoxModel>(POS.class);

    for (final POS pos : POS.CATS) {
      final PointerTypeComboBoxModel posBoxModel = new PointerTypeComboBoxModel(pos);
      this.posBoxModels.put(pos, posBoxModel);
      posBoxModel.addElement(capitalize(pos.getLabel())); // this will be a marker
      posBoxModel.addElement("Senses"); // this is special

      final JComboBox comboBox = new JComboBox(posBoxModel);
      this.posBoxes.put(pos, comboBox);
      
      final ItemListener listener = new ItemListener() {
        public void itemStateChanged(final ItemEvent event) {
          if (event.getStateChange() == ItemEvent.DESELECTED) {
            return;
          }
          final int selection = comboBox.getSelectedIndex();
          comboBox.setSelectedIndex(0);
          //FIXME have to do morphstr logic here
          final String inputString = searchField.getText().trim();
          IndexWord word = dictionary.lookupIndexWord(pos, inputString);
          if(word == null) {
            final String[] forms = dictionary.lookupBaseForms(pos, inputString);
            assert forms.length > 0;
            word = dictionary.lookupIndexWord(pos, forms[0]);
            assert forms.length > 0;
          }
          if (selection == 0 || selection == 1) {
            displaySenses(word);
          } else {
            final PointerType pointerType = posBoxModel.getPointerTypeForIndex(selection);
            assert pointerType != null;
            displaySenseChain(word, pointerType);
          }
        }
      };
      comboBox.setMaximumRowCount(30);
      comboBox.addItemListener(listener);
      pointerPanel.add(comboBox);
      comboBox.setEnabled(false);
    }

    // Keep comboboxes as narrow as their main label
    for (final JComboBox comboBox : this.posBoxes.values()) {
      //final Dimension d = comboBox.getPreferredSize();
      //comboBox.setPreferredSize(new Dimension(150, d.height));
      comboBox.setPrototypeDisplayValue(comboBox.getItemAt(0));
    }

    return pointerPanel;
  }

  // used by substring search panel
  // FIXME synchronization probably insufficient
  synchronized void setWord(final IndexWord word) {
    searchField.setText(word.getLemma());
    displayOverview();
  }

  private static class PointerTypeComboBoxModel extends DefaultComboBoxModel {
    private static final long serialVersionUID = 1L;

    private final POS pos;
    private final Map<Integer, PointerType> selectionIndexToPointerType;
    PointerTypeComboBoxModel(final POS pos) {
      this.pos = pos;
      this.selectionIndexToPointerType = new TreeMap<Integer, PointerType>();
    }
    
    /** populate with pointer types which apply to pos+word */
    void updateFor(final POS pos, final IndexWord word) { 
      assert this.pos == pos;
      int firstRemoved = -1;
      int lastRemoved = -1;
      for (int i = this.getSize() - 1; i >= 2; i--) {
        if (firstRemoved == -1) {
          this.selectionIndexToPointerType.clear();
          firstRemoved = 2;
          lastRemoved = i;
        }
        this.removeElementAt(i);
      }
      //this.fireIntervalRemoved(this, firstRemoved, lastRemoved);
      int firstAdded = -1;
      int lastAdded = -1;
      for (final PointerType pointerType : word.getPointerTypes()) {
        if (firstAdded == -1) {
          firstAdded = 2;
          lastAdded = 2;
        } else {
          lastAdded++;
        }
        this.selectionIndexToPointerType.put(lastAdded, pointerType);
        // use word+pos custom labels for drop downs
        this.addElement(String.format(pointerType.getFormatLabel(word.getPOS()), word.getLemma()));
      }
      //this.fireIntervalAdded(this, firstAdded, lastAdded);
      this.fireContentsChanged(this, 2, Math.max(lastRemoved, lastAdded));
    }

    PointerType getPointerTypeForIndex(final int selection) {
      return this.selectionIndexToPointerType.get(selection);
    }
  } // end class PointerTypeComboBoxModel

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
      for (final String form : forms) {
        final IndexWord word = dictionary.lookupIndexWord(pos, form);
        //System.err.println("  BrowserPanel form: \""+form+"\" pos: "+pos+" IndexWord found?: "+(word != null));
        enabled |= (word != null);
        appendSenses(word, buffer, false);
        if (word != null) {
          posBoxModels.get(pos).updateFor(pos, word);
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
      buffer.append("<html>The " + word.getPOS().getLabel() + " <b>" + 
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
              buffer.append("<li>");
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

  // add information from pointers (recursive)
  void appendSenseChain(
      final StringBuilder buffer, 
      final Word rootWordSense,
      final PointerTarget sense, 
      final PointerType inheritanceType, 
      final PointerType attributeType, 
      final int tab, 
      Link ancestors) {
    buffer.append("<li>");
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
