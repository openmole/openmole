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
package org.openmole.misc.fileservice.internal;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.openmole.misc.fileservice.IFileService;
import org.openmole.misc.hashservice.IHashService;
import org.openmole.misc.updater.IUpdater;
import org.openmole.misc.workspace.IWorkspace;

/**
 *
 * @author reuillon
 */
public class Activator implements BundleActivator {

    private static IWorkspace workspace;
    private static BundleContext context;
    private static IHashService hashService;
    private static IUpdater updater;
    private ServiceRegistration regExecutor;
    private static ExecutorService cleanFiles;

    @Override
    public void start(BundleContext context) throws Exception {
        this.context = context;
        regExecutor = context.registerService(IFileService.class.getName(), new FileService(), null);

    }

    @Override
    public void stop(BundleContext arg0) throws Exception {
        regExecutor.unregister();
    }

    public static BundleContext getContext() {
        return context;
    }

    public static ExecutorService getCleanFiles() {
        if (cleanFiles != null) {
            return cleanFiles;
        }

        synchronized (Activator.class) {
            if (cleanFiles == null) {
                cleanFiles = Executors.newSingleThreadExecutor(new ThreadFactory() {

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread ret = new Thread(r);
                        ret.setDaemon(true);
                        return ret;
                    }
                });

            }
        }
        return cleanFiles;
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
}
