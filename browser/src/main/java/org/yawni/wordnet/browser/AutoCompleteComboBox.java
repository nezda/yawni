package org.yawni.wordnet.browser;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;

// adapted from http://www.java.happycodings.com/Java_Swing/code5.html
public class AutoCompleteComboBox extends JComboBox	implements JComboBox.KeySelectionManager {
	private String searchFor;
	private long lap;
	public class CBDocument extends PlainDocument {
    @Override
		public void insertString(int offset, String str, AttributeSet a) throws BadLocationException {
			if (str == null) {
        return;
      }
      super.insertString(offset, str, a);
      if (! isPopupVisible() && str.length() != 0) {
        fireActionEvent();
      }
    }
  } // end class CBDocument

  public AutoCompleteComboBox(Object[] items) {
    super(items);
    final ComboBoxModel aModel = getModel();
    setKeySelectionManager(this);
    // select item0 , but setArmed(false)
//    final JMenuItem item0 = (JMenuItem) getItemAt(0);
//    item0.setSelected(true);
//    item0.setArmed(false);

    lap = System.currentTimeMillis();
    
    if (getEditor() == null) {
      // e.g., non-editable ?
      System.err.println("non editable?");
      return;
    }
    final JTextField textField = (JTextField) getEditor().getEditorComponent();
    textField.setDocument(new CBDocument());
    
    addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        final String text = textField.getText();
        for (int i = 0; i < aModel.getSize(); i++) {
          final String current = aModel.getElementAt(i).toString();
          if (current.toLowerCase().startsWith(text.toLowerCase())) {
            textField.setText(current);
            textField.setSelectionStart(text.length());
            textField.setSelectionEnd(current.length());
            break;
          }
        }
      }
    });
  }
  /** {@inheritDoc} */
  public int selectionForKey(final char aKey, final ComboBoxModel aModel) {
    final long now = System.currentTimeMillis();
    if (searchFor != null && aKey == KeyEvent.VK_BACK_SPACE && searchFor.length() > 0) {
      assert false : "will never happen as only keyTyped() chars are sent and control chars are not issued; see KeyEvent";
      System.err.println("BS?: aKey: "+aKey);
      searchFor = searchFor.substring(0, searchFor.length() - 1);
    } else {
      System.err.println("aKey: "+aKey);
      // Never came here
      if (lap + 1000 < now) {
        searchFor = "" + aKey;
      } else {
        searchFor = searchFor + aKey;
      }
    }
    lap = now;
    String current;
    for (int i = 0; i < aModel.getSize(); i++) {
      current = aModel.getElementAt(i).toString().toLowerCase();
      if (current.startsWith(searchFor.toLowerCase())) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public void fireActionEvent() {
    super.fireActionEvent();
  }

  public static void main(String arg[]) {
    final JFrame frame = new JFrame("AutoCompleteComboBox");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(200, 300);
    final Container contentPane = frame.getContentPane();
    contentPane.setLayout(null);
    final String[] names = {"Beate", "Claudia", "Fjodor", "Fred", "Friedrich",	"Fritz", "Frodo", "Hermann", "Willi"};
    final JComboBox comboBox = new AutoCompleteComboBox(names);
//    final Locale[] locales = Locale.getAvailableLocales();
//    final JComboBox comboBox = new AutoCompleteComboBox(locales);
    comboBox.setBounds(50, 50, 100, 21);
    comboBox.setEditable(true);
    contentPane.add(comboBox);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }
}