/*
 *  Copyright © 2008, 2009, Cemagref
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
package org.simexplorer.ui.ide.structure;

import javax.swing.Action;
import org.openide.util.Utilities;
import org.openmole.core.structuregenerator.ComplexNode;

class ComplexNodeNode extends StructureNodeNode<ComplexNode> {

    public ComplexNodeNode(ComplexNode node, ComplexNodeChildren parent) {
        super(new ComplexNodeChildren(node), node, parent);
    }

    @Override
    public Action[] getActions(boolean context) {
        if (Utilities.actionsGlobalContext().lookup(ComplexNode.class) != null) {
            if (parent == null) {
                // It means that remove action should be disabled (root)
                return new Action[]{new NewAction()};
            } else {
                return new Action[]{new NewAction(), new EditAction(), new DeleteAction()
                     /*   , SystemAction.get( PasteAction.class )*/};
            }
        } else {
            // the actions has been requested on the empty area in the nodes view without any node selected
            return new Action[]{new ImportAction()};
        }
    }
}
