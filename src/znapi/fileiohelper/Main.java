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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Hashtable;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public final class Main extends NanoHTTPD implements TableModelListener, ItemListener {

	private String rootDir;
	private boolean useConsole;
	private static final int port = 8080;

	public boolean requirePermissions;
	public Hashtable<String, Boolean> readPermissions;
	public Hashtable<String, Boolean> writePermissions;

	private PermissionsTableModel permissionsTableModel;

	private JFrame frame;
	private JCheckBox permissionsCheckBox;
	private JTable permissionsTable;
	private JTextArea consoleArea;
	private PlainDocument console;

	private static final String usageMsg = "Usage:\n\tjava -jar file-io-helper-app.jar [-gui] /root/directory\n\n\t  -gui         Optional argument. Runs the app in GUI mode.\n\t  -nosecurity  Only works with the -gui flag. Runs without\n\t               requiring permissions by default.";

	public static void main(String[] args) {
		String rootDir = null;
		boolean useGui, useSecurity = true;

		if(System.console() != null)
			useGui = false;
		else
			useGui = true;

		for(String arg : args) {
			switch(arg) {
			case "-gui":
				useGui = true; break;
			case "-nosecurity":
				useSecurity = false; break;
			default:
				if(arg.charAt(0) == '-') {
					System.out.println("Unrecognized argument: " + arg);
					System.out.println(usageMsg);
					return;
				}
				else
					rootDir = arg; break;
			}
		}

		if(System.console() != null && rootDir == null) {
			System.out.println("You must specify a root directory.\nExample:\n\tjava -jar file-io-helper-app.jar /directory/you/want/as/root/\n"+usageMsg);
			return;
		}

		new Main(rootDir, !useGui, useSecurity);
	}

	public void log(String s) {
		if(useConsole)
			System.out.println(s);
		else {
			try {
				console.insertString(console.getLength(), s + '\n', null);
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
		}
	}

	public Main(String rootDir, boolean useConsole, boolean useSecurity) {
		super(port);
		this.useConsole = useConsole;
		if(useConsole) this.requirePermissions = false;
		else this.requirePermissions = useSecurity;

		readPermissions = new Hashtable<String, Boolean>();
		writePermissions = new Hashtable<String, Boolean>();

		if(useConsole)
			this.rootDir = rootDir;
		else {
			frame = new JFrame("ScratchX File I/O Helper App");

			// create space to write log messages to
			console = new PlainDocument();
			consoleArea = new JTextArea(console);
			consoleArea.setFont(Font.decode(Font.MONOSPACED));
			consoleArea.setEditable(false);
			JScrollPane consoleScrollPane = new JScrollPane(consoleArea);

			// create table of file permissions
			permissionsCheckBox = new JCheckBox("Require permissions");
			permissionsCheckBox.setSelected(useSecurity);
			permissionsCheckBox.addItemListener(this);
			permissionsTableModel = new PermissionsTableModel();
			permissionsTableModel.addTableModelListener(this);
			permissionsTable = new JTable(permissionsTableModel);
			permissionsTable.setFillsViewportHeight(true);
			JScrollPane permissionsScrollPane = new JScrollPane(permissionsTable);
			permissionsScrollPane.setMinimumSize(new Dimension(0, 0));
			permissionsTable.setMinimumSize(new Dimension(0, 0));

			// put log space and table in panel
			SpringLayout layout = new SpringLayout();
			JPanel panel = new JPanel(layout);
			JLabel permissionsLabel = new JLabel("Permissions table:");
			JLabel consoleLabel = new JLabel("Console:");
			panel.add(permissionsLabel);
			panel.add(permissionsCheckBox);
			panel.add(permissionsScrollPane);
			panel.add(consoleLabel);
			panel.add(consoleScrollPane);

			layout.putConstraint(SpringLayout.NORTH, permissionsLabel, 2, SpringLayout.NORTH, panel);
			layout.putConstraint(SpringLayout.WEST, permissionsLabel, 5, SpringLayout.WEST, panel);

			layout.putConstraint(SpringLayout.NORTH, permissionsCheckBox, 0, SpringLayout.SOUTH, permissionsLabel);
			layout.putConstraint(SpringLayout.WEST, permissionsCheckBox, 0, SpringLayout.WEST, panel);

			layout.putConstraint(SpringLayout.NORTH, permissionsScrollPane, 5, SpringLayout.SOUTH, permissionsCheckBox);
			layout.putConstraint(SpringLayout.WEST, permissionsScrollPane, 1, SpringLayout.WEST, panel);
			layout.putConstraint(SpringLayout.EAST, permissionsScrollPane, -1, SpringLayout.EAST, panel);

			layout.putConstraint(SpringLayout.NORTH, consoleLabel, 5, SpringLayout.SOUTH, permissionsScrollPane);
			layout.putConstraint(SpringLayout.WEST, consoleLabel, 5, SpringLayout.WEST, panel);

			layout.putConstraint(SpringLayout.NORTH, consoleScrollPane, 5, SpringLayout.SOUTH, consoleLabel);
			layout.putConstraint(SpringLayout.WEST, consoleScrollPane, 1, SpringLayout.WEST, panel);
			layout.putConstraint(SpringLayout.EAST, consoleScrollPane, -1, SpringLayout.EAST, panel);
			layout.putConstraint(SpringLayout.SOUTH, consoleScrollPane, -1, SpringLayout.SOUTH, panel);

			// make window/frame
			frame.setContentPane(panel);
			frame.pack();
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setSize(500, 600);
			frame.setVisible(true);

			if(rootDir == null) {
				log("Please select a directory to use as root");
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setDialogTitle("Select a directory to use as root");
				fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

				switch(fileChooser.showOpenDialog(frame)) {
				case JFileChooser.APPROVE_OPTION:
					this.rootDir = fileChooser.getSelectedFile().getAbsolutePath();
					break;
				case JFileChooser.CANCEL_OPTION:
					log("No directory selected.\nStopped. You may close the window to exit.");
					return;
				}

			}
			else
				this.rootDir = rootDir;
		}

		log("Starting server with root at `" + this.rootDir + "` on port " + port + "\n");
		try {
			this.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
		} catch (IOException ioe) {
			log("Couldn't start server:\n" + ioe + "\nStopped. You may close the window to exit");
			return;
		}

		if(useConsole) {
			System.out.println("Server started. Hit Enter to stop.\n");

			try {
				System.in.read();
			} catch (Throwable ignored) {
			}

			this.stop();
			System.out.println("Stopped.\n");
		}
		else
			log("Server started. Close the window to exit.");
	}

	public void checkRead(String dir) {
		if(requirePermissions) {
			if(readPermissions.containsKey(dir)) {
				if(!readPermissions.get(dir))
					throw new SecurityException();
			}
			else {
				boolean allow = PermissionDialog.askPermission(frame, dir, true);
				readPermissions.put(dir, allow);
				permissionsTableModel.addReadPermission(dir, allow);
				if(!allow)
					throw new SecurityException();
			}
		}
	}

	public void checkWrite(String dir) {
		if(requirePermissions) {
			if(writePermissions.containsKey(dir)) {
				if(!writePermissions.get(dir))
					throw new SecurityException();
			}
			else {
				boolean allow = PermissionDialog.askPermission(frame, dir, false);
				writePermissions.put(dir, allow);
				permissionsTableModel.addWritePermission(dir, allow);
				if(!allow)
					throw new SecurityException();
			}
		}
	}

	public Status createFile(File f, String dir) {
		log("Creating file: " + f.getName());
		try {
			checkWrite(dir);
			f.getParentFile().mkdirs();
			f.createNewFile();
			return Status.OK;
		} catch(SecurityException e) {
			log("Unauthorized");
			return Status.UNAUTHORIZED;
		} catch(FileNotFoundException e) {
			log("ERROR: FILE DOES NOT EXIST"); // the file was just created
			return Status.INTERNAL_ERROR;
		} catch (IOException e) {
			log("I/O Error");
			return Status.INTERNAL_ERROR;
		}
	}

	public Status writeFile(File f, String dir, String contents, boolean isAppend) {
		log("Writing file: " + f.getName());
		try {
			checkWrite(dir);
			PrintWriter writer;
			if(!isAppend) {
				writer = new PrintWriter(f);
				writer.print(contents);
			}
			else {
				writer = new PrintWriter(new FileOutputStream(f, true));
				writer.println(contents);
			}
			writer.close();
			return Status.OK;
		} catch (SecurityException e) {
			log("Unauthorized");
			return Status.UNAUTHORIZED;
		} catch (FileNotFoundException e) {
			log("ERROR: FILE DOES NOT EXIST"); // the caller ensures that the file exists
			return Status.INTERNAL_ERROR;
		}
	}

	@Override
	public Response serve(IHTTPSession session) {
		Response r = newFixedLengthResponse(null);
		r.addHeader("Access-Control-Allow-Origin", "*");
		r.addHeader("Access-Control-Expose-Headers", "X-Is-ScratchX-File-IO-Helper-App");
		r.addHeader("X-Is-ScratchX-File-IO-Helper-App", "yes");
		r.addHeader("Access-Control-Allow-Headers", "X-Action");

		File f;
		String uri = session.getUri();
		switch(session.getMethod()) {

		case OPTIONS:
			r.setStatus(Response.Status.OK);
			r.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS");
			break;

		// send the contents of the specified file to the Scratch extension
		case GET:
			log("GET " + uri);
			f = new File(rootDir + uri);
			if(f.exists()) {
				try { // this could not be abstracted into another method because no pass by reference and only one return type
					checkRead(uri);
					log("Reading file: " + uri);
					FileInputStream s = new FileInputStream(f);
					r.setStatus(Status.OK);
					r.setData(s);
				} catch(SecurityException e) {
					log("Unauthorized");
					r.setStatus(Status.UNAUTHORIZED);
				} catch (FileNotFoundException e) {
					log("File not found");
					r.setStatus(Status.NOT_FOUND);
				}
			}
			else
				r.setStatus(createFile(f, uri));
			break;

		// create/empty the specified file
		case POST:
			log("POST " + uri);
			f = new File(rootDir+uri);
			if(f.exists())
				r.setStatus(writeFile(f, uri, "", false));
			else
				r.setStatus(createFile(f, uri));
			break;

		// set contents of file
		case PUT:
			boolean isAppend = "append".equalsIgnoreCase(session.getHeaders().get("X-Action".toLowerCase()));
			if(isAppend)
				log("PUT append " + session.getUri());
			else 
				log("PUT " + session.getUri());

			f = new File(rootDir+session.getUri());
			if(!f.exists()) {
				r.setStatus(createFile(f, uri));
				if(r.getStatus() != Status.OK) break;
			}

			int contentLength = Integer.parseInt(session.getHeaders().get("content-length"));
			byte[] buffer = new byte[contentLength];
			try {
				session.getInputStream().read(buffer, 0, contentLength);
				r.setStatus(writeFile(f, uri, new String(buffer), isAppend));
			} catch (IOException e) {
				log("I/O error");
				r.setStatus(Status.INTERNAL_ERROR);
			}
			break;

		default:
			r.setStatus(Response.Status.METHOD_NOT_ALLOWED);
			break;

		}
		return r;
	}

	public JFrame getFrame() {
		return frame;
	}

	// changes a hashtable in event that the user changed the table
	public void tableChanged(TableModelEvent e) {
		int r = e.getFirstRow();
		boolean isReadPerm = e.getColumn() == 1;

		String dir = (String)permissionsTable.getValueAt(r, 0);
		boolean newValue;
		if(isReadPerm) {
			newValue = (boolean)permissionsTable.getValueAt(r, 1);
			readPermissions.replace(dir, newValue);
		}
		else {
			newValue = (boolean)permissionsTable.getValueAt(r, 2);
			writePermissions.replace(dir, newValue);
		}
	}

	public void itemStateChanged(ItemEvent e) {
		// don't bother checking the source of the event, since it can only be from the permissionsCheckBox
		this.requirePermissions = (e.getStateChange() == ItemEvent.SELECTED);
	}

}
