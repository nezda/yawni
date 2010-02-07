package org.yawni.wordnet.browser;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * JDialog doesn't honor apple.awt.brushMetalLook programmatically
 * via client property or via JVM init System property.
 * All Snow Leopard Java 6 versions have had this issue;
 * Also reproducible on 10M3013 32-bit or 64-bit.
 * Minor adaptation of Thomas Singer's bug 7312264 example.
 * Filed as 7428246.
 */
public class ActivationTest extends JFrame {
  // Static =================================================================
  public static void main(String[] args) throws Exception {
    EventQueue.invokeLater(new Runnable() {
      public void run() {
        new ActivationTest().setVisible(true);
      }
    });
  }
  // Fields =================================================================
  private JButton dialogButton;
  private JButton helpButton;
  private JLabel stateLabel;

  // Setup ==================================================================
  /**
   * Creates new form ActivationTest
   */
  public ActivationTest() {
    getRootPane().putClientProperty("apple.awt.brushMetalLook", true);
    initComponents();
    ((JComponent) getContentPane()).setBorder(new EmptyBorder(18, 20, 22, 20));
    helpButton.putClientProperty("JButton.buttonType", "help");

    // Display the active state of the window in a JLabel
    stateLabel.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {
        if ("Frame.active".equals(evt.getPropertyName())) {
          stateLabel.setText((Boolean) evt.getNewValue() ? "Active" : "Inactive");
        }
      }
    });

    setSize(400, 300);
  }

  // Utils ==================================================================
  private void initComponents() {
    GridBagConstraints gridBagConstraints;

    stateLabel = new JLabel();
    helpButton = new JButton();
    dialogButton = new JButton();

    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    getContentPane().setLayout(new GridBagLayout());

    stateLabel.setHorizontalAlignment(SwingConstants.CENTER);
    stateLabel.setText("Active");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    getContentPane().add(stateLabel, gridBagConstraints);
    getContentPane().add(helpButton, new GridBagConstraints());

    dialogButton.setText("Show JDialog");
    dialogButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        if (evt.getSource() == dialogButton) {
          //JOptionPane.showConfirmDialog(ActivationTest.this, "foo");
          final JDialog dialog = new JDialog(ActivationTest.this);
          dialog.getRootPane().putClientProperty("apple.awt.brushMetalLook", true);
          ((JComponent) dialog.getContentPane()).setBorder(new EmptyBorder(20, 20, 20, 20));
          dialog.add(new JLabel("Sidekick Dialog"));
          dialog.pack();
          dialog.setVisible(true);
          dialog.setLocationRelativeTo(ActivationTest.this);
          dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        }
      }
    });
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.anchor = GridBagConstraints.EAST;
    getContentPane().add(dialogButton, gridBagConstraints);

    pack();
  }
}