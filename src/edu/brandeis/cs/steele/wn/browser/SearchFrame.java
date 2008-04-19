/*
 * WordNet-Java
 *
 * Copyright 1998 by Oliver Steele.  You can use this software freely so long as you preserve
 * the copyright notice and this restriction, and label your changes.
 */
package edu.brandeis.cs.steele.wn.browser;

import edu.brandeis.cs.steele.wn.DictionaryDatabase;
import edu.brandeis.cs.steele.wn.POS;
import edu.brandeis.cs.steele.wn.Word;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.undo.*;
import java.util.*;
import java.util.List;

class SearchFrame extends JFrame {
  private static final long serialVersionUID = 1L;

  private final BrowserPanel browser;
  private final DictionaryDatabase dictionary;
  private final JTextField searchField;
  private final JList resultList;
  private final SearchResultsModel resultListModel;
  private POS pos;

  private class SearchResultsModel extends AbstractListModel {
    private static final long serialVersionUID = 1L;
    private List<String> lemmas = new ArrayList<String>();
    public String getElementAt(int i) {
      return lemmas.get(i);
    }
    public int getSize() {
      return lemmas.size();
    }
    void searchingFor(final String searchString) {
      //System.err.println("isEventDispatchThread: "+SwingUtilities.isEventDispatchThread());
      final int size = this.getSize();
      this.lemmas = Collections.emptyList();
      if(size > 0) {
        this.fireIntervalRemoved(this, 0, size - 1);
      }
      this.lemmas = Collections.singletonList("Searching for " + searchString + "...");
      this.fireIntervalAdded(this, 0, 0);
    }
    void showResults(final String searchString, final List<String> lemmas) {
      final int size = this.getSize();
      this.lemmas = Collections.emptyList();
      if(size > 0) {
        this.fireIntervalRemoved(this, 0, size - 1);
      }
      this.lemmas = lemmas;
      final int newSize = this.getSize();
      if(newSize > 0) {
        resultList.setFocusable(true);
        this.fireIntervalAdded(this, 0, newSize - 1);
      }
    }
  } // end class SearchResultsModel

  SearchFrame(final BrowserPanel browser) {
    super("Substring Search");
    this.browser = browser;
    this.dictionary = browser.dictionary;
    this.pos = POS.CATS[0];
    this.setVisible(false);

    final int metaKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    final KeyListener windowHider = new KeyAdapter() {
      public void keyTyped(final KeyEvent event) {
        if(event.getKeyChar() == 'w' &&
          (event.getModifiers() & metaKey) != 0) {
          setVisible(false);
        }
      }
    };
    this.addKeyListener(windowHider);

    this.setSize(400, 300);
    this.setLocation(browser.getLocation().x + 20, browser.getLocation().y + 20);
    this.setLayout(new BorderLayout());

    final JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    final JLabel searchLabel = new JLabel("Substring");
    searchPanel.add(searchLabel);
    this.searchField = new JTextField("", 12);
    //fairly involved to add: 
    //make undo/redo actions, bind to keys Ctrl+z, Ctrl+z+shift
    //final UndoManager undoManager = new UndoManager();
    //this.searchField.getDocument().addUndoableEditListener(undoManager);
    
    this.searchField.addKeyListener(windowHider);
    searchPanel.add(searchField);
    this.searchField.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent event) {
        searchField.selectAll();
        recomputeResults();
      }
    });

    final Action slashAction = new AbstractAction("Slash") {
      private static final long serialVersionUID = 1L;
      public void actionPerformed(final ActionEvent event) {
        searchField.grabFocus();
      }
    };

    final JComboBox posChoice = new JComboBox();
    posChoice.addKeyListener(windowHider);
    for (final POS pos : POS.CATS) {
      posChoice.addItem(BrowserPanel.capitalize(pos.getLabel()));
    }
    posChoice.addItemListener(new ItemListener() {
      public void itemStateChanged(final ItemEvent event) {
        final JComboBox posChoice = (JComboBox) event.getSource();
        pos = POS.CATS[posChoice.getSelectedIndex()];
        recomputeResults();
      }
    });
    posChoice.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, 0, false), "Slash");
    posChoice.getActionMap().put("Slash", slashAction);
    searchPanel.add(posChoice);
    this.add(searchPanel, BorderLayout.NORTH);

    this.resultListModel = new SearchResultsModel();
    this.resultList = new JList(resultListModel);
    this.resultList.setFocusable(false);
    this.resultList.addKeyListener(windowHider);
    this.resultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    this.resultList.setLayoutOrientation(JList.VERTICAL);

    this.resultList.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(final ListSelectionEvent event) {
        if(resultList.isSelectionEmpty()) {
          return;
        }
        final int index = resultList.getSelectedIndex();
        final String lemma = resultListModel.getElementAt(index);
        final Word word = dictionary.lookupWord(pos, lemma);
        if(word == null) {
          System.err.println("NULL WORD for lemma: "+lemma);
          return;
        }
        SearchFrame.this.browser.setWord(word);
      }
    });

    this.resultList.addFocusListener(new FocusAdapter() {
      public void focusGained(final FocusEvent e) {
        if(resultList.isSelectionEmpty() && resultListModel.getSize() > 0) {
          resultList.setSelectedIndex(0);
        }
      }
    });

    final JScrollPane jsp = new  JScrollPane(resultList);
    jsp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, 0, false), "Slash");
    jsp.getActionMap().put("Slash", slashAction);
    jsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    jsp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    this.add(jsp, BorderLayout.CENTER);

    this.addWindowListener(new WindowAdapter() {
      public void windowClosing(final WindowEvent event) {
        setVisible(false);
      }
    });

    validate();
    //setSize(getPreferredSize().width, getPreferredSize().height);
    this.setVisible(true);
    this.searchField.requestFocusInWindow();
  }

  protected void recomputeResults() {
    this.resultList.setFocusable(false);
    final String searchString = searchField.getText().trim();
    resultListModel.searchingFor(searchString);
    final List<String> lemmas = new ArrayList<String>();
    for (final Word word : dictionary.searchWords(pos, searchString)) {
      lemmas.add(word.getLemma());
    }
    resultListModel.showResults(searchString, lemmas);
  }
}
