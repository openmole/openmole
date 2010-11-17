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
public class PrototypeNode implements StructureNode {

    Prototype prototype;

    public PrototypeNode(Prototype prototype) {
        this.prototype = prototype;
    }

    @Override
    public String name() {
        return prototype.name();
    }

    public Prototype prototype() {
        return prototype;
    }

    @Override
    public void setName(String name) {
        prototype = new Prototype(name, prototype.type());
    }
   
    public void setType(Class type) {
        prototype = new Prototype(prototype.name(), type);
    }
}
