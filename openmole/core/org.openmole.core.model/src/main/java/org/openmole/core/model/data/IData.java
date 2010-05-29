/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.core.model.data;

/**
 *
 * {@link IData} modelizes data atomic elements of data-flows.
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public interface IData<T> {

    /**
     *
     * Get if the data is optional or mandatory.
     *
     * @return true if the data is optional
     */
    boolean isOptional();

    /**
     *
     * Get the prototype of this data.
     *
     * @return the prototype of this data
     */
    IPrototype<T> getPrototype();
}
