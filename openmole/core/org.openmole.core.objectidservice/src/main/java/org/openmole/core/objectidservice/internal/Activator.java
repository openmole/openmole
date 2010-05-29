package org.openmole.core.objectidservice.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.openmole.core.objectidservice.IObjectIDService;

public class Activator implements BundleActivator {

    private static BundleContext context;
    private ServiceRegistration regService;
    private ObjectIDService service;
    /*
     * (non-Javadoc)
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(BundleContext context) throws Exception {
        Activator.context = context;
        service = new ObjectIDService();
	regService = context.registerService(IObjectIDService.class.getName(), service, null);
    }

    /*
     * (non-Javadoc)
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        service.interruptCollector();
        service = null;
        Activator.context = null;
        regService.unregister();
    }

    private static BundleContext getContext() {
        return context;
    }
}
