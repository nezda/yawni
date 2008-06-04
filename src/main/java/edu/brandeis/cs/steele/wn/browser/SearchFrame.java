/*
 * WordNet-Java
 *
 * Copyright 1998 by Oliver Steele.  You can use this software freely so long as you preserve
 * the copyright notice and this restriction, and label your changes.
 */
package edu.brandeis.cs.steele.wn.browser;

import edu.brandeis.cs.steele.wn.POS;
import edu.brandeis.cs.steele.wn.Word;
import edu.brandeis.cs.steele.util.MutatedIterable;

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
  private final ConcurrentSearchListModel searchListModel;
  private final JList resultList;
  private POS pos;
  private SearchType searchType;
  private static final String LONGEST_WORD = "blood-oxygenation level dependent functional magnetic resonance imaging";

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

    final Action slashAction = new AbstractAction("Slash") {
      private static final long serialVersionUID = 1L;
      public void actionPerformed(final ActionEvent evt) {
        searchField.grabFocus();
      }
    };

    final JComboBox posChoice = new JComboBox();
    posChoice.setFont(posChoice.getFont().deriveFont(posChoice.getFont().getSize() - 1f));
    posChoice.setRequestFocusEnabled(false);
    posChoice.addKeyListener(windowHider);
    for (final POS pos : POS.CATS) {
      posChoice.addItem(BrowserPanel.capitalize(pos.getLabel()));
    }
    posChoice.addItemListener(new ItemListener() {
      public void itemStateChanged(final ItemEvent evt) {
        final JComboBox posChoice = (JComboBox) evt.getSource();
        pos = POS.CATS[posChoice.getSelectedIndex()];
        reissueSearch();
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
    this.addConstraintButtons(this.searchPanel);

    this.add(this.searchPanel, BorderLayout.NORTH);

    this.searchListModel = new ConcurrentSearchListModel() {
      private static final long serialVersionUID = 1L;
      @Override 
      public Iterable search(final String query) {
        // performs the actual search
        switch(searchType) {
          case SUBSTRING:
            return new WordToLemma(browserPanel.dictionary().searchWords(pos, query));
          case PREFIX:
            return new WordToLemma(browserPanel.dictionary().searchIndexBeginning(pos, query));
          default:
            throw new IllegalArgumentException();
        }
      }
    };
    this.searchField.getDocument().addDocumentListener(this.searchListModel);
    this.resultList = new JList(searchListModel);
    this.resultList.setPrototypeCellValue(LONGEST_WORD);
    this.searchListModel.setJList(this.resultList);
    this.resultList.addKeyListener(windowHider);
    this.resultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    this.resultList.setLayoutOrientation(JList.VERTICAL);

    this.resultList.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(final ListSelectionEvent evt) {
        if (resultList.isSelectionEmpty()) {
          return;
        }
        final int index = resultList.getSelectedIndex();
        //XXX final String lemma = resultListModel.getElementAt(index);
        final String lemma = searchListModel.getElementAt(index).toString();
        final Word word = browserPanel.dictionary().lookupWord(pos, lemma);
        if (word == null) {
          System.err.println("NULL WORD for lemma: "+lemma);
          return;
        }
        SearchFrame.this.browserPanel.setWord(word);
      }
    });

    this.resultList.addFocusListener(new FocusAdapter() {
      public void focusGained(final FocusEvent evt) {
        if (resultList.isSelectionEmpty() && searchListModel.getSize() > 0) {
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
      public void windowClosing(final WindowEvent evt) {
        setVisible(false);
      }
    });

    validate();
    final int height = 300;
    final int width = (int)(getPreferredSize().width * 1.1);
    this.setSize(Math.min(300, width), height);
    this.minSize = new Dimension(300, getMinimumSize().height);
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
  
  static class WordToLemma extends MutatedIterable<Word, String> {
    WordToLemma(final Iterable<Word> iterable) {
      super(iterable, String.class);
    }
    @Override
    public String apply(final Word word) { 
      return word.getLemma(); 
    }
  } // end class WordToLemma

  void reposition() {
    //TODO align top of SearchFrame with top of Browser along its left edge
    final Point browserLocation = browserPanel.getLocationOnScreen();
    final Point adjacent = new Point();
    adjacent.x = browserLocation.x - this.getWidth();
    //TODO if adjacent.x < 0, consider aligning with the left edge instead
    adjacent.x = Math.max(0, adjacent.x);
    adjacent.y = browserLocation.y;
    this.setLocation(adjacent);
    //XXX this.setLocationRelativeTo(browserPanel);
    //this.setLocation(browserPanel.getLocation().x + 20, browserPanel.getLocation().y + 20);
  }

  /** Used by BrowserPanel to pre-populate the search field */
  synchronized void setSearchText(final String searchText) {
    searchField.setText(searchText);
  }
  
  private void reissueSearch() {
    // pos may change, SearchType may change
    // trigger DocumentEvent which will cause updates to be displayed
    searchField.setText(searchField.getText());
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
        reissueSearch();
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
        reissueSearch();
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

  @Override 
  public void setVisible(final boolean visible) {
    super.setVisible(visible);
    if(visible) {
      searchField.requestFocusInWindow();
    }
  }

  //XXX unused
  String cleanSearchField() {
    // " " is OK
    // " a" is NOT OK (translate to "a")
    //return searchField.getText().trim();
    //FIXME return searchField.getText().replaceAll("\\s+", " ");
    return searchField.getText();
  }
}
