/*
 *
 *  Copyright (c) 2008, Cemagref
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation; either version 3 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston,
 *  MA  02110-1301  USA
 */
package org.openmole.core.structuregenerator;

import org.openmole.misc.exception.InternalProcessingError;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.openmole.core.implementation.data.Prototype;

public class ComplexNode implements StructureNode, Iterable<StructureNode> {

    private String name;
    private Map<String, StructureNode> childrenContent;
    private ComplexNode parent;

    public ComplexNode() {
        this("Root");
    }

    public ComplexNode(String name) {
        super();
        this.name = name;
        childrenContent = new HashMap<String, StructureNode>();
    }

    public ComplexNode(String name, ComplexNode parent) {
        this(name);
        setParent(parent);
    }

    public PrototypeNode addPrototype(String name, Class type) {
        PrototypeNode node = new PrototypeNode(new Prototype(new String(name), type));
        add(node);
        return node;
    }

    public SequenceNode<PrototypeNode> addPrototypeSequence(String name, Class type) {
        SequenceNode<PrototypeNode> node = new SequenceNode<PrototypeNode>(new PrototypeNode(new Prototype(new String(name), type)));
        add(node);
        return node;
    }

    public ComplexNode addComplexNode(String name) {
        ComplexNode node = new ComplexNode(new String(name));
        add(node);
        return node;
    }

    public ComplexNode addComplexNodeSequence(String name) {
        ComplexNode node = new ComplexNode(new String(name));
        this.add(new SequenceNode<ComplexNode>(node));
        return node;
    }

    public void add(StructureNode element) {
        getChildrenContent().put(element.name(), element);
    }

    public StructureNode get(String name) throws InternalProcessingError {
        return getChildrenContent().get(name);
    }

    public StructureNode get(int index) {
        return childrenContent.get(index);
    }

    /**
     * Gives the name of the element at a specified index.
     * @param index
     * @return
     */
    public String getName(int index) {
        return childrenContent.get(index).name();
    }

    public void clear() {
        childrenContent.clear();
    }

    public Map<String, StructureNode> getChildrenContent() {
        return childrenContent;
    }

    @Override
    public Iterator<StructureNode> iterator() {
        return getChildrenContent().values().iterator();
    }

    public void remove(int index) {
        childrenContent.remove(index);
    }

    public void remove(StructureNode element) {
        remove(element.name());
    }

    public void remove(String name) {
        getChildrenContent().remove(name);
    }

    public int size() {
        return childrenContent.size();
    }

    @Override
    public String name() {
        return name;
    }
    
    @Override
    public void setName(String name) {
        this.name = name;
    }

    public ComplexNode getParent() {
        return parent;
    }

    public void setParent(ComplexNode parent) {
        this.parent = parent;
    }

}
