/*
 *  Copyright © 2009, Cemagref
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

/**
 * An extension of EditorPanel for a more complex type than the parametrized type
 * @author thierry
 */
public abstract class DomainEditorPanel<T> extends EditorPanel<T> {

    public DomainEditorPanel(Class<? extends T>... typesEditable) {
        super(typesEditable);
    }

    /**
     * Type of the edited object
     * @return the type of the edited object
     */
    public abstract Class getType();
}
