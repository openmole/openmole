/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openmole.misc.eventdispatcher.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.openmole.misc.eventdispatcher.IEventDispatcher;
import org.openmole.misc.executorservice.IExecutorService;

/**
 *
 * @author reuillon
 */
public class Activator implements BundleActivator {

    private static IExecutorService executorService;
    private static IEventDispatcher eventDispatcher;
    private static BundleContext context;
    private ServiceRegistration reg;

    @Override
    public void start(BundleContext bc) throws Exception {
        this.context = bc;
        eventDispatcher = new EventDispatcher();
        reg = bc.registerService(IEventDispatcher.class.getName(), eventDispatcher, null);
    }

    @Override
    public void stop(BundleContext bc) throws Exception {
        reg.unregister();
    }

    public static IExecutorService getExecutorService() {
        if (executorService != null) {
            return executorService;
        }

        synchronized (Activator.class) {
            if (executorService == null) {
                ServiceReference ref = getContext().getServiceReference(IExecutorService.class.getName());
                executorService = (IExecutorService) getContext().getService(ref);
            }
        }
        return executorService;
    }

    public static IEventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }

    public static BundleContext getContext() {
        return context;
    }
}
