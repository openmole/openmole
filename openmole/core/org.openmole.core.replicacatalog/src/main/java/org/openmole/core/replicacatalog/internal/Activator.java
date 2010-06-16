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


package org.openmole.core.replicacatalog.internal;

import org.openmole.core.batchenvironmentauthenticationregistry.IBatchEnvironmentAuthenticationRegistry;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.openmole.misc.executorservice.IExecutorService;
import org.openmole.misc.updater.IUpdater;
import org.openmole.core.replicacatalog.IReplicaCatalog;
import org.openmole.misc.workspace.IWorkspace;

public class Activator implements BundleActivator {

    static BundleContext context;
    private static IWorkspace workspace;
    private static IExecutorService executorService;
 //   private static IBatchRessourceControl batchRessourceControl;
    private static IUpdater updater;
    private static IBatchEnvironmentAuthenticationRegistry batchEnvironmentAuthenticationRegistry;
    private IReplicaCatalog catalog;
    private ServiceRegistration reg;

    @Override
    public void start(BundleContext context) throws Exception {
        this.context = context;

        catalog = new ReplicaCatalog();
        reg = context.registerService(IReplicaCatalog.class.getName(), catalog, null);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        catalog.close();
        context = null;
        reg.unregister();
    }

    public synchronized static BundleContext getContext() {
        return context;
    }

    public synchronized static IWorkspace getWorkpace() {
        if (workspace == null) {
            ServiceReference ref = getContext().getServiceReference(IWorkspace.class.getName());
            workspace = (IWorkspace) getContext().getService(ref);
        }
        return workspace;
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


      public synchronized static IBatchEnvironmentAuthenticationRegistry getBatchEnvironmentAuthenticationRegistry() {
        if (batchEnvironmentAuthenticationRegistry == null) {
            ServiceReference ref = getContext().getServiceReference(IBatchEnvironmentAuthenticationRegistry.class.getName());
            batchEnvironmentAuthenticationRegistry = (IBatchEnvironmentAuthenticationRegistry) getContext().getService(ref);
        }
        return batchEnvironmentAuthenticationRegistry;
    }
}
