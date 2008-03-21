/*
 * WordNet-Java
 *
 * Copyright 1998 by Oliver Steele.  You can use this software freely so long as you preserve
 * the copyright notice and this restriction, and label your changes.
 */
package edu.brandeis.cs.steele.wn.browser;

import edu.brandeis.cs.steele.wn.*;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.*;
import java.util.*;
import java.util.logging.*;
import javax.swing.*;


public class BrowserPanel extends JPanel {
  private static final Logger log = Logger.getLogger(BrowserPanel.class.getName());

  private static final long serialVersionUID = 1L;

  final DictionaryDatabase dictionary;

  private JTextField searchField;
  private JEditorPane resultEditorPane;
  private JComboBox[] posBoxes = new JComboBox[POS.CATS.length];

  public BrowserPanel(final DictionaryDatabase dictionary) {
    this.dictionary = dictionary;

    setLayout(new GridBagLayout());
    final GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = GridBagConstraints.RELATIVE;
    constraints.fill = GridBagConstraints.BOTH;
    final JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    //searchPanel.setBackground(Color.LIGHT_GRAY);
    final JLabel searchLabel = new JLabel("Search Word:");
    searchPanel.add(searchLabel);
    searchField = new JTextField("", 20);
    //searchField.setBackground(Color.WHITE);
    searchPanel.add(searchField);
    add(searchPanel, constraints);

    final JPanel pointerPanel = makePointerPanel();
    add(pointerPanel, constraints);

    constraints.weightx = constraints.weighty = 1.0;
    resultEditorPane = new JEditorPane();
    resultEditorPane.setContentType("text/html");
    //resultEditorPane.setBackground(Color.WHITE);
    resultEditorPane.setEditable(false);
    add(new JScrollPane(resultEditorPane), constraints);

    searchField.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        displayOverview();
      }
    });

    validate();
  }

  @Override
  public void setVisible(boolean visible) {
    searchField.requestFocus();
    super.setVisible(visible);
  }

  private JPanel makePointerPanel() {
    final JPanel pointerPanel = new JPanel();
    pointerPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
    //pointerPanel.setBackground(Color.LIGHT_GRAY);

    final ButtonGroup group = new ButtonGroup();
    for (int i = 0; i < POS.CATS.length; ++i) {
      final POS pos = POS.CATS[i];
      final Vector pointerTypes = new Vector();
      pointerTypes.add(pos.getLabel()); // this will be a marker
      pointerTypes.add("Senses"); // this is special

      for (final PointerType pointerType : PointerType.values()) {
        if (pointerType.appliesTo(pos)) {
          if (log.isLoggable(Level.FINEST)) {
            log.finest("Pointer Type:"+pointerType.getLabel());
          }
          pointerTypes.add(pointerType);
        }
      }

      final JComboBox comboBox = posBoxes[i] = new JComboBox(pointerTypes);

      final ItemListener listener = new ItemListener() {
        public void itemStateChanged(ItemEvent event) {
          if (event.getStateChange() == ItemEvent.DESELECTED) {
            return;
          }
          //FIXME have to do morphstr logic here
          final IndexWord word = dictionary.lookupIndexWord(pos, searchField.getText());
          final int selection = comboBox.getSelectedIndex();
          if (selection == 0 || selection == 1) {
            displaySenses(word);
          } else {
            displaySenseChain(word, (PointerType) pointerTypes.get(selection));
          }
        }
      };

      comboBox.addItemListener(listener);
      final JPanel panel = new JPanel(new GridBagLayout());
      //GridBagConstraints constraints = new GridBagConstraints();
      //constraints.gridy = GridBagConstraints.RELATIVE;
      //constraints.gridx = 0;
      //panel.add(comboBox, constraints);
      panel.add(comboBox);
      pointerPanel.add(panel);
      comboBox.setEnabled(false);
    }

    return pointerPanel;
  }

  synchronized void setWord(final IndexWord word) {
    searchField.setText(word.getLemma());
    displayOverview();
  }

  private synchronized void displayOverview() {
    final String inputString = searchField.getText().trim(); // FIXME trim edge whitespace and normalize internal space
    final StringBuilder buffer = new StringBuilder();
    boolean definitionExists = false;
    for (int i = 0; i < POS.CATS.length; ++i) {
      final POS pos = POS.CATS[i];
      //IndexWord word = dictionary.lookupIndexWord(pos, inputString);
      //if (word == null) {
      //  //System.err.println(searchField.getText()+" pos "+pos);
      //  final String baseForm = dictionary.lookupBaseForm(pos, searchField.getText());
      //  if(baseForm != null) {
      //    System.err.println("  "+searchField.getText()+" baseForm: "+baseForm);
      //    word = dictionary.lookupIndexWord(pos, baseForm);
      //  }
      //}
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
          //throw new RuntimeException("inputString: \"" + inputString +
          //    "\" not found in forms: "+Arrays.toString(forms));
          System.err.println("    BrowserPanel inputString: \"" + inputString +
              "\" not found in forms: "+Arrays.toString(forms));
        }
      }
      boolean enabled = false;
      for (final String form : forms) {
        final IndexWord word = dictionary.lookupIndexWord(pos, form);
        System.err.println("  BrowserPanel form: \""+form+"\" pos: "+pos+" IndexWord found?: "+(word != null));
        enabled |= (word != null);
        appendSenses(word, buffer);
      }
      posBoxes[i].setEnabled(enabled);
      definitionExists |= enabled;
    }
    if (definitionExists == false) {
      buffer.append("\"");
      buffer.append(inputString);
      buffer.append("\" is not defined.");
    }
    resultEditorPane.setText(buffer.toString());
    resultEditorPane.setCaretPosition(0); // scroll to top
  }

  private synchronized void displaySenses(final IndexWord word) {
    final StringBuilder buffer = new StringBuilder();
    appendSenses(word, buffer);
    resultEditorPane.setText(buffer.toString());
  }

  private void appendSenses(final IndexWord word, final StringBuilder buffer) {
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
      buffer.append(")<br><br>");
      buffer.append("<ol>");
      for (final Synset sense : senses) {
        buffer.append("<li>");
        buffer.append(sense.getLongDescription());
        buffer.append("</li>");
      }
      buffer.append("</ol>");
    }
  }

  private synchronized void displaySenseChain(IndexWord word, PointerType pointerType) {
    final StringBuilder buffer = new StringBuilder();
    final Synset[] senses = word.getSynsets();
    buffer.append(senses.length + " senses of <b>" + word.getLemma() + "</b><br><br>");
    for (int i = 0; i < senses.length; ++i) {
      if (senses[i].getTargets(pointerType).length > 0) {
        buffer.append("Sense " + (i + 1) + "<br>");

        PointerType inheritanceType = PointerType.HYPERNYM;
        PointerType attributeType = pointerType;

        if (pointerType.equals(inheritanceType) || pointerType.symmetricTo(inheritanceType)) {
          inheritanceType = pointerType;
          attributeType = null;
        }
        appendSenseChain(buffer, senses[i], inheritanceType, attributeType);
        buffer.append("<br>");
      }
    }
    resultEditorPane.setText(buffer.toString());
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

  void appendSenseChain(
    final StringBuilder buffer, 
    final PointerTarget sense, 
    final PointerType inheritanceType, 
    final PointerType attributeType) {
    appendSenseChain(buffer, sense, inheritanceType, attributeType, 0, null);
  }

  void appendSenseChain(
      final StringBuilder buffer, 
      final PointerTarget sense, 
      final PointerType inheritanceType, 
      final PointerType attributeType, 
      final int tab, 
      Link ancestors) {
    buffer.append("<li>");
    buffer.append(sense.getLongDescription());
    buffer.append("</li>");

    if (attributeType != null) {
      for(final PointerTarget target : sense.getTargets(attributeType)) {
        buffer.append("<li>");
        buffer.append(target.getLongDescription());
        buffer.append("</li>");
      }
    }
    if (attributeType != null) {
      String key = attributeType.getKey();
      if (key == null) {
        key = "";
      }
      // Don't get ancestors for derived or category relationships.
      if (key.startsWith("+") || key.startsWith("-") || key.startsWith(";")) {
        return;
      }
    }
    if (ancestors == null || ancestors.contains(sense) == false) {
      ancestors = new Link(sense, ancestors);
      for(final PointerTarget parent : sense.getTargets(inheritanceType)) {
        buffer.append("<ul>");
        appendSenseChain(buffer, parent, inheritanceType, attributeType, tab + 4, ancestors);
        buffer.append("</ul>");
      }
    }
  }
}
