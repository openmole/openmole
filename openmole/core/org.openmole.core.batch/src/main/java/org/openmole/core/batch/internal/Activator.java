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
import org.openmole.core.batch.replication.ReplicaCatalog;
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

    private static IEventDispatcher _eventDispatcher;
    private static BundleContext context;
    private static IWorkspace _workspace;
    private static IUpdater _updater;
    private static IExecutorService _executorService;
    private static IJSagaSessionService _JSAGASessionService;
    private static IFileService _fileService;
    private static ISerializer _serializer;
    private static IHashService _hashService;
    private static IPluginManager _pluginManager;
    
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

    public static IEventDispatcher eventDispatcher() {
        if (_eventDispatcher != null) {
            return _eventDispatcher;
        }

        synchronized (Activator.class) {
            if (_eventDispatcher == null) {
                ServiceReference ref = getContext().getServiceReference(IEventDispatcher.class.getName());
                _eventDispatcher = (IEventDispatcher) getContext().getService(ref);
            }
            return _eventDispatcher;
        }
    }

    
    public static BundleContext getContext() {
        return context;
    }

    public static IWorkspace workspace() {
        if (_workspace != null) {
            return _workspace;
        }

        synchronized (Activator.class) {
            if (_workspace == null) {
                ServiceReference ref = getContext().getServiceReference(IWorkspace.class.getName());
                _workspace = (IWorkspace) getContext().getService(ref);
            }
        }
        return _workspace;
    }
    
    public static IUpdater updater() {
        if (_updater != null) {
            return _updater;
        }

        synchronized (Activator.class) {
            if (_updater == null) {
                ServiceReference ref = getContext().getServiceReference(IUpdater.class.getName());
                _updater = (IUpdater) getContext().getService(ref);
            }
        }
        return _updater;
    }
    
    public static IExecutorService executorService() {
        if (_executorService != null) {
            return _executorService;
        }

        synchronized (Activator.class) {
            if (_executorService == null) {
                ServiceReference ref = getContext().getServiceReference(IExecutorService.class.getName());
                _executorService = (IExecutorService) getContext().getService(ref);
            }
        }
        return _executorService;
    }
    
    public static IJSagaSessionService JSAGASessionService() {
        if (_JSAGASessionService != null) {
            return _JSAGASessionService;
        }

        synchronized (Activator.class) {
            if (_JSAGASessionService == null) {
                ServiceReference ref = getContext().getServiceReference(IJSagaSessionService.class.getName());
                _JSAGASessionService = (IJSagaSessionService) getContext().getService(ref);
            }
        }
        return _JSAGASessionService;
    }
    
   public synchronized static ISerializer serializer() {
        if (_serializer == null) {
            ServiceReference ref = getContext().getServiceReference(ISerializer.class.getName());
            _serializer = (ISerializer) getContext().getService(ref);
        }
        return _serializer;
    }
   
   public synchronized static IFileService fileService() {
        if (_fileService == null) {
            ServiceReference ref = getContext().getServiceReference(IFileService.class.getName());
            _fileService = (IFileService) getContext().getService(ref);
        }
        return _fileService;
    }

    public static IHashService hashService() {
        if (_hashService != null) {
            return _hashService;
        }

        synchronized (Activator.class) {
            if (_hashService == null) {
                ServiceReference ref = getContext().getServiceReference(IHashService.class.getName());
                _hashService = (IHashService) getContext().getService(ref);
            }
        }
        return _hashService;
    }
    
    public synchronized static IPluginManager pluginManager() {
        if (_pluginManager == null) {
            ServiceReference ref = getContext().getServiceReference(IPluginManager.class.getName());
            _pluginManager = (IPluginManager) getContext().getService(ref);
        }
        return _pluginManager;
    }
}
