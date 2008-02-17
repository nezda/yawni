/*
 * WordNet-Java
 *
 * Copyright 1998 by Oliver Steele.  You can use this software freely so long as you preserve
 * the copyright notice and this restriction, and label your changes.
 */

/* Fixes:
 * - change font of top line
 * - make button not stretch to fit
 * - remove scroll bars on text field
 */
//package edu.brandeis.cs.steele.wn.browser;
package browser;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

public class AboutDialog extends JDialog {
  protected static final String[] LABEL_TEXT = {
    "A graphical interface to the WordNet online lexical database.",
    "",
    "This Java version by Oliver Steele.",
    "",
    "The GUI is loosely based on the interface to the Tcl/Tk version by David Slomin."
  };

  public AboutDialog(Frame parent) {
    super(parent, true);
    setVisible(false);
    setTitle("About");
    //setResizable(false);
    //setSize(300,200);

    setLayout(new GridBagLayout());
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridx = 0;

    JLabel label1 = new JLabel("JWordNet Browser");
    add(label1, constraints);
    JTextArea label2 = makeTextArea(LABEL_TEXT);
    add(label2, constraints);
    JButton okButton = new JButton("OK");
    add(okButton, constraints);

    okButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        setVisible(false);
        dispose();
      }
    });

    Rectangle bounds = getParent().getBounds();
    Rectangle abounds = getBounds();
    setLocation(bounds.x + (bounds.width - abounds.width) / 2,
        bounds.y + (bounds.height - abounds.height) / 2);
    // the * 1.2 is a quick workaround 
    setSize((int)(getPreferredSize().width * 1.2), (int)(getPreferredSize().height * 1.3));
    setVisible(true);
  }

  protected JTextArea makeTextArea(String[] paragraphs) {
    JTextArea area = new JTextArea();
    //area.(TextArea.SCROLLBARS_NONE)
    area.setEditable(false);
    for (int i = 0; i < paragraphs.length; ++i) {
      if (i > 0) area.append("\n");
      area.append(paragraphs[i]);
    }
    return area;
  }
}
