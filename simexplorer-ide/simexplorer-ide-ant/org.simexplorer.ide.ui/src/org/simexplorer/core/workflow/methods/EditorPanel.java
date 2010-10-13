/*
 *
 *  Copyright (c) 2007 - 2009, Cemagref
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License as
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
package org.simexplorer.core.workflow.methods;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.UndoableEditListener;

public abstract class EditorPanel<T> extends JPanel {

    private T method;
    private Class<? extends T>[] typesEditable;

    public EditorPanel(Class<? extends T>... typesEditable) {
        this.typesEditable = typesEditable;
    }

    public void applyChanges() {
    }

    public Class<? extends T>[] getTypesEditable() {
        return typesEditable;
    }

    public T getObjectEdited() {
        return method;
    }

    public JComponent getEditor() {
        return this;
    }

    public void setObjectEdited(T method) {
        this.method = method;
    }

    /**
     * To process validation of the panel
     * @return null if panel isValid otherwise a string with the validation error message
     */
    public String isInputValid() {
        return null;
    }

    /**
     * Override this method to be aware of undo/redo
     * @return
     */
    public void addUndoableEditListener(UndoableEditListener listener) {
    }
}
