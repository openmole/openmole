/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.core.model.data;

import java.util.Collection;

/**
 *
 * {@link IPrototype} is a prototype in the sens of C language prototypes. It is composed of a type and a name.
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public interface IPrototype<T> {

    /**
     *
     * Get the name of the prototype.
     *
     * @return the name of the prototype
     */
    String getName();

    /**
     *
     * Get the type of the prototype.
     *
     * @return the type of the prototype
     */
    Class<? extends T> getType();

    /**
     *
     * Test if this prototype can be assingned from another prototype, in the sens of Class.isAssignableFrom().
     *
     * @param prototype the prototype to test
     * @return true if the prototype is assignable from the given prototype.
     */
    boolean isAssignableFrom(IPrototype<?> prototype);

    /**
     *
     * Return the array version of this prototype.
     *
     * @return the array version of this prototype
     */
    IPrototype<Collection<T>> array();
}
