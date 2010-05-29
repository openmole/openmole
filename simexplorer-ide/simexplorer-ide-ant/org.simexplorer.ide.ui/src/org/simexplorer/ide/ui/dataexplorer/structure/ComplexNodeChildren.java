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
package org.simexplorer.ide.ui.dataexplorer.structure;

import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openmole.core.structuregenerator.ComplexNode;
import org.openmole.core.structuregenerator.StructureNode;

class ComplexNodeChildren extends Children.Keys<StructureNode> {

    private ComplexNode inputStructure;

    public ComplexNodeChildren(ComplexNode inputStructure) {
        setKeys(inputStructure.getChildrenContent().values());
        this.inputStructure = inputStructure;
    }

    public ComplexNode getInputStructure() {
        return inputStructure;
    }

    @Override
    protected Node[] createNodes(StructureNode node) {
        return new Node[]{StructureNodeNode.createNode(node, this)};
    }

    void removeNode(StructureNode node) {
        inputStructure.remove(node);
        // TODO remove the corresponding node instead of forcing reload
        setKeys(inputStructure.getChildrenContent().values());
    }
}
