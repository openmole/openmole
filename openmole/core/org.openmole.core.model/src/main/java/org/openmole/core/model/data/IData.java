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

 
    IDataMod getMod();


    /**
     *
     * Get the prototype of this data.
     *
     * @return the prototype of this data
     */
    IPrototype<T> getPrototype();
}
