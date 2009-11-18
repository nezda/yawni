/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.yawni.wn.browser;

import org.yawni.wn.POS;
import org.yawni.wn.Word;
import org.yawni.wn.WordToLemma;
import org.yawni.util.MergedIterable;
import org.yawni.util.Utils;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.util.prefs.*;
import org.yawni.wn.browser.ActionHelper;

// TODO add Meta + F everywhere we have "slash"
class SearchFrame extends JDialog {
  private static Preferences prefs = Preferences.userNodeForPackage(SearchFrame.class).node(SearchFrame.class.getSimpleName());
  private static final long serialVersionUID = 1L;

  private final Dimension minSize;
  private final Browser browser;
  private final BrowserPanel browserPanel;
  private final JComponent searchPanel;
  private final JTextField searchField;
  private final ConcurrentSearchListModel searchListModel;
  private final JList resultList;
  private final JLabel statusLabel;
  private final JComboBox posChoice;
  private POS pos;
  private SearchType searchType;
  //private static final String LONGEST_WORD = "blood-oxygenation level dependent functional magnetic resonance imaging";

  private static final POS[] CATS = { POS.NOUN, POS.VERB, POS.ADJ, POS.ADV, POS.ALL };

  enum SearchType {
    /**  operates on lemmas (Word) */
    SUBSTRING,
    /** operates on lemmas (Word) */
    PREFIX,
    /** operates on glosses (Synset) */
    GLOSS_SUBSTRING,
    //TODO regex - could be used to search lemmas or glosses
  };

  SearchFrame(final Browser browser, final BrowserPanel browserPanel) {
    super(browser, "Substring Search");
    // this is the preferred way to set brushMetalRounded
    // http://lists.apple.com/archives/Java-dev/2007/Nov/msg00081.html
    getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
    // doesn't add anything
    //getRootPane().putClientProperty("Window.style", "small");
    
    this.browser = browser;
    this.browserPanel = browserPanel;
    this.setVisible(false);
    this.pos = POS.valueOf(prefs.get("searchPOS", POS.ALL.name()));
    this.searchType = SearchType.valueOf(prefs.get("searchType", SearchType.SUBSTRING.name()));

    // Command+W
    final KeyListener windowHider = new KeyAdapter() {
      @Override
      public void keyTyped(final KeyEvent evt) {
        if (evt.getKeyChar() == 'w' &&
          (evt.getModifiers() & Browser.MENU_MASK) != 0) {
          setVisible(false);
        }
      }
    };
    this.addKeyListener(windowHider);

    // handle window manager window close
    this.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent evt) {
        setVisible(false);
      }
    });

    // Control JList with up/down arrow keys in adjacent JTextField searchField
    // without surrendering focus.
    final KeyListener listArrowNav = new KeyAdapter() {
      @Override
      public void keyPressed(final KeyEvent evt) {
        assert evt.getSource() == searchField;
        if (evt.getKeyCode() == KeyEvent.VK_UP ||
            evt.getKeyCode() == KeyEvent.VK_DOWN) {
          final int size = resultList.getModel().getSize();
          Integer nextSi = null;
          if (size == 0) {
            return;
          }
          final int si = resultList.getSelectedIndex();
          if (si < 0) {
            nextSi = 0;
          }
          if (evt.getKeyCode() == KeyEvent.VK_UP) {
            if (si != 0) {
              nextSi = si - 1;
            }
          } else  if (evt.getKeyCode() == KeyEvent.VK_DOWN) {
            final int max = resultList.getModel().getSize() - 1;
            if (si < max) {
              nextSi = si + 1;
            }
          }
          if (nextSi != null) {
            // FIXME has produced exceptions (nextSi = -2)
            resultList.setSelectedIndex(nextSi);
            resultList.ensureIndexIsVisible(nextSi);
          }
          // don't use these arrow keys as input to searchField
          evt.consume();
        }
      }
    };

    this.searchPanel = new JPanel();

    final MoveMouseListener searchWindowMouseListener = new MoveMouseListener(searchPanel);
    this.addMouseListener(searchWindowMouseListener);
    this.addMouseMotionListener(searchWindowMouseListener);

    this.searchPanel.setLayout(new GridBagLayout());
    this.searchPanel.setBorder(new EmptyBorder(3 /*top*/, 3 /*left*/, 3 /*bottom*/, 3 /*right*/));

    final int searchFieldWidth = 12;
    this.searchField = new JTextField("", searchFieldWidth);
    this.searchField.setDocument(new SearchFieldDocument());
    // rounded corners and magnifying glass icon on OS X
    this.searchField.putClientProperty("JTextField.variant", "search");
    this.searchField.putClientProperty("JTextField.Search.CancelAction",
      ActionHelper.selectAllCut()
      );
    this.searchField.addKeyListener(windowHider);
    this.searchField.addKeyListener(listArrowNav);
    multiClickSelectAll(this.searchField);

    // use slash key ("/") to initiate search like vi, gmail, ...
    final Action slashAction = new AbstractAction("Slash") {
      private static final long serialVersionUID = 1L;
      public void actionPerformed(final ActionEvent evt) {
        searchField.selectAll();
        searchField.grabFocus();
      }
    };

    // build POS chooser
    this.posChoice = new JComboBox();
    this.posChoice.putClientProperty("JComboBox.isPopDown", Boolean.TRUE);
    this.posChoice.setFont(this.posChoice.getFont().deriveFont(this.posChoice.getFont().getSize() - 1f));
    this.posChoice.setRequestFocusEnabled(false);
    this.posChoice.addKeyListener(windowHider);
    int idx = -1;
    for (final POS pos : CATS) {
      idx++;
      this.posChoice.addItem(Utils.capitalize(pos.getLabel()));
      if (pos == this.pos) {
        this.posChoice.setSelectedIndex(idx);
      }
    }
    this.posChoice.addItemListener(new ItemListener() {
      public void itemStateChanged(final ItemEvent evt) {
        assert posChoice == evt.getSource();
        final POS pos = getSelectedPOS();
        prefs.put("searchPOS", pos.name());
        reissueSearch();
      }
    });
    this.posChoice.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, 0, false), "Slash");
    this.posChoice.getActionMap().put("Slash", slashAction);

    // layout searchField and controls
    this.setLayout(new BorderLayout());

    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 0;
    c.gridheight = GridBagConstraints.REMAINDER;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.anchor = GridBagConstraints.EAST;
    c.weightx = 1.0;
    this.searchPanel.add(searchField, c);

    c = new GridBagConstraints();
    c.gridx = 1;
    c.gridy = 0;
    c.gridwidth = GridBagConstraints.REMAINDER;
    this.searchPanel.add(this.posChoice, c);
    this.addConstraintButtons(this.searchPanel);

    this.add(this.searchPanel, BorderLayout.NORTH);

    // build re-active searching ListModel + DocumentListener
    this.searchListModel = new ConcurrentSearchListModel() {
      private static final long serialVersionUID = 1L;
      @Override
      public Iterable search(final String query) {
        // performs the actual search
        final Iterable<Word> searchResults;
        switch (searchType) {
          case SUBSTRING:
            if (pos != POS.ALL) {
              searchResults = browserPanel.dictionary().searchBySubstring(query, pos);
            } else {
              searchResults = MergedIterable.merge(
                  browserPanel.dictionary().searchBySubstring(query, POS.NOUN),
                  browserPanel.dictionary().searchBySubstring(query, POS.VERB),
                  browserPanel.dictionary().searchBySubstring(query, POS.ADJ),
                  browserPanel.dictionary().searchBySubstring(query, POS.ADV));
            }
            break;
          case PREFIX:
            if (pos != POS.ALL) {
              searchResults = browserPanel.dictionary().searchByPrefix(query, pos);
            } else {
              searchResults = MergedIterable.merge(
                  browserPanel.dictionary().searchByPrefix(query, POS.NOUN),
                  browserPanel.dictionary().searchByPrefix(query, POS.VERB),
                  browserPanel.dictionary().searchByPrefix(query, POS.ADJ),
                  browserPanel.dictionary().searchByPrefix(query, POS.ADV));
            }
            break;
//          case GLOSS_SUBSTRING:
//            searchResults = MergedIterable.merge(
//                  browserPanel.dictionary().searchGlossBySubstring(query, POS.NOUN),
//                  browserPanel.dictionary().searchGlossBySubstring(query, POS.VERB),
//                  browserPanel.dictionary().searchGlossBySubstring(query, POS.ADJ),
//                  browserPanel.dictionary().searchGlossBySubstring(query, POS.ADV));
//            break;
          default:
            throw new IllegalArgumentException("unsupported SearchType "+searchType);
        }
        return Utils.uniq(new WordToLemma(searchResults));
      }
      @Override
      public void searchDone(final String query, final int numHits) {
        if (numHits != 0) {
          if (numHits != 1) {
            //updateStatusBar(Status.SUMMARY, numHits, searchType, pos);
            updateStatusBar(Status.SUMMARY, numHits);
          } else {
            updateStatusBar(Status.ONE_HIT);
          }
        } else {
          if (query.length() != 0) {
            updateStatusBar(Status.NO_MATCHES);
          } else {
            updateStatusBar(Status.NO_SEARCH);
          }
        }
      }
    };
    this.searchField.getDocument().addDocumentListener(this.searchListModel);
    this.resultList = new JList(searchListModel);
    // JList cell prototype, causes horizontal scrollbar to always
    // show which is confusing
    //this.resultList.setPrototypeCellValue(LONGEST_WORD);
    this.searchListModel.setJList(this.resultList);
    this.resultList.addKeyListener(windowHider);
    this.resultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    this.resultList.setLayoutOrientation(JList.VERTICAL);

    // handle changes to selected list item (by mouse or arrows)
    this.resultList.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(final ListSelectionEvent evt) {
        if (resultList.isSelectionEmpty()) {
          return;
        }
        final int index = resultList.getSelectedIndex();
        final String lemma = searchListModel.getElementAt(index).toString();
        Word word = null;
        if (pos != POS.ALL) {
          word = browserPanel.dictionary().lookupWord(lemma, pos);
        } else {
          // do lookup for all POS and return first hit
          word = browserPanel.dictionary().lookupWord(lemma, POS.NOUN);
          if (word == null) {
            word = browserPanel.dictionary().lookupWord(lemma, POS.VERB);
          }
          if (word == null) {
            word = browserPanel.dictionary().lookupWord(lemma, POS.ADJ);
          }
          if (word == null) {
            word = browserPanel.dictionary().lookupWord(lemma, POS.ADV);
          }
        }
        if (word == null) {
          System.err.println("null Word for lemma: "+lemma);
          return;
        }
        SearchFrame.this.browserPanel.setWord(word);
      }
    });

    this.resultList.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(final FocusEvent evt) {
        if (resultList.isSelectionEmpty() && searchListModel.getSize() > 0) {
          resultList.setSelectedIndex(0);
        }
      }
    });

    // respond to double+ clicks when result list gains focus
    final MouseListener doubleClickListener = new MouseAdapter() {
      @Override
      public void mouseClicked(final MouseEvent evt) {
        if (evt.getClickCount() >= 2) {
          final int index = SearchFrame.this.resultList.locationToIndex(evt.getPoint());
          SearchFrame.this.resultList.clearSelection();
          SearchFrame.this.resultList.setSelectedIndex(index);
        }
      }
    };
    this.resultList.addMouseListener(doubleClickListener);

    // layout search results list
    final JScrollPane jsp = new  JScrollPane(resultList);
    jsp.setBorder(browser.textAreaBorder());
    jsp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, 0, false), "Slash");
    jsp.getActionMap().put("Slash", slashAction);
    jsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    //XXX considering violating the OS X HIG policy of always or never showing the
    // horizontal scrollbar
    //XXX jsp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    jsp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    this.add(jsp, BorderLayout.CENTER);

    this.statusLabel = new JLabel();
    this.statusLabel.setBorder(BorderFactory.createEmptyBorder(0 /*top*/, 3 /*left*/, 3 /*bottom*/, 0 /*right*/));
    this.add(this.statusLabel, BorderLayout.SOUTH);
    updateStatusBar(Status.NO_SEARCH);

    // set window size
    validate();
    final int height = 300;
    final int width = (int)(getPreferredSize().width * 1.1);
    this.setSize(Math.min(300, width), height);
    this.minSize = new Dimension(300, getMinimumSize().height);
    setMinimumSize(minSize);

    // enforce min dimensions
    addComponentListener(new ComponentAdapter() {
      @Override
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
  }

  private POS getSelectedPOS() {
    pos = CATS[posChoice.getSelectedIndex()];
    return pos;
  }
  
  JComponent searchPanel() {
    return searchPanel;
  }

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

  /**
   * Make 4+ multi-clicks behave like 3-clicks.
   */
  static void multiClickSelectAll(final JTextField searchField) {
    searchField.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(final MouseEvent evt) {
        //System.err.printf("evt.getClickCount(): %3d when: %,d ms\n", 
        //  evt.getClickCount(), evt.getWhen());
        //for (MouseListener ml : searchField.getMouseListeners()) {
        //  System.err.println("ml: "+ml+" "+ml.getClass());
        //}
        if (evt.getClickCount() > 3) {
          if (evt.getComponent() == searchField) {
            //System.err.println("multi-select");
            searchField.selectAll();
          }
        }
      }
    });
  }

  /**
   * Function object used to show status of user interaction as text at the bottom
   * of the main window.
   */
  private enum Status {
    NO_SEARCH("\u00A0"),
    ONE_HIT("1 result"),
    //SUMMARY("%,d results for %s as %s"),
    SUMMARY("%,d results"),
    NO_MATCHES("No matches found."),
    ;

    private final String formatString;

    private Status(final String formatString) {
      this.formatString = formatString;
    }

    String get(Object... args) {
      return String.format(formatString, args);
    }
  } // end enum Status

  private void updateStatusBar(final Status status, final Object... args) {
    this.statusLabel.setText(status.get(args));
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

  /**
   * Build SearchType constraints controls
   */
  private void addConstraintButtons(final JComponent constraintPanel) {
    final ButtonGroup group = new ButtonGroup();
    class SubstringAction extends AbstractAction {
      private static final long serialVersionUID = 1L;
      SubstringAction() {
        super("Substring");
      }
      public void actionPerformed(final ActionEvent evt) {
        searchType = SearchType.SUBSTRING;
        prefs.put("searchType", SearchType.SUBSTRING.name());
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
        prefs.put("searchType", SearchType.PREFIX.name());
        reissueSearch();
      }
    } // end class PrefixAction
    class GlossSubstringAction extends AbstractAction {
      private static final long serialVersionUID = 1L;
      GlossSubstringAction() {
        super("Gloss Substring");
      }
      public void actionPerformed(final ActionEvent evt) {
        searchType = SearchType.GLOSS_SUBSTRING;
        prefs.put("searchType", SearchType.GLOSS_SUBSTRING.name());
        reissueSearch();
      }
    } // end class GlossSubstringAction

    final Action substring = new SubstringAction();
    final Action prefix = new PrefixAction();
    final Action gloss = new GlossSubstringAction();

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
    final JRadioButton glossButton = new JRadioButton(gloss);
    glossButton.putClientProperty("JComponent.sizeVariant", "small");
    //XXX glossButton.setFont(prefixButton.getFont().deriveFont(prefixButton.getFont().getSize()-2f));
    glossButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
    glossButton.setFocusable(false);

    group.add(substringButton);
    group.add(prefixButton);
//    group.add(glossButton);

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

    c.gridx = 1;
    c.gridy = 2;
    c.ipady = 0;
    c.fill = java.awt.GridBagConstraints.NONE;
    c.gridwidth = GridBagConstraints.REMAINDER;
    c.anchor = java.awt.GridBagConstraints.CENTER;
    //c.anchor = java.awt.GridBagConstraints.WEST;
//    constraintPanel.add(glossButton, c);
    
    switch (searchType) {
      case SUBSTRING : substringButton.setSelected(true); break;
      case PREFIX : prefixButton.setSelected(true); break;
      case GLOSS_SUBSTRING : glossButton.setSelected(true); break;
      default : throw new IllegalStateException("Unsupported SearchType "+searchType);
    }
  }

  @Override
  public void setVisible(final boolean visible) {
    super.setVisible(visible);
    if (visible) {
      searchField.requestFocusInWindow();
    }
  }

  //XXX unused
  String cleanSearchField() {
    // " " is OK (SearchType.SUBSTRING search for collocations)
    // " a" is NOT OK (translate to "a")
    //return searchField.getText().trim();
    //FIXME return searchField.getText().replaceAll("\\s+", " ");
    return searchField.getText();
  }
}