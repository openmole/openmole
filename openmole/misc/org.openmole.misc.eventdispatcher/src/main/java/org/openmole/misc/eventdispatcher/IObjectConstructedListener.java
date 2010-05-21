/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.misc.eventdispatcher;


/**
 *
 * @author reuillon
 */
public interface IObjectConstructedListener<T> extends IObjectListener{
    void objectConstructed(T obj);
}
