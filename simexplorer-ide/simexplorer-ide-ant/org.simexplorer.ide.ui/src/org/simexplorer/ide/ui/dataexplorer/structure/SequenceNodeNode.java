/*
 *
 *  Copyright (c) 2009, Cemagref
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
package org.simexplorer.ide.ui.dataexplorer.structure;

import javax.swing.Action;
import org.openide.nodes.Children;
import org.openmole.core.structuregenerator.ComplexNode;
import org.openmole.core.structuregenerator.PrototypeNode;
import org.openmole.core.structuregenerator.SequenceNode;
import org.openmole.core.workflow.implementation.data.Prototype;

public class SequenceNodeNode extends StructureNodeNode<SequenceNode> {

    public SequenceNodeNode(SequenceNode sequenceNode, ComplexNodeChildren parent) {
        super(sequenceNode.getInnerNode() instanceof PrototypeNode ? Children.LEAF : new ComplexNodeChildren((ComplexNode) sequenceNode.getInnerNode()),
                sequenceNode, sequenceNode, parent);
    }

    @Override
    public Action[] getActions(boolean context) {
        if (nodeValue.getInnerNode() instanceof Prototype) {
            return new Action[]{new EditAction(), new DeleteAction()};
        } else if (nodeValue.getInnerNode() instanceof ComplexNode) {
            return new Action[]{new NewAction(), new EditAction(), new DeleteAction()};
        } else {
            assert nodeValue.getInnerNode() instanceof SequenceNode;
            return new Action[]{new EditAction(), new DeleteAction()};
        }
    }
}
