/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openmole.commons.aspect.eventdispatcher.internal;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.openmole.commons.aspect.eventdispatcher.IEventDispatcher;
import org.openmole.commons.aspect.eventdispatcher.IObjectChangedAsynchronousListener;
import org.openmole.commons.aspect.eventdispatcher.IObjectChangedAsynchronousListenerWithArgs;
import org.openmole.commons.aspect.eventdispatcher.IObjectChangedSynchronousListenerWithArgs;
import org.openmole.commons.aspect.eventdispatcher.IObjectChangedSynchronousListener;
import org.openmole.commons.aspect.eventdispatcher.IObjectConstructedAsynchronousListener;
import org.openmole.commons.aspect.eventdispatcher.IObjectConstructedSynchronousListener;
import org.openmole.commons.tools.structure.Priority;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;


/**
 *
 * @author reuillon
 */
public class EventDispatcher implements IEventDispatcher {

    final static private ExecutorService Executor = Executors.newCachedThreadPool();

    final ObjectChangedListnerMap<IObjectChangedAsynchronousListener> asynchronousObjectChangedMap = new ObjectChangedListnerMap<IObjectChangedAsynchronousListener>();
    final ObjectChangedListnerMap<IObjectChangedSynchronousListener> synchronousObjectChangedMap = new ObjectChangedListnerMap<IObjectChangedSynchronousListener>();
    final ObjectChangedListnerMap<IObjectChangedAsynchronousListenerWithArgs> asynchronousObjectChangedWithArgsMap = new ObjectChangedListnerMap<IObjectChangedAsynchronousListenerWithArgs>();
    final ObjectChangedListnerMap<IObjectChangedSynchronousListenerWithArgs> synchronousObjectChangedWithArgsMap = new ObjectChangedListnerMap<IObjectChangedSynchronousListenerWithArgs>();
    final ObjectConstructedListnerMap<IObjectConstructedAsynchronousListener> asynchronousObjectConstructedMap = new ObjectConstructedListnerMap<IObjectConstructedAsynchronousListener>();
    final ObjectConstructedListnerMap<IObjectConstructedSynchronousListener> synchronousObjectConstructedMap = new ObjectConstructedListnerMap<IObjectConstructedSynchronousListener>();

    @Override
    public <T> void registerListener(T object, IObjectChangedAsynchronousListener<? super T> listner) {
        asynchronousObjectChangedMap.registerListner(object, Priority.NORMAL.getValue(), listner);
    }

    @Override
    public <T> void registerListener(T object, IObjectChangedAsynchronousListener<? super T> listner, String type) {
        asynchronousObjectChangedMap.registerListner(object, Priority.NORMAL.getValue(), listner, type);
    }

    @Override
    public void objectChanged(final Object object, final String type, final Object[] args) throws InternalProcessingError, UserBadDataError {

        final Iterable<IObjectChangedAsynchronousListener> objectChangedAsynchronouslisteners = asynchronousObjectChangedMap.getListners(object);
        final Iterable<IObjectChangedAsynchronousListener> objectChangedWithTypeAsynchronouslisteners = asynchronousObjectChangedMap.getListners(object, type);

        final Iterable<IObjectChangedAsynchronousListenerWithArgs> objectChangedAsynchronouslistenersWithArgs = asynchronousObjectChangedWithArgsMap.getListners(object);
        final Iterable<IObjectChangedAsynchronousListenerWithArgs> objectChangedWithTypeAsynchronouslistenersWithArgs = asynchronousObjectChangedWithArgsMap.getListners(object, type);


        //Avoid creating threads if no listners
        if (objectChangedAsynchronouslisteners.iterator().hasNext() || objectChangedWithTypeAsynchronouslisteners.iterator().hasNext()) {

            Executor.submit(new Runnable() {

                @Override
                public void run() {

                    /* --- Listners without args ---*/

                    synchronized (objectChangedAsynchronouslisteners) {
                        for (IObjectChangedAsynchronousListener listner : objectChangedAsynchronouslisteners) {
                            listner.objectChanged(object);
                        }
                    }

                    synchronized (objectChangedWithTypeAsynchronouslisteners) {
                        for (IObjectChangedAsynchronousListener listner : objectChangedWithTypeAsynchronouslisteners) {
                            listner.objectChanged(object);
                        }
                    }

                    /* --- Listners with args ---*/

                    synchronized (objectChangedAsynchronouslistenersWithArgs) {
                        for (IObjectChangedAsynchronousListenerWithArgs listner : objectChangedAsynchronouslistenersWithArgs) {
                            listner.objectChanged(object, args);
                        }
                    }

                    synchronized (objectChangedWithTypeAsynchronouslistenersWithArgs) {
                        for (IObjectChangedAsynchronousListenerWithArgs listner : objectChangedWithTypeAsynchronouslistenersWithArgs) {
                            listner.objectChanged(object, args);
                        }
                    }
                }
            });
        }

        /* --- Listners without args ---*/

        Iterable<IObjectChangedSynchronousListener> listeners = synchronousObjectChangedMap.getListners(object);

        synchronized (listeners) {
            for (IObjectChangedSynchronousListener listner : listeners) {
                listner.objectChanged(object);
            }
        }

        listeners = synchronousObjectChangedMap.getListners(object, type);

        synchronized (listeners) {
            for (IObjectChangedSynchronousListener listner : listeners) {
                listner.objectChanged(object);
            }
        }

        /* --- Listners with args ---*/

        Iterable<IObjectChangedSynchronousListenerWithArgs> listenersWithArgs = synchronousObjectChangedWithArgsMap.getListners(object);

        synchronized (listenersWithArgs) {
            for (IObjectChangedSynchronousListenerWithArgs listner : listenersWithArgs) {
                listner.objectChanged(object, args);
            }
        }

        listenersWithArgs = synchronousObjectChangedWithArgsMap.getListners(object, type);

        synchronized (listenersWithArgs) {
            for (IObjectChangedSynchronousListenerWithArgs listner : listenersWithArgs) {
                listner.objectChanged(object, args);
            }
        }

    }

    @Override
    public <T> void registerListener(T object, Integer priority, IObjectChangedSynchronousListener<? super T> listner) {
        synchronousObjectChangedMap.registerListner(object, priority, listner);
    }

    @Override
    public <T> void registerListener(T object, Integer priority, IObjectChangedSynchronousListener<? super T> listner, String type) {
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
    public <T> void registerListener(T object, IObjectChangedAsynchronousListenerWithArgs<? super T> listner) {
        asynchronousObjectChangedWithArgsMap.registerListner(object, Priority.NORMAL.getValue(), listner);
    }

    @Override
    public <T> void registerListener(T object, Integer priority, IObjectChangedSynchronousListenerWithArgs<? super T> listner) {
        synchronousObjectChangedWithArgsMap.registerListner(object, priority, listner);
    }

    @Override
    public <T> void registerListener(T object, IObjectChangedAsynchronousListenerWithArgs<? super T> listner, String type) {
        asynchronousObjectChangedWithArgsMap.registerListner(object, Priority.NORMAL.getValue(), listner, type);
    }

    @Override
    public <T> void registerListener(T object, Integer priority, IObjectChangedSynchronousListenerWithArgs<? super T> listner, String type) {
        synchronousObjectChangedWithArgsMap.registerListner(object, priority, listner, type);
    }

    @Override
    public void objectChanged(Object object, String type) throws InternalProcessingError, UserBadDataError {
        objectChanged(object, type, Collections.EMPTY_LIST.toArray());
    }

    @Override
    public <T> boolean isRegistred(T object, IObjectChangedAsynchronousListener<? super T> listner) {
        return asynchronousObjectChangedMap.containsListner(object, listner);
    }

    @Override
    public <T> boolean isRegistred(T object, IObjectChangedAsynchronousListener<? super T> listner, String type) {
        return asynchronousObjectChangedMap.containsListner(object, type, listner);
    }

    @Override
    public <T> boolean isRegistred(T object, IObjectChangedSynchronousListener<? super T> listner) {
        return synchronousObjectChangedMap.containsListner(object, listner);
    }

    @Override
    public <T> boolean isRegistred(T object, IObjectChangedSynchronousListener<? super T> listner, String type) {
        return synchronousObjectChangedMap.containsListner(object, type, listner);
    }

    @Override
    public <T> boolean isRegistred(T object, IObjectChangedAsynchronousListenerWithArgs<? super T> listner) {
        return asynchronousObjectChangedWithArgsMap.containsListner(object, listner);
    }

    @Override
    public <T> boolean isRegistred(T object, IObjectChangedAsynchronousListenerWithArgs<? super T> listner, String type) {
        return asynchronousObjectChangedWithArgsMap.containsListner(object, type, listner);
    }

    @Override
    public <T> boolean isRegistred(T object, IObjectChangedSynchronousListenerWithArgs<? super T> listner) {
        return synchronousObjectChangedWithArgsMap.containsListner(object, listner);
    }

    @Override
    public <T> boolean isRegistred(T object, IObjectChangedSynchronousListenerWithArgs<? super T> listner, String type) {
        return synchronousObjectChangedWithArgsMap.containsListner(object, type, listner);
    }

  

}
