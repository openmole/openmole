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

package org.openmole.core.file.internal;

import org.openmole.core.file.IURIFileCache;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.openmole.core.batchservicecontrol.IBatchServiceControl;
import org.openmole.misc.executorservice.IExecutorService;
import org.openmole.core.jsagasession.IJSagaSessionService;
import org.openmole.misc.workspace.IWorkspace;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    private static BundleContext context;
    private static IWorkspace workspace;
    private static IJSagaSessionService jSagaSessionService;
    private static IBatchServiceControl batchRessourceControl;
    private static IExecutorService executorService;

    ServiceRegistration reg;
    
    @Override
    public void start(BundleContext context) throws Exception {
        Activator.context = context;

        IURIFileCache fileCache = new URIFileCache();
        reg = context.registerService(IURIFileCache.class.getName(), fileCache, null);

        //Activate JSAGA session bundle.
        getJSagaSessionService();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        reg.unregister();
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

    private static BundleContext getContext() {
        return context;
    }


}
