/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.misc.eventdispatcher;

/**
 *
 * @author reuillon
 */
public interface IObjectConstructedAsynchronousListener<T> extends IObjectConstructedListener<T> {
    @Override
    void objectConstructed(T obj);
}
