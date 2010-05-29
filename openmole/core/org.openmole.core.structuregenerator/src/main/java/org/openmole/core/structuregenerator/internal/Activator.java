package org.openmole.core.structuregenerator.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.openmole.misc.pluginmanager.IPluginManager;
import org.openmole.misc.workspace.IWorkspace;
import org.openmole.core.structuregenerator.IStructureGenerator;

public class Activator implements BundleActivator {

    static BundleContext context;
    private static IWorkspace workspace;
    private static IPluginManager pluginManager;
    private ServiceRegistration reg;

    @Override
    public void start(BundleContext context) throws Exception {
        this.context = context;

        IStructureGenerator structureGenerator = new StructureGenerator();
        reg = context.registerService(IStructureGenerator.class.getName(), structureGenerator, null);

    }

    @Override
    public void stop(BundleContext context) throws Exception {
        context = null;
        reg.unregister();
    }

    public synchronized static BundleContext getContext() {
        return context;
    }

    public synchronized static IWorkspace getWorkspace() {
        if (workspace == null) {
            ServiceReference ref = getContext().getServiceReference(IWorkspace.class.getName());
            workspace = (IWorkspace) getContext().getService(ref);
        }
        return workspace;
    }


    public synchronized static IPluginManager getPluginManager() {
        if (pluginManager == null) {
            ServiceReference ref = getContext().getServiceReference(IPluginManager.class.getName());
            pluginManager = (IPluginManager) getContext().getService(ref);
        }
        return pluginManager;
    }
}
