/*
 *  Copyright (C) 2010 reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.core.workflow.implementation.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.openmole.core.batchservicecontrol.IBatchServiceControl;
import org.openmole.misc.clonning.IClonningService;
import org.openmole.commons.aspect.eventdispatcher.IEventDispatcher;
import org.openmole.misc.executorservice.IExecutorService;
import org.openmole.core.fileservice.IFileService;
import org.openmole.misc.hashservice.IHashService;
import org.openmole.misc.pluginmanager.IPluginManager;
import org.openmole.core.jsagasession.IJSagaSessionService;
import org.openmole.core.replicacatalog.IReplicaCatalog;
import org.openmole.core.execution.runtimemessageserializer.IRuntimeMessageSerializer;
import org.openmole.misc.backgroundexecutor.IBackgroundExecutor;
import org.openmole.misc.updater.IUpdater;
import org.openmole.misc.workspace.IWorkspace;
import org.openmole.core.environmentprovider.IEnvironmentProvider;
import org.openmole.core.execution.runtimemessageserializer.IEnvironmentDescriptionSerializer;

public class Activator implements BundleActivator {

    static BundleContext context;
    private static transient IClonningService clonning;
    private static transient IUpdater updater;
    private static transient IReplicaCatalog replicaCatalog;
    private static IWorkspace workspace;
    private static IRuntimeMessageSerializer messageSerializer;
    private static IExecutorService executorService;
    private static IJSagaSessionService jSagaSessionService;
    private static IBatchServiceControl batchRessourceControl;
    private static IBackgroundExecutor transferMonitor;
    private static IEventDispatcher eventDispatcher;
    private static IPluginManager pluginManager;
    private static IEnvironmentProvider environmentProvider;
    private static IFileService fileService;
    private static IHashService hashService;
    private static IEnvironmentDescriptionSerializer environmentDescriptionSerializer;

    @Override
    public synchronized void start(BundleContext context) throws Exception {
        this.context = context;
    }

    @Override
    public synchronized void stop(BundleContext context) throws Exception {
        this.context = null;
    }

    public synchronized static BundleContext getContext() {
        return context;
    }

    public static IReplicaCatalog getReplicaCatalog() {
        if (replicaCatalog != null) {
            return replicaCatalog;
        }

        synchronized (Activator.class) {
            if (replicaCatalog == null) {
                ServiceReference ref = getContext().getServiceReference(IReplicaCatalog.class.getName());
                replicaCatalog = (IReplicaCatalog) getContext().getService(ref);
            }
        }

        return replicaCatalog;
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

    public static IClonningService getClonningService() {
        if (clonning != null) {
            return clonning;
        }

        synchronized (Activator.class) {
            if (clonning == null) {
                ServiceReference ref = getContext().getServiceReference(IClonningService.class.getName());
                clonning = (IClonningService) getContext().getService(ref);
            }
        }
        return clonning;
    }

    public static IUpdater getUpdater() {
        if (updater != null) {
            return updater;
        }

        synchronized (Activator.class) {
            if (updater == null) {
                ServiceReference ref = getContext().getServiceReference(IUpdater.class.getName());
                updater = (IUpdater) getContext().getService(ref);
            }
        }
        return updater;
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

    public synchronized static IRuntimeMessageSerializer getMessageSerialiser() {
        if (messageSerializer == null) {
            ServiceReference ref = getContext().getServiceReference(IRuntimeMessageSerializer.class.getName());
            messageSerializer = (IRuntimeMessageSerializer) getContext().getService(ref);
        }
        return messageSerializer;
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

    public static IBatchServiceControl getBatchRessourceControl() {
        if (batchRessourceControl != null) {
            return batchRessourceControl;
        }

        synchronized (Activator.class) {
            if (batchRessourceControl == null) {
                ServiceReference ref = getContext().getServiceReference(IBatchServiceControl.class.getName());
                batchRessourceControl = (IBatchServiceControl) getContext().getService(ref);
            }
            return batchRessourceControl;
        }
    }

    public static IBackgroundExecutor getBackgroundExecutor() {
        if (transferMonitor != null) {
            return transferMonitor;
        }

        synchronized (Activator.class) {
            if (transferMonitor == null) {
                ServiceReference ref = getContext().getServiceReference(IBackgroundExecutor.class.getName());
                transferMonitor = (IBackgroundExecutor) getContext().getService(ref);
            }
            return transferMonitor;
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


    public synchronized static IEnvironmentProvider getEnvironmentProvider() {
        if (environmentProvider == null) {
            ServiceReference ref = getContext().getServiceReference(IEnvironmentProvider.class.getName());
            environmentProvider = (IEnvironmentProvider) getContext().getService(ref);
        }
        return environmentProvider;
    }

    public synchronized static IFileService getFileService() {
        if (fileService == null) {
            ServiceReference ref = getContext().getServiceReference(IFileService.class.getName());
            fileService = (IFileService) getContext().getService(ref);
        }
        return fileService;
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

    public synchronized static IEnvironmentDescriptionSerializer getEnvironmentDescriptionSerializer() {
        if (environmentDescriptionSerializer == null) {
            ServiceReference ref = getContext().getServiceReference(IEnvironmentDescriptionSerializer.class.getName());
            environmentDescriptionSerializer = (IEnvironmentDescriptionSerializer) getContext().getService(ref);
        }
        return environmentDescriptionSerializer;
    }
}
