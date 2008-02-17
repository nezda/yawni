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
import java.util.Vector;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;




public class BrowserPanel extends Panel {
    Log log = LogFactory.getLog(this.getClass());

	protected DictionaryDatabase dictionary;
	
	protected TextField searchField;
	protected TextArea resultTextArea;
	protected Checkbox[] posBoxes = new Checkbox[POS.CATS.length];
	protected Choice[] posChoices = new Choice[POS.CATS.length];
	
	public BrowserPanel(DictionaryDatabase dictionary) {
		this.dictionary = dictionary;
		
		Panel searchPanel;
		Panel pointerPanel;
		Label searchLabel;

		setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = GridBagConstraints.RELATIVE;
		constraints.fill = GridBagConstraints.BOTH;
		searchPanel = new Panel(new FlowLayout(FlowLayout.LEFT));
		searchPanel.setBackground(Color.lightGray);
		searchLabel = new Label("Search IndexWord:");
		searchPanel.add(searchLabel);
		searchField = new TextField("", 20);
		searchField.setBackground(Color.white);
		searchPanel.add(searchField);
		add(searchPanel, constraints);
		
		pointerPanel = makePointerPanel();
		add(pointerPanel, constraints);
		
		constraints.weightx = constraints.weighty = 1.0;
		resultTextArea = new TextArea(80, 24);
		resultTextArea.setBackground(Color.white);
		resultTextArea.setEditable(false);
		add(resultTextArea, constraints);
		
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

	protected Panel makePointerPanel() {
		Panel pointerPanel = new Panel();
		pointerPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		pointerPanel.setBackground(Color.lightGray);
		
		final CheckboxGroup group = new CheckboxGroup();
		for (int i = 0; i < POS.CATS.length; ++i) {

			final POS pos = POS.CATS[i];
			final Checkbox box = posBoxes[i] = new Checkbox(pos.getLabel(), group, false);
			final Choice choice = posChoices[i] = new Choice();
            final Vector pointerTypes = new Vector();
            if (log.isDebugEnabled()) {
                log.debug("POS:"+pos.getLabel());
            }

			choice.addItem("Senses");

            for (int j = 0; j < PointerType.TYPES.length; ++j) {
                PointerType pointerType = PointerType.TYPES[j];
                if (pointerType.appliesTo(pos)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Pointer Type:"+pointerType.getLabel());
                    }
                    choice.addItem(pointerType.getLabel());
                    pointerTypes.addElement(pointerType);
                }
            }

			ItemListener listener = new ItemListener() {
				public void itemStateChanged(ItemEvent event) {
                    IndexWord word = dictionary.lookupIndexWord(pos, searchField.getText());

                    group.setSelectedCheckbox(box);
                    int selection = choice.getSelectedIndex();
                    if (selection == 0) {
                            displaySenses(word);
                    } else {
                            displaySenseChain(word, (PointerType) pointerTypes.elementAt(selection - 1));
                    }
				}
			};

			box.addItemListener(listener);
			choice.addItemListener(listener);
			Panel panel = new Panel(new GridBagLayout());
			GridBagConstraints constraints = new GridBagConstraints();
			panel.add(box, constraints);
			constraints.gridy = GridBagConstraints.RELATIVE;
			constraints.gridx = 0;
			panel.add(choice, constraints);
			pointerPanel.add(panel);
			box.setEnabled(false);
			choice.setEnabled(false);
		}
		
		
		return pointerPanel;
	}
	
	synchronized void setWord(IndexWord word) {
		searchField.setText(word.getLemma());
		displayOverview();
	}
	
	protected synchronized void displayOverview() {
		String lemma = searchField.getText();
		StringBuffer buffer = new StringBuffer();
		boolean definitionExists = false;
		for (int i = 0; i < POS.CATS.length; ++i) {
			IndexWord word = dictionary.lookupIndexWord(POS.CATS[i], lemma);
			appendSenses(word, buffer);
			boolean enabled = (word != null);
			posBoxes[i].setEnabled(enabled);
			posChoices[i].setEnabled(enabled);
			definitionExists |= enabled;
		}
		if (!definitionExists) {
			buffer.append("\"" + lemma + "\" is not defined.");
		}
		resultTextArea.setText(buffer.toString());
	}
	
	protected synchronized void displaySenses(IndexWord word) {
		StringBuffer buffer = new StringBuffer();
		appendSenses(word, buffer);
		resultTextArea.setText(buffer.toString());
	}
	
	protected void appendSenses(IndexWord word, StringBuffer buffer) {
		if (word != null) {
			Synset[] senses = word.getSenses();
			int taggedCount = word.getTaggedSenseCount();
			buffer.append("The " + word.getPOS().getLabel() + " " + word.getLemma() + " has " + senses.length + " sense" + (senses.length == 1 ? "" : "s") + " ");
			buffer.append("(");
			if (taggedCount == 0) {
				buffer.append("no senses from tagged texts");
			} else {
				buffer.append("first " + taggedCount + " from tagged texts");
			}
			buffer.append(")\n\n");
			for (int i = 0; i < senses.length; ++i) {
				Synset sense = senses[i];
				buffer.append("" + (i + 1) + ". " + sense.getLongDescription());
				buffer.append('\n');
			}
			buffer.append('\n');
		}
	}

	protected synchronized void displaySenseChain(IndexWord word, PointerType pointerType) {
		StringBuffer buffer = new StringBuffer();
		Synset[] senses = word.getSenses();
		buffer.append("" + senses.length + " senses of " + word.getLemma() + "\n\n");
		for (int i = 0; i < senses.length; ++i) {
			if (senses[i].getTargets(pointerType).length > 0) {
				buffer.append("Sense " + (i + 1) + "\n");
                                
				PointerType inheritanceType = PointerType.HYPERNYM;
				PointerType attributeType = pointerType;
                                
				if (pointerType.equals(inheritanceType) || pointerType.symmetricTo(inheritanceType)) {
					inheritanceType = pointerType;
					attributeType = null;
				}
				appendSenseChain(buffer, senses[i], inheritanceType, attributeType);
				buffer.append('\n');

			}
		}
		resultTextArea.setText(buffer.toString());
	}
	

	protected class Link {
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
	}
	

	void appendSenseChain(StringBuffer buffer, PointerTarget sense, PointerType inheritanceType, PointerType attributeType) {
		appendSenseChain(buffer, sense, inheritanceType, attributeType, 0, null);
	}
	

	void appendSenseChain(StringBuffer buffer, PointerTarget sense, PointerType inheritanceType, PointerType attributeType, int tab, Link ancestors) {
		for (int i = 0; i < tab; ++i) {
			buffer.append(' ');
		}
		if (tab > 0) {
			buffer.append("=> ");
		}
        buffer.append(sense.getLongDescription());
		buffer.append('\n');
		
		if (attributeType != null) {
			PointerTarget[] targets = sense.getTargets(attributeType);
			for (int i = 0; i < targets.length; ++i) {
				for (int j = 0; j < tab; ++j) {
					buffer.append(' ');
				}
                                
				buffer.append(targets[i].getLongDescription());
				buffer.append('\n');
                                
                                
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
		if (ancestors == null || !ancestors.contains(sense)) {
			ancestors = new Link(sense, ancestors);
			PointerTarget[] parents = sense.getTargets(inheritanceType);
			for (int i = 0; i < parents.length; ++i) {
				appendSenseChain(buffer, parents[i], inheritanceType, attributeType, tab + 4, ancestors);
			}
		}
	}
}
