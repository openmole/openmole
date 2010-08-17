/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openmole.commons.aspect.eventdispatcher.internal;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openmole.commons.aspect.eventdispatcher.IEventDispatcher;
import org.openmole.commons.aspect.eventdispatcher.IObjectChangedAsynchronousListener;
import org.openmole.commons.aspect.eventdispatcher.IObjectChangedAsynchronousListenerWithArgs;
import org.openmole.commons.aspect.eventdispatcher.IObjectChangedSynchronousListenerWithArgs;
import org.openmole.commons.aspect.eventdispatcher.IObjectChangedSynchronousListener;
import org.openmole.commons.aspect.eventdispatcher.IObjectConstructedAsynchronousListener;
import org.openmole.commons.aspect.eventdispatcher.IObjectConstructedSynchronousListener;
import org.openmole.commons.tools.service.Priority;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;


/**
 *
 * @author reuillon
 */
public class EventDispatcher implements IEventDispatcher {

    final static Logger LOGGER = Logger.getLogger(EventDispatcher.class.getName());
    volatile int nb = 0;
    
    final static private ExecutorService Executor = Executors.newCachedThreadPool();

    final ObjectChangedListnerMap<IObjectChangedAsynchronousListener> asynchronousObjectChangedMap = new ObjectChangedListnerMap<IObjectChangedAsynchronousListener>();
    final ObjectChangedListnerMap<IObjectChangedSynchronousListener> synchronousObjectChangedMap = new ObjectChangedListnerMap<IObjectChangedSynchronousListener>();
    final ObjectChangedListnerMap<IObjectChangedAsynchronousListenerWithArgs> asynchronousObjectChangedWithArgsMap = new ObjectChangedListnerMap<IObjectChangedAsynchronousListenerWithArgs>();
    final ObjectChangedListnerMap<IObjectChangedSynchronousListenerWithArgs> synchronousObjectChangedWithArgsMap = new ObjectChangedListnerMap<IObjectChangedSynchronousListenerWithArgs>();
    final ObjectConstructedListnerMap<IObjectConstructedAsynchronousListener> asynchronousObjectConstructedMap = new ObjectConstructedListnerMap<IObjectConstructedAsynchronousListener>();
    final ObjectConstructedListnerMap<IObjectConstructedSynchronousListener> synchronousObjectConstructedMap = new ObjectConstructedListnerMap<IObjectConstructedSynchronousListener>();


    @Override
    public <T> void registerListener(T object, IObjectChangedAsynchronousListener<? super T> listner, String type) {
        asynchronousObjectChangedMap.registerListner(object, Priority.NORMAL.getValue(), listner, type);
    }

    @Override
    public void objectChanged(final Object object, final String type, final Object[] args) throws InternalProcessingError, UserBadDataError {
        
        final Iterable<IObjectChangedAsynchronousListener> objectChangedWithTypeAsynchronouslisteners = asynchronousObjectChangedMap.getListners(object, type);

        final Iterable<IObjectChangedAsynchronousListenerWithArgs> objectChangedWithTypeAsynchronouslistenersWithArgs = asynchronousObjectChangedWithArgsMap.getListners(object, type);


        //Avoid creating threads if no listners
        if (objectChangedWithTypeAsynchronouslisteners.iterator().hasNext()) {

            Executor.submit(new Runnable() {

                @Override
                public void run() {

                    /* --- Listners without args ---*/

                    synchronized (objectChangedWithTypeAsynchronouslisteners) {
                        for (IObjectChangedAsynchronousListener listner : objectChangedWithTypeAsynchronouslisteners) {
                            listner.objectChanged(object);
                        }
                    }

                    /* --- Listners with args ---*/

                    synchronized (objectChangedWithTypeAsynchronouslistenersWithArgs) {
                        for (IObjectChangedAsynchronousListenerWithArgs listner : objectChangedWithTypeAsynchronouslistenersWithArgs) {
                            listner.objectChanged(object, args);
                        }
                    }
                }
            });
        }
    
        /* --- Listners without args ---*/

        Iterable<IObjectChangedSynchronousListener> listeners = synchronousObjectChangedMap.getListners(object, type);

        synchronized (listeners) {
            for (IObjectChangedSynchronousListener listner : listeners) {
                listner.objectChanged(object);
            }
        }

        /* --- Listners with args ---*/

        Iterable<IObjectChangedSynchronousListenerWithArgs> listenersWithArgs = synchronousObjectChangedWithArgsMap.getListners(object, type);

        synchronized (listenersWithArgs) {
            for (IObjectChangedSynchronousListenerWithArgs listner : listenersWithArgs) {
                listner.objectChanged(object, args);
            }
        }

    }

    @Override
    public <T> void registerListener(T object, Integer priority, IObjectChangedSynchronousListener<? super T> listner, String type) {
        LOGGER.log(Level.FINE, "Register {0} {1}", new Object[]{nb++, object});
        synchronousObjectChangedMap.registerListner(object, priority, listner, type);
    }

    @Override
    public <T> void registerListener(Class<T> c, Integer priority, IObjectConstructedSynchronousListener<? super T> listner) {
        synchronousObjectConstructedMap.registerListner(c, priority, listner);
    }

    @Override
    public <T> void registerListener(Class<T> c, IObjectConstructedAsynchronousListener<? super T> listner) {
        asynchronousObjectConstructedMap.registerListner(c, Priority.NORMAL.getValue(), listner);
    }

    @Override
    public void objectConstructed(final Object object) {

        final Class c = object.getClass();
        final Iterable<IObjectConstructedAsynchronousListener> asynchronousObjectConstructedListners = asynchronousObjectConstructedMap.getListners(c);


        //Dont create thread if no assynchronous listner are registred
        if (asynchronousObjectConstructedListners.iterator().hasNext()) {
            Executor.submit(new Runnable() {

                @Override
                public void run() {
                    synchronized (asynchronousObjectConstructedListners) {
                        for (IObjectConstructedAsynchronousListener listner : asynchronousObjectConstructedListners) {
                            listner.objectConstructed(object);
                        }
                    }
                }
            });
        }

        Iterable<IObjectConstructedSynchronousListener> listeners = synchronousObjectConstructedMap.getListners(c);

        synchronized (listeners) {
            for (IObjectConstructedSynchronousListener listner : listeners) {
                listner.objectConstructed(object);
            }
        }
    }

    @Override
    public <T> void registerListener(T object, IObjectChangedAsynchronousListenerWithArgs<? super T> listner, String type) {
        LOGGER.log(Level.FINE, "Register {0} {1}", new Object[]{nb++, object});

        asynchronousObjectChangedWithArgsMap.registerListner(object, Priority.NORMAL.getValue(), listner, type);
    }

    @Override
    public <T> void registerListener(T object, Integer priority, IObjectChangedSynchronousListenerWithArgs<? super T> listner, String type) {
        LOGGER.log(Level.FINE, "Register {0} {1}", new Object[]{nb++, object});

        synchronousObjectChangedWithArgsMap.registerListner(object, priority, listner, type);
    }

    @Override
    public void objectChanged(Object object, String type) throws InternalProcessingError, UserBadDataError {
        LOGGER.log(Level.FINE, "Object changed {0}", object);
        objectChanged(object, type, Collections.EMPTY_LIST.toArray());
    }

}
