package org.openmole.misc.clonning.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.openmole.misc.clonning.IClonningService;

public class Activator implements BundleActivator {

    private IClonningService clonning;
    private ServiceRegistration reg;

    /*
     * (non-Javadoc)
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(BundleContext context) throws Exception {
        clonning = new ClonningService();
        reg = context.registerService(IClonningService.class.getName(), clonning, null);
    }

    /*
     * (non-Javadoc)
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext context) throws Exception {
        reg.unregister();
    }
}
