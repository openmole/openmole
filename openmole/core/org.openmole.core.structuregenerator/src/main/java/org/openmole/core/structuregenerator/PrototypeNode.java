/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.core.structuregenerator;

import org.openmole.core.implementation.data.Prototype;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class PrototypeNode<T> implements StructureNode {

    Prototype<T> prototype;

    public PrototypeNode(Prototype<T> prototype) {
        this.prototype = prototype;
    }

    @Override
    public String getName() {
        return prototype.getName();
    }

    public Prototype<T> getPrototype() {
        return prototype;
    }

    @Override
    public void setName(String name) {
        prototype = new Prototype<T>(prototype, name);
    }

    public void setType(Class<? extends T> type) {
        prototype = new Prototype<T>(prototype, type);
    }

}
