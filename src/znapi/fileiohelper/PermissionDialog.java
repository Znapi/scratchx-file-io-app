package znapi.fileiohelper;

/*
	Helper App for File I/O Scratch extension
	Copyright (C) 2016  Zachary Napier

	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

import java.awt.Button;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;

@SuppressWarnings("serial")
public final class PermissionDialog extends JDialog implements ActionListener {

	private Button allowButton;

	private boolean isAllowed;

	public static boolean askPermission(JFrame frame, String dir, boolean isAskingRead) {
		PermissionDialog d = new PermissionDialog(frame, dir, isAskingRead);
		try {
			synchronized(d) {
				d.wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			d.dispose();
			return false;
		}
		d.dispose();
		return d.isAllowed;
	}

	private PermissionDialog(JFrame frame, String dir, boolean isAskingRead) {
		super(frame);
		if(isAskingRead)
			this.setTitle("New Read Permission");
		else
			this.setTitle("New Write Permission");

		this.setAutoRequestFocus(true);
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		allowButton = new Button("Allow");
		Button denyButton = new Button("Don't allow");
		allowButton.addActionListener(this);
		denyButton.addActionListener(this);

		JTextArea message = new JTextArea();
		message.setEditable(false);
		message.setBackground(frame.getBackground());
		if(isAskingRead)
			message.setText("The Scratch project wants to read the file " + dir);
		else
			message.setText("The Scratch project wants to write the file " + dir);

		JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
		buttonPanel.add(allowButton);
		buttonPanel.add(denyButton);
		JPanel p = new JPanel(new GridLayout(2, 1));
		p.add(message);
		p.add(buttonPanel);

		this.setContentPane(p);
		this.pack();
		this.setVisible(true);
		this.requestFocus();
	}

	// called when the ok button is pressed
	public void actionPerformed(ActionEvent e) {
		this.setVisible(false);

		if(e.getSource() == allowButton)
			isAllowed = true;
		else
			isAllowed = false;

		synchronized(this) {
			this.notifyAll();
		}
	}

}
