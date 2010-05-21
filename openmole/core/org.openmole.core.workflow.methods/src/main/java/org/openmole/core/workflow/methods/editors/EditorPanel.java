/*
 *
 *  Copyright (c) 2007, Cemagref
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation; either version 3 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston,
 *  MA  02110-1301  USA
 */
package org.openmole.core.workflow.methods.editors;

import javax.swing.JComponent;
import javax.swing.JPanel;

public abstract class EditorPanel<T> extends JPanel {

    private T method;

    public void applyChanges() {}

    public T getObjectEdited() {
        return method;
    }

    public JComponent getEditor() {
        return this;
    }

    public void setObjectEdited(T method) {
        this.method = method;
    }
}
