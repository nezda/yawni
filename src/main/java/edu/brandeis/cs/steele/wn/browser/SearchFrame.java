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
import javax.swing.border.*;
import java.util.*;
import java.util.List;
import java.util.prefs.*;

class SearchFrame extends JFrame {
  private static Preferences prefs = Preferences.userNodeForPackage(SearchFrame.class);
  private static final long serialVersionUID = 1L;

  private final Dimension minSize;
  private final BrowserPanel browserPanel;
  final JComponent searchPanel;
  private final JTextField searchField;
  private final JList resultList;

  private final SearchResultsModel resultListModel;
  private POS pos;
  private SearchType searchType;

  enum SearchType {
    SUBSTRING,
    PREFIX
  };

  SearchFrame(final BrowserPanel browserPanel) {
    super("Substring Search");
    this.browserPanel = browserPanel;
    this.pos = POS.CATS[0];
    this.setVisible(false);
    this.searchType = SearchType.valueOf(prefs.get("SearchFrame.searchType", SearchType.SUBSTRING.name()));

    final int metaKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    final KeyListener windowHider = new KeyAdapter() {
      public void keyTyped(final KeyEvent event) {
        if (event.getKeyChar() == 'w' &&
          (event.getModifiers() & metaKey) != 0) {
          setVisible(false);
        }
      }
    };
    this.addKeyListener(windowHider);

    this.setLayout(new BorderLayout());

    this.searchPanel = new JPanel();
    this.searchPanel.setLayout(new GridBagLayout());
    this.searchPanel.setBorder(new EmptyBorder(3,3,3,3));
    this.searchField = new JTextField("", 12);
    this.searchField.putClientProperty("JTextField.variant", "search");


    this.searchField.addKeyListener(windowHider);

    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 0;
    c.gridheight = GridBagConstraints.REMAINDER;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.anchor = GridBagConstraints.EAST;
    c.weightx = 1.0;
    this.searchPanel.add(searchField, c);

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
    posChoice.setFont(posChoice.getFont().deriveFont(posChoice.getFont().getSize()-1f));
    posChoice.setRequestFocusEnabled(false);
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
    
    c = new GridBagConstraints();
    c.gridx = 1;
    c.gridy = 0;
    c.gridy = 0;
    c.gridwidth = GridBagConstraints.REMAINDER;
    this.searchPanel.add(posChoice, c);
    addConstraintButtons(this.searchPanel);

    this.add(this.searchPanel, BorderLayout.NORTH);

    this.resultListModel = new SearchResultsModel();
    this.resultList = new JList(resultListModel);
    this.resultList.setFocusable(false);
    this.resultList.addKeyListener(windowHider);
    this.resultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    this.resultList.setLayoutOrientation(JList.VERTICAL);

    this.resultList.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(final ListSelectionEvent event) {
        if (resultList.isSelectionEmpty()) {
          return;
        }
        final int index = resultList.getSelectedIndex();
        final String lemma = resultListModel.getElementAt(index);
        final Word word = browserPanel.dictionary().lookupWord(pos, lemma);
        if (word == null) {
          System.err.println("NULL WORD for lemma: "+lemma);
          return;
        }
        SearchFrame.this.browserPanel.setWord(word);
      }
    });

    this.resultList.addFocusListener(new FocusAdapter() {
      public void focusGained(final FocusEvent e) {
        if (resultList.isSelectionEmpty() && resultListModel.getSize() > 0) {
          resultList.setSelectedIndex(0);
        }
      }
    });

    final JScrollPane jsp = new  JScrollPane(resultList);
    jsp.setBorder(browserPanel.browser.textAreaBorder);
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
    final int height = 300;
    this.setSize((int)(getPreferredSize().width * 1.1), height);
    this.minSize = new Dimension((int)(getPreferredSize().width * 1.1), getMinimumSize().height);
    setMinimumSize(minSize);
    addComponentListener(new ComponentAdapter() {
      public void componentResized(final ComponentEvent evt) {        
        int width = getWidth();
        int height = getHeight();
        // we check if either the width
        // or the height are below minimum
        boolean resize = false;
        if (width < minSize.width) {
          resize = true;
          width = minSize.width;
        }
        //if (height < minSize.height) {
        //  resize = true;
        //  height = minSize.height;
        //}
        if (resize) {
          setSize(width, height);
        }
      }
    });

    reposition();
    //setSize(getPreferredSize().width, getPreferredSize().height);
    this.setVisible(true);
    this.searchField.requestFocusInWindow();
    this.searchField.setRequestFocusEnabled(true);

    //System.err.println("searchField: "+searchField);
    //System.err.println("  border: "+searchField.getBorder());
    //System.err.println("  insets: "+searchField.getInsets());

    //final ComponentGlassPane glass = new ComponentGlassPane(this);
    //this.setGlassPane(glass);
    //glass.setVisible(true);
  }

  void reposition() {
    //FIXME align top of SearchFrame with top of Browser along its left edge
    final Point browserLocation = browserPanel.getLocationOnScreen();
    final Point adjacent = new Point();
    adjacent.x = browserLocation.x - this.getWidth();
    adjacent.x = Math.max(0, adjacent.x);
    adjacent.y = browserLocation.y;
    this.setLocation(adjacent);
    //XXX this.setLocationRelativeTo(browserPanel);
    //this.setLocation(browserPanel.getLocation().x + 20, browserPanel.getLocation().y + 20);
  }

  synchronized void setSearchText(final String searchText) {
    searchField.setText(searchText);
  }

  private void addConstraintButtons(final JComponent constraintPanel) {
    final ButtonGroup group = new ButtonGroup();
    class SubstringAction extends AbstractAction {
      private static final long serialVersionUID = 1L;
      SubstringAction() {
        super("Substring");
      }
      public void actionPerformed(final ActionEvent evt) {
        searchType = SearchType.SUBSTRING;
        prefs.put("SearchFrame.searchType", SearchType.SUBSTRING.name());
      }
    } // end class SubstringAction
    class PrefixAction extends AbstractAction {
      private static final long serialVersionUID = 1L;
      PrefixAction() {
        super("Prefix");
      }
      public void actionPerformed(final ActionEvent evt) {
        searchType = SearchType.PREFIX;
        prefs.put("SearchFrame.searchType", SearchType.PREFIX.name());
      }
    } // end class PrefixAction
    final Action substring = new SubstringAction();
    final Action prefix = new PrefixAction();
    final JRadioButton substringButton = new JRadioButton(substring);
    substringButton.putClientProperty("JComponent.sizeVariant", "small");
    //XXX substringButton.setFont(substringButton.getFont().deriveFont(substringButton.getFont().getSize()-2f));
    substringButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
    substringButton.setFocusable(false);
    final JRadioButton prefixButton = new JRadioButton(prefix);
    prefixButton.putClientProperty("JComponent.sizeVariant", "small");
    //XXX prefixButton.setFont(prefixButton.getFont().deriveFont(prefixButton.getFont().getSize()-2f));
    prefixButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
    prefixButton.setFocusable(false);
    group.add(substringButton);
    group.add(prefixButton);
    final GridBagConstraints c = new GridBagConstraints();
    c.gridx = 1;
    c.gridy = 1;
    c.ipady = 3;
    c.fill = java.awt.GridBagConstraints.HORIZONTAL;
    c.anchor = java.awt.GridBagConstraints.EAST;
    constraintPanel.add(substringButton, c);
    c.gridx = 2;
    c.gridy = 1;
    c.fill = java.awt.GridBagConstraints.HORIZONTAL;
    c.anchor = java.awt.GridBagConstraints.EAST;
    constraintPanel.add(prefixButton, c);
    switch(searchType) {
      case SUBSTRING: substringButton.setSelected(true); break;
      case PREFIX: prefixButton.setSelected(true); break;
      default: throw new IllegalStateException("Unknown SearchType "+searchType);
    }
  }

  @Override public void setVisible(final boolean visible) {
    super.setVisible(visible);
    if(visible) {
      searchField.requestFocusInWindow();
    }
  }

  String cleanSearchField() {
    // " " is OK
    // " a" is NOT OK
    //return searchField.getText().trim();
    return searchField.getText().replaceAll("\\s+", " ");
  }

  protected void recomputeResults() {
    this.resultList.setFocusable(false);
    final String searchString = cleanSearchField();
    resultListModel.searchingFor(searchString);
    final List<String> lemmas = new ArrayList<String>();
    switch(searchType) {
      case SUBSTRING:
        for (final Word word : browserPanel.dictionary().searchWords(pos, searchString)) {
          lemmas.add(word.getLemma());
        }
        break;
      case PREFIX:
        for (final Word word : browserPanel.dictionary().searchIndexBeginning(pos, searchString)) {
          lemmas.add(word.getLemma());
        }
        break;
      default:
        assert false;
    }
    resultListModel.showResults(searchString, lemmas);
  }

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
      if (size > 0) {
        this.fireIntervalRemoved(this, 0, size - 1);
      }
      //FIXME this doesn't work - needs some threads
      this.lemmas = Collections.singletonList("Searching for " + searchString + "...");
      this.fireIntervalAdded(this, 0, 0);
    }
    void showResults(final String searchString, final List<String> lemmas) {
      final int size = this.getSize();
      this.lemmas = Collections.emptyList();
      if (size > 0) {
        this.fireIntervalRemoved(this, 0, size - 1);
      }
      this.lemmas = lemmas;
      final int newSize = this.getSize();
      if (newSize > 0) {
        resultList.setFocusable(true);
        this.fireIntervalAdded(this, 0, newSize - 1);
      }
    }
  } // end class SearchResultsModel
}
