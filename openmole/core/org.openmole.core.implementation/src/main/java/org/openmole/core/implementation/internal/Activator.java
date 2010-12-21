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
package org.openmole.core.implementation.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.openmole.commons.aspect.eventdispatcher.IEventDispatcher;
import org.openmole.misc.executorservice.IExecutorService;
import org.openmole.misc.pluginmanager.IPluginManager;
import org.openmole.core.serializer.ISerializer;
import org.openmole.misc.workspace.IWorkspace;

public class Activator implements BundleActivator {

    static BundleContext context;
    private static IWorkspace workspace;
    private static ISerializer messageSerializer;
    private static IEventDispatcher eventDispatcher;
    private static IPluginManager pluginManager;

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

    public synchronized static ISerializer getSerializer() {
        if (messageSerializer == null) {
            ServiceReference ref = getContext().getServiceReference(ISerializer.class.getName());
            messageSerializer = (ISerializer) getContext().getService(ref);
        }
        return messageSerializer;
    }

    public synchronized static IPluginManager getPluginManager() {
        if (pluginManager == null) {
            ServiceReference ref = getContext().getServiceReference(IPluginManager.class.getName());
            pluginManager = (IPluginManager) getContext().getService(ref);
        }
        return pluginManager;
    }
}
