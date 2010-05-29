package org.openmole.ui.console.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.openmole.misc.pluginmanager.IPluginManager;
import org.openmole.misc.workspace.IWorkspace;
import org.openmole.core.structuregenerator.IStructureGenerator;
import org.openmole.ui.console.IConsole;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    static BundleContext context;
    private ServiceRegistration reg;
    private static Console console;
    private static IWorkspace workspace;
    private static IPluginManager pluginManager;
    private static IStructureGenerator structureGenerator;

    @Override
    public void start(BundleContext context) throws Exception {
        this.context = context;
        console = new Console();
        reg = context.registerService(IConsole.class.getName(), console, null);

    }

    @Override
    public void stop(BundleContext context) throws Exception {
        context = null;
        reg.unregister();
    }

    public static Console getConsole() {
        return console;
    }

    public synchronized static IPluginManager getPluginManager() {
        if (pluginManager == null) {
            ServiceReference ref = getContext().getServiceReference(IPluginManager.class.getName());
            pluginManager = (IPluginManager) getContext().getService(ref);
        }
        return pluginManager;
    }

    public synchronized static IWorkspace getWorkspace() {
        if (workspace == null) {
            ServiceReference ref = getContext().getServiceReference(IWorkspace.class.getName());
            workspace = (IWorkspace) getContext().getService(ref);
        }
        return workspace;
    }

    private static BundleContext getContext() {
        return context;
    }

    public synchronized static IStructureGenerator getStructureGenerator() {
        if (structureGenerator == null) {
            ServiceReference ref = getContext().getServiceReference(IStructureGenerator.class.getName());
            structureGenerator = (IStructureGenerator) getContext().getService(ref);
        }
        return structureGenerator;
    }
}
