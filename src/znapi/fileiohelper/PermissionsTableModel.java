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

import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

@SuppressWarnings("serial")
public class PermissionsTableModel extends AbstractTableModel {

	private final static String[] cNames = {"File", "Allow Reading", "Allow Writing"};
	private ArrayList<String> dirs;
	private ArrayList<Boolean[]> permissions;

	// lists that together contain a boolean for each cell
	// it is `true` if the cell is editable
	// i couldn't think of better names
	private ArrayList<Boolean> readColStates;
	private ArrayList<Boolean> writeColStates;

	public PermissionsTableModel() {
		dirs = new ArrayList<String>();
		permissions = new ArrayList<Boolean[]>();
		readColStates = new ArrayList<Boolean>();
		writeColStates = new ArrayList<Boolean>();
	}

	public int getRowCount() {
		return dirs.size();
	}

	public int getColumnCount() {
		return 3;
	}

	public String getColumnName(int c) {
		return cNames[c];
	}

	public Object getValueAt(int r, int c) {
		if(c == 0)
			return dirs.get(r);
		else if(c == 1)
			return permissions.get(r)[0];
		else
			return permissions.get(r)[1];
	}

	public Class<?> getColumnClass(int c) {
		if(c == 0) return String.class;
		else return Boolean.class;
	}

	public boolean isCellEditable(int r, int c) {
		if(c == 0)
			return false;
		else if(c == 1)
			return readColStates.get(r);
		else
			return writeColStates.get(r);
	}

	public void setValueAt(Object value, int r, int c) {
		if(c == 1)
			permissions.get(r)[0] = (boolean)value;
		else if(c == 2)
			permissions.get(r)[1] = (boolean)value;
		fireTableCellUpdated(r, c);
	}

	public void addReadPermission(String dir, boolean isAllowed) {
		int i = dirs.indexOf(dir);
		if(i == -1) {
			dirs.add(dir);
			permissions.add(new Boolean[]{isAllowed, false});

			readColStates.add(true);
			writeColStates.add(false);
		}
		else {
			permissions.get(i)[0] = isAllowed;

			readColStates.set(i, true);
		}
	}

	public void addWritePermission(String dir, boolean isAllowed) {
		int i = dirs.indexOf(dir);
		if(i == -1) {
			dirs.add(dir);
			permissions.add(new Boolean[]{false, isAllowed});

			readColStates.add(false);
			writeColStates.add(true);
		}
		else {
			Boolean[] p = permissions.get(i);
			p[1] = isAllowed;
			permissions.set(i, p);

			writeColStates.set(i, true);
		}
	}

}
