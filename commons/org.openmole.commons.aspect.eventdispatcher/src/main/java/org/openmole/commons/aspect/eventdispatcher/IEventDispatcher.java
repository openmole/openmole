/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.commons.aspect.eventdispatcher;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
/**
 *
 * @author reuillon
 */
public interface IEventDispatcher {

    <T> void registerListener(T object, IObjectChangedAsynchronousListener<? super T> listner);
    <T> void registerListener(T object, Integer priority, IObjectChangedSynchronousListener<? super T> listner);
    <T> void registerListener(T object, IObjectChangedAsynchronousListener<? super T> listner, String type);
    <T> void registerListener(T object, Integer priority, IObjectChangedSynchronousListener<? super T> listner, String type);

    <T> void registerListener(T object, IObjectChangedAsynchronousListenerWithArgs<? super T> listner);
    <T> void registerListener(T object, Integer priority, IObjectChangedSynchronousListenerWithArgs<? super T> listner);
    <T> void registerListener(T object, IObjectChangedAsynchronousListenerWithArgs<? super T> listner, String type);
    <T> void registerListener(T object, Integer priority, IObjectChangedSynchronousListenerWithArgs<? super T> listner, String type);

    <T> void registerListener(Class<T> c, Integer priority, IObjectConstructedSynchronousListener<? super T> listner);
    <T> void registerListener(Class<T> c, IObjectConstructedAsynchronousListener<? super T> listner);
    
    void objectChanged(Object object, String type) throws InternalProcessingError, UserBadDataError;
    void objectChanged(Object object, String type, Object[] args) throws InternalProcessingError, UserBadDataError;
    void objectConstructed(Object object);

    <T> boolean isRegistred(T object, IObjectChangedAsynchronousListener<? super T> listner);
    <T> boolean isRegistred(T object, IObjectChangedAsynchronousListener<? super T> listner, String type);
    <T> boolean isRegistred(T object, IObjectChangedSynchronousListener<? super T> listner);
    <T> boolean isRegistred(T object, IObjectChangedSynchronousListener<? super T> listner, String type);

    <T> boolean isRegistred(T object, IObjectChangedAsynchronousListenerWithArgs<? super T> listner);
    <T> boolean isRegistred(T object, IObjectChangedAsynchronousListenerWithArgs<? super T> listner, String type);
    <T> boolean isRegistred(T object, IObjectChangedSynchronousListenerWithArgs<? super T> listner);
    <T> boolean isRegistred(T object, IObjectChangedSynchronousListenerWithArgs<? super T> listner, String type);
 }
