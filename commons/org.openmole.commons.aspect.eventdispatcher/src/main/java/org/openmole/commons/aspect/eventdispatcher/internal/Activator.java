/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openmole.commons.aspect.eventdispatcher.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.openmole.commons.aspect.eventdispatcher.IEventDispatcher;

/**
 *
 * @author reuillon
 */
public class Activator implements BundleActivator {

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

    public static IEventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }

    public static BundleContext getContext() {
        return context;
    }
}
