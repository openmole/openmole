package org.openmole.runtime.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.openmole.commons.aspect.eventdispatcher.IEventDispatcher;
import org.openmole.misc.hashservice.IHashService;
import org.openmole.misc.pluginmanager.IPluginManager;
import org.openmole.core.jsagasession.IJSagaSessionService;
import org.openmole.core.replicacatalog.IReplicaCatalog;
import org.openmole.core.execution.runtimemessageserializer.IEnvironmentDescriptionSerializer;
import org.openmole.core.execution.runtimemessageserializer.IRuntimeMessageSerializer;
import org.openmole.misc.backgroundexecutor.IBackgroundExecutor;
import org.openmole.misc.workspace.IWorkspace;

public class Activator implements BundleActivator {

    private static BundleContext context;
    private static IRuntimeMessageSerializer messageSerializer;
    private static IReplicaCatalog replicaCatalog;
    private static IWorkspace workspace;
    private static IEnvironmentDescriptionSerializer environmentDescriptionSerializer;
    private static IJSagaSessionService jSagaSessionService;
    private static IBackgroundExecutor backgroundExecutor;
    private static IEventDispatcher eventDispatcher;
    private static IPluginManager pluginManager;
    private static IHashService hashService;

    @Override
    public void start(BundleContext context) throws Exception {
        this.context = context;

    }

    @Override
    public void stop(BundleContext context) throws Exception {
        context = null;
    }

    public synchronized static IRuntimeMessageSerializer getMessageSerialiser() {
        if (messageSerializer == null) {
            ServiceReference ref = getContext().getServiceReference(IRuntimeMessageSerializer.class.getName());
            messageSerializer = (IRuntimeMessageSerializer) getContext().getService(ref);
        }
        return messageSerializer;
    }

    private static BundleContext getContext() {
        return context;
    }

    public synchronized static IReplicaCatalog getReplicaCatalog() {
        if (replicaCatalog == null) {
            ServiceReference ref = getContext().getServiceReference(IReplicaCatalog.class.getName());
            replicaCatalog = (IReplicaCatalog) getContext().getService(ref);
        }
        return replicaCatalog;
    }

    public synchronized static IWorkspace getWorkspace() {
        if (workspace == null) {
            ServiceReference ref = getContext().getServiceReference(IWorkspace.class.getName());
            workspace = (IWorkspace) getContext().getService(ref);
        }
        return workspace;
    }

    public synchronized static IEnvironmentDescriptionSerializer getEnvironmentDescriptionSerializer() {
        if (environmentDescriptionSerializer == null) {
            ServiceReference ref = getContext().getServiceReference(IEnvironmentDescriptionSerializer.class.getName());
            environmentDescriptionSerializer = (IEnvironmentDescriptionSerializer) getContext().getService(ref);
        }
        return environmentDescriptionSerializer;
    }

    public static IJSagaSessionService getJSagaSessionService() {
        if (jSagaSessionService != null) {
            return jSagaSessionService;
        }

        synchronized (Activator.class) {
            if (jSagaSessionService == null) {
                ServiceReference ref = getContext().getServiceReference(IJSagaSessionService.class.getName());
                jSagaSessionService = (IJSagaSessionService) getContext().getService(ref);
            }
            return jSagaSessionService;
        }
    }

    public static IBackgroundExecutor getBackgroundExecutor() {
        if (backgroundExecutor != null) {
            return backgroundExecutor;
        }

        synchronized (Activator.class) {
            if (backgroundExecutor == null) {
                ServiceReference ref = getContext().getServiceReference(IBackgroundExecutor.class.getName());
                backgroundExecutor = (IBackgroundExecutor) getContext().getService(ref);
            }
            return backgroundExecutor;
        }
    }

    public static IEventDispatcher getEventDispatcher() {
        if (eventDispatcher != null) {
            return eventDispatcher;
        }

        synchronized (Activator.class) {
            if (eventDispatcher == null) {
                ServiceReference ref = getContext().getServiceReference(IEventDispatcher.class.getName());
                eventDispatcher = (IEventDispatcher) getContext().getService(ref);
            }
            return eventDispatcher;
        }
    }

    public synchronized static IPluginManager getPluginManager() {
        if (pluginManager == null) {
            ServiceReference ref = getContext().getServiceReference(IPluginManager.class.getName());
            pluginManager = (IPluginManager) getContext().getService(ref);
        }
        return pluginManager;
    }


     public static IHashService getHashService() {
        if (hashService != null) {
            return hashService;
        }

        synchronized (Activator.class) {
            if (hashService == null) {
                ServiceReference ref = getContext().getServiceReference(IHashService.class.getName());
                hashService = (IHashService) getContext().getService(ref);
            }
        }
        return hashService;
    }
}
