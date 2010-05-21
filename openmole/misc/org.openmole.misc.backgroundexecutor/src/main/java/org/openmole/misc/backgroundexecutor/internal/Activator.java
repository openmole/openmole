package org.openmole.misc.backgroundexecutor.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.openmole.misc.executorservice.IExecutorService;
import org.openmole.misc.workspace.IWorkspace;
import org.openmole.misc.backgroundexecutor.IBackgroundExecutor;

public class Activator implements BundleActivator {

    private static BundleContext context;
    private static IExecutorService executorService;
    private static IWorkspace workspace;
    private static BackgroundExecutor transfertMonitor;
    private ServiceRegistration reg;

    /*
     * (non-Javadoc)
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(BundleContext context) throws Exception {
        Activator.context = context;

        transfertMonitor = new BackgroundExecutor();
        reg = context.registerService(IBackgroundExecutor.class.getName(), transfertMonitor, null);
    }

    /*
     * (non-Javadoc)
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext context) throws Exception {
        Activator.context = null;
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

    public static IWorkspace getWorkspace() {
        if (workspace != null) {
            return workspace;
        }

        synchronized (Activator.class) {
            if (workspace == null) {
                ServiceReference ref = getContext().getServiceReference(IWorkspace.class.getName());
                workspace = (IWorkspace) getContext().getService(ref);
            }
        }
        return workspace;
    }

    public static BackgroundExecutor getTransfertMonitor() {
        return transfertMonitor;
    }

    
    private static BundleContext getContext() {
        return context;
    }
}
