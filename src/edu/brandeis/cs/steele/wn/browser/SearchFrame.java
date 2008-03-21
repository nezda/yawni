/*
 * WordNet-Java
 *
 * Copyright 1998 by Oliver Steele.  You can use this software freely so long as you preserve
 * the copyright notice and this restriction, and label your changes.
 */
package edu.brandeis.cs.steele.wn.browser;

import edu.brandeis.cs.steele.wn.DictionaryDatabase;
import edu.brandeis.cs.steele.wn.POS;
import edu.brandeis.cs.steele.wn.IndexWord;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;
import java.util.List;

class SearchFrame extends JFrame {
  private static final long serialVersionUID = 1L;

  protected BrowserPanel browser;
  protected DictionaryDatabase dictionary;
  protected JTextField searchField;
  //protected java.awt.List resultList;
  protected JList resultList;
  private DefaultListModel resultListModel;
  protected POS pos = POS.CATS[0];

  SearchFrame(final BrowserPanel browser) {
    super("Substring Search");
    this.browser = browser;
    this.dictionary = browser.dictionary;
    setVisible(false);

    setSize(400,300);
    setLocation(browser.getLocation().x + 20, browser.getLocation().y + 20);
    setLayout(new GridBagLayout());
    final GridBagConstraints constraints = new GridBagConstraints();
    constraints.fill = GridBagConstraints.BOTH;

    final JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    final JLabel searchLabel = new JLabel("Substring");
    searchPanel.add(searchLabel);
    searchField = new JTextField("", 12);
    //searchField.setBackground(Color.WHITE);
    searchPanel.add(searchField);
    searchField.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        recomputeResults();
      }
    });
    final Choice posChoice = new Choice();
    for (int i = 0; i < POS.CATS.length; ++i) {
      posChoice.add(POS.CATS[i].getLabel());
    }
    posChoice.addItemListener(new ItemListener() {
      public void itemStateChanged(final ItemEvent event) {
        final Choice choice = (Choice) event.getSource();
        pos = POS.CATS[choice.getSelectedIndex()];
      }
    });
    searchPanel.add(posChoice);
    add(searchPanel, constraints);

    constraints.gridx = 0;
    constraints.weightx = constraints.weighty = 1.0;
    //resultList = new java.awt.List();
    resultListModel = new DefaultListModel();
    resultList = new JList(resultListModel);
    resultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    resultList.setLayoutOrientation(JList.VERTICAL);
    
    //resultList.setBackground(Color.WHITE);
    //XXX resultList.addActionListener(new ActionListener() {
    //XXX   public void actionPerformed(final ActionEvent event) {
    //XXX     int index = resultList.getSelectedIndex();
    //XXX     //XXX final IndexWord word = dictionary.lookupIndexWord(pos, resultList.getSelectedItem());
    //XXX     final IndexWord word = dictionary.lookupIndexWord(pos, (String)resultListModel.getElementAt(index));
    //XXX     SearchFrame.this.browser.setWord(word);
    //XXX   }
    //XXX });
    resultList.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(final ListSelectionEvent event) {
        int index = event.getFirstIndex();
        //XXX final IndexWord word = dictionary.lookupIndexWord(pos, resultList.getSelectedItem());
        final IndexWord word = dictionary.lookupIndexWord(pos, (String)resultListModel.getElementAt(index));
        SearchFrame.this.browser.setWord(word);
      }
    });
    add(new JScrollPane(resultList), constraints);

    addWindowListener(new WindowAdapter() {
      public void windowClosing(final WindowEvent event) {
        setVisible(false);
      }
    });

    validate();
    //setSize(getPreferredSize().width, getPreferredSize().height);
    setVisible(true);
    searchField.requestFocus();
  }

  protected void recomputeResults() {
    final String searchString = searchField.getText();
    resultListModel.removeAllElements();
    resultListModel.addElement("Searching for " + searchString + "...");
    resultList.setEnabled(false);
    final List<String> strings = new ArrayList<String>();
    for (final IndexWord word : dictionary.searchIndexWords(pos, searchString)) {
      strings.add(word.getLemma());
    }
    resultListModel.removeAllElements();
    for (final String e : strings) {
      resultListModel.addElement(e);
    }
    resultList.setEnabled(true);
  }
}
