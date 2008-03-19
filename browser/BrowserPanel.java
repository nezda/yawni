/*
 * WordNet-Java
 *
 * Copyright 1998 by Oliver Steele.  You can use this software freely so long as you preserve
 * the copyright notice and this restriction, and label your changes.
 */
//package edu.brandeis.cs.steele.wn.browser;
package browser;

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

  protected DictionaryDatabase dictionary;

  protected JTextField searchField;
  protected JEditorPane resultEditorPane;
  protected JComboBox[] posBoxes = new JComboBox[POS.CATS.length];

  public BrowserPanel(DictionaryDatabase dictionary) {
    this.dictionary = dictionary;

    JPanel searchPanel;
    JPanel pointerPanel;
    JLabel searchLabel;

    setLayout(new GridBagLayout());
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = GridBagConstraints.RELATIVE;
    constraints.fill = GridBagConstraints.BOTH;
    searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    searchPanel.setBackground(Color.LIGHT_GRAY);
    searchLabel = new JLabel("Search Word:");
    searchPanel.add(searchLabel);
    searchField = new JTextField("", 20);
    searchField.setBackground(Color.WHITE);
    searchPanel.add(searchField);
    add(searchPanel, constraints);

    pointerPanel = makePointerPanel();
    add(pointerPanel, constraints);

    constraints.weightx = constraints.weighty = 1.0;
    resultEditorPane = new JEditorPane();
    resultEditorPane.setContentType("text/html");
    resultEditorPane.setBackground(Color.WHITE);
    resultEditorPane.setEditable(false);
    add(new JScrollPane(resultEditorPane), constraints);

    searchField.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        displayOverview();
      }
    });

    validate();
  }

  public void setVisible(boolean visible) {
    searchField.requestFocus();
    super.setVisible(visible);
  }

  protected JPanel makePointerPanel() {
    JPanel pointerPanel = new JPanel();
    pointerPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
    pointerPanel.setBackground(Color.LIGHT_GRAY);

    final ButtonGroup group = new ButtonGroup();
    for (int i = 0; i < POS.CATS.length; ++i) {

      final POS pos = POS.CATS[i];
      final Vector pointerTypes = new Vector();
      pointerTypes.add(pos.getLabel()); // this will be a marker
      pointerTypes.add("Senses"); // this is special

      for (int j = 0; j < PointerType.TYPES.length; ++j) {
        final PointerType pointerType = PointerType.TYPES[j];
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
          if(event.getStateChange() == ItemEvent.DESELECTED) {
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
      JPanel panel = new JPanel(new GridBagLayout());
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

  synchronized void setWord(IndexWord word) {
    searchField.setText(word.getLemma());
    displayOverview();
  }

  protected synchronized void displayOverview() {
    final String inputString = searchField.getText().trim(); // FIXME trim edge whitespace and normalize internal space
    final StringBuffer buffer = new StringBuffer();
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
      if(forms == null) {
        forms = new String[]{ inputString };
      } else {
        boolean found = false;
        for(final String form : forms) {
          if(form.equals(inputString)) {
            found = true;
            break;
          }
        }
        if(forms != null && forms.length > 0 && found == false) {
          //throw new RuntimeException("inputString: \"" + inputString +
          //    "\" not found in forms: "+Arrays.toString(forms));
          System.err.println("    BrowserPanel inputString: \"" + inputString +
              "\" not found in forms: "+Arrays.toString(forms));
        }
      }
      IndexWord word = null;
      boolean enabled = false;
      for(final String form : forms) {
        word = dictionary.lookupIndexWord(pos, form);
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
  }

  protected synchronized void displaySenses(IndexWord word) {
    StringBuffer buffer = new StringBuffer();
    appendSenses(word, buffer);
    resultEditorPane.setText(buffer.toString());
  }

  protected void appendSenses(IndexWord word, StringBuffer buffer) {
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
      for (int i = 0; i < senses.length; ++i) {
        final Synset sense = senses[i];
        buffer.append("<li>");
        buffer.append(sense.getLongDescription());
        buffer.append("</li>");
      }
      buffer.append("</ol>");
    }
  }

  protected synchronized void displaySenseChain(IndexWord word, PointerType pointerType) {
    StringBuffer buffer = new StringBuffer();
    Synset[] senses = word.getSynsets();
    buffer.append("" + senses.length + " senses of <b>" + word.getLemma() + "</b><br><br>");
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

  protected static class Link {
    Object object;
    Link link;

    Link(Object object, Link link) {
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

  void appendSenseChain(StringBuffer buffer, PointerTarget sense, PointerType inheritanceType, PointerType attributeType) {
    appendSenseChain(buffer, sense, inheritanceType, attributeType, 0, null);
  }

  void appendSenseChain(
      final StringBuffer buffer, 
      final PointerTarget sense, 
      final PointerType inheritanceType, 
      final PointerType attributeType, 
      final int tab, 
      Link ancestors) {
    buffer.append("<li>");
    buffer.append(sense.getLongDescription());
    buffer.append("</li>");

    if (attributeType != null) {
      final PointerTarget[] targets = sense.getTargets(attributeType);
      for (int i = 0; i < targets.length; ++i) {
        buffer.append("<li>");
        buffer.append(targets[i].getLongDescription());
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
      final PointerTarget[] parents = sense.getTargets(inheritanceType);
      for (int i = 0; i < parents.length; ++i) {
        buffer.append("<ul>");
        appendSenseChain(buffer, parents[i], inheritanceType, attributeType, tab + 4, ancestors);
        buffer.append("</ul>");
      }
    }
  }
}
