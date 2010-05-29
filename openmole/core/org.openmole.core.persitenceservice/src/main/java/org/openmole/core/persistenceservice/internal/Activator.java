package org.openmole.core.commons.persistenceservice.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.openmole.core.commons.persistenceservice.IPersistenceService;

public class Activator implements BundleActivator {

    private static BundleContext context;
    private ServiceRegistration regService;

    /*
     * (non-Javadoc)
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(BundleContext context) throws Exception {
        Activator.context = context;
        IPersistenceService persistenceService = new PersitenceService();
	regService = context.registerService(IPersistenceService.class.getName(), persistenceService, null);
    }

    /*
     * (non-Javadoc)
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        Activator.context = null;
        regService.unregister();
    }

    
    private static BundleContext getContext() {
        return context;
    }
}
