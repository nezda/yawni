/*
 * WordNet-Java
 *
 * Copyright 1998 by Oliver Steele.  You can use this software freely so long as you preserve
 * the copyright notice and this restriction, and label your changes.
 */
//package edu.brandeis.cs.steele.wn.browser;
package browser;

import edu.brandeis.cs.steele.wn.DictionaryDatabase;
import edu.brandeis.cs.steele.wn.POS;
import edu.brandeis.cs.steele.wn.IndexWord;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import java.util.Enumeration;

class SearchFrame extends Frame {
	protected BrowserPanel browser;
	protected DictionaryDatabase dictionary;
	protected TextField searchField;
	protected java.awt.List resultList;
	protected POS pos = POS.CATS[0];
	
	SearchFrame(BrowserPanel browser) {
		super("Substring Search");
		this.browser = browser;
		this.dictionary = browser.dictionary;
		setVisible(false);
		
		setSize(400,300);
		setLocation(browser.getLocation().x + 20, browser.getLocation().y + 20);
		setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.BOTH;
		
		Panel searchPanel = new Panel(new FlowLayout(FlowLayout.LEFT));
		Label searchLabel = new Label("Substring");
		searchPanel.add(searchLabel);
		searchField = new TextField("", 20);
		searchField.setBackground(Color.white);
		searchPanel.add(searchField);
		searchField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				recomputeResults();
			}
		});
		Choice posChoice = new Choice();
		for (int i = 0; i < POS.CATS.length; ++i) {
			posChoice.add(POS.CATS[i].getLabel());
		}
		posChoice.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent event) {
				Choice choice = (Choice) event.getSource();
				pos = POS.CATS[choice.getSelectedIndex()];
			}
		});
		searchPanel.add(posChoice);
		add(searchPanel, constraints);
		
		constraints.gridx = 0;
		constraints.weightx = constraints.weighty = 1.0;
		resultList = new java.awt.List();
		resultList.setBackground(Color.white);
		resultList.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				IndexWord word = dictionary.lookupIndexWord(pos, resultList.getSelectedItem());
				SearchFrame.this.browser.setWord(word);
			}
		});
		add(resultList, constraints);
		
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent event) {
				setVisible(false);
			}
		});

		validate();
		//setSize(getPreferredSize().width, getPreferredSize().height);
		setVisible(true);
		searchField.requestFocus();
	}
	
	protected void recomputeResults() {
		String searchString = searchField.getText();
		resultList.removeAll();
		resultList.add("Searching for " + searchString + "...");
		resultList.setEnabled(false);
		Vector strings = new Vector();
		for (Enumeration e = dictionary.searchIndexWords(pos, searchString); e.hasMoreElements(); ) {
			IndexWord word = (IndexWord) e.nextElement();
			strings.addElement(word.getLemma());
		}
		resultList.removeAll();
		for (Enumeration e = strings.elements(); e.hasMoreElements(); ) {
			resultList.add((String) e.nextElement());
		}
		resultList.setEnabled(true);
	}
}
