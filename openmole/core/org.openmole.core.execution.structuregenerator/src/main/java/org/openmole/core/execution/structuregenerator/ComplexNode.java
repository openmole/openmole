/*
 *
 *  Copyright (c) 2008, Cemagref
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
package org.openmole.core.execution.structuregenerator;

import org.openmole.commons.exception.InternalProcessingError;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.openmole.core.workflow.implementation.data.Prototype;

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

    public <T> PrototypeNode<T> addPrototype(String name, Class<? extends T> type) {
        PrototypeNode<T> node = new PrototypeNode(new Prototype(new String(name), type));
        add(node);
        return node;
    }

    public <T> SequenceNode<PrototypeNode<T>> addPrototypeSequence(String name, Class<? extends T> type) {
        SequenceNode<PrototypeNode<T>> node = new SequenceNode<PrototypeNode<T>>(new PrototypeNode(new Prototype(new String(name), type)));
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
        getChildrenContent().put(element.getName(), element);
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
        return childrenContent.get(index).getName();
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
        remove(element.getName());
    }

    public void remove(String name) {
        getChildrenContent().remove(name);
    }

    public int size() {
        return childrenContent.size();
    }

    @Override
    public String getName() {
        return name;
    }

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
