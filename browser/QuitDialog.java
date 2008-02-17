/*
 * WordNet-Java
 *
 * Copyright 1998 by Oliver Steele.  You can use this software freely so long as you preserve
 * the copyright notice and this restriction, and label your changes.
 */
//package edu.brandeis.cs.steele.wn.browser;
package browser;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;

public class QuitDialog extends Dialog {
	Label label1;
	Button yesButton;
	Button noButton;

	public QuitDialog(Frame parent, boolean modal) {
		super(parent, modal);
		setTitle("Quit");
		setResizable(false);

		setLayout(null);
		setSize(insets().left + insets().right + 337,insets().top + insets().bottom + 135);
		
		label1 = new Label("Do you really want to quit?",Label.CENTER);
		label1.setBounds(78,33,180,23);
		add(label1);
		yesButton = new Button(" Yes ");
		yesButton.setBounds(insets().left + 72,insets().top + 80,79,22);
		yesButton.setFont(new Font("Dialog", Font.BOLD, 12));
		add(yesButton);
		noButton = new Button("No");
		noButton.setBounds(insets().left + 185,insets().top + 80,79,22);
		noButton.setFont(new Font("Dialog", Font.BOLD, 12));
		add(noButton);

		/*addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent event)
			{
				Object object = event.getSource();
				if (object == QuitDialog.this)
					QuitDialog_WindowClosing(event);
			}
		});*/
		noButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
			    dispose();
			}
		});
		yesButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(new WindowEvent((Window)getParent(), WindowEvent.WINDOW_CLOSING));
			}
		});
	}

	public QuitDialog(Frame parent, String title, boolean modal) {
		this(parent, modal);
		setTitle(title);
	}

    public void setVisible(boolean b) {
		if(b) {
			Rectangle bounds = getParent().getBounds();
			Rectangle abounds = getBounds();
	
			setLocation(bounds.x + (bounds.width - abounds.width)/ 2,
				 bounds.y + (bounds.height - abounds.height)/2);
		}
		super.setVisible(b);
	}
}

