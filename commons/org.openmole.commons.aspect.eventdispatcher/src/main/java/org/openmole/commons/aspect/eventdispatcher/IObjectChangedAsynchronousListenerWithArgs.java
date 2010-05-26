/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.commons.aspect.eventdispatcher;

/**
 *
 * @author reuillon
 */
public interface IObjectChangedAsynchronousListenerWithArgs<T> extends IObjectChangedListener<T> {
      void objectChanged(T obj, Object[] args);
}
