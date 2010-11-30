/*
 *  Copyright (C) 2010 reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
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
package org.openmole.core.batch.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.openmole.commons.aspect.eventdispatcher.IEventDispatcher;
import org.openmole.core.jsagasession.IJSagaSessionService;
import org.openmole.core.serializer.ISerializer;
import org.openmole.misc.executorservice.IExecutorService;
import org.openmole.misc.fileservice.IFileService;
import org.openmole.misc.hashservice.IHashService;
import org.openmole.misc.pluginmanager.IPluginManager;
import org.openmole.misc.updater.IUpdater;
import org.openmole.misc.workspace.IWorkspace;
import org.osgi.framework.ServiceReference;

public class Activator implements BundleActivator {

    private static IEventDispatcher eventDispatcher;
    private static BundleContext context;
    private static IWorkspace workspace;
    private static IUpdater updater;
    private static IExecutorService executorService;
    private static IJSagaSessionService JSAGASessionService;
    private static IFileService fileService;
    private static ISerializer messageSerializer;
    private static IHashService hashService;
    private static IPluginManager pluginManager;
    
    /*
     * (non-Javadoc)
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    public void start(BundleContext context) throws Exception {
        this.context = context;
    }

    /*
     * (non-Javadoc)
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext context) throws Exception {

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

    
    public static BundleContext getContext() {
        return context;
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
    
    public static IJSagaSessionService getJSAGASessionService() {
        if (JSAGASessionService != null) {
            return JSAGASessionService;
        }

        synchronized (Activator.class) {
            if (JSAGASessionService == null) {
                ServiceReference ref = getContext().getServiceReference(IJSagaSessionService.class.getName());
                JSAGASessionService = (IJSagaSessionService) getContext().getService(ref);
            }
        }
        return JSAGASessionService;
    }
    
   public synchronized static ISerializer getSerializer() {
        if (messageSerializer == null) {
            ServiceReference ref = getContext().getServiceReference(ISerializer.class.getName());
            messageSerializer = (ISerializer) getContext().getService(ref);
        }
        return messageSerializer;
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
    
    public synchronized static IPluginManager getPluginManager() {
        if (pluginManager == null) {
            ServiceReference ref = getContext().getServiceReference(IPluginManager.class.getName());
            pluginManager = (IPluginManager) getContext().getService(ref);
        }
        return pluginManager;
    }
}
