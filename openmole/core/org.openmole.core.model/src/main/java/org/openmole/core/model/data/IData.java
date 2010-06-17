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
     * Get the mode of the data
     *
     * @return mode of the data
     */
    IDataMode getMode();


    /**
     *
     * Get the prototype of the data.
     *
     * @return the prototype of the data
     */
    IPrototype<T> getPrototype();
}
