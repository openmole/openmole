/*
 *  Copyright (C) 2010 Romain Reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.structuregenerator;

/**
 *
 * @author reuillon
 */
public class SequenceNode<T extends StructureNode> implements StructureNode {
    
    private T innerNode;

    private SequenceNode(){}
    
    public SequenceNode(T innerNode) {
        super();
        this.innerNode = innerNode;
    }

    public T getInnerNode() {
        return innerNode;
    }

    @Override
    public String name() {
        return innerNode.name();
    }

    @Override
    public void setName(String name) {
        innerNode.setName(name);
    }

}
