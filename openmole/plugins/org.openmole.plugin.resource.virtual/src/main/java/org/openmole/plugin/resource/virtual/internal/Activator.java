/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
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
package org.openmole.plugin.resource.virtual.internal;

import org.openmole.commons.aspect.caching.SoftCachable;
import org.openmole.misc.executorservice.IExecutorService;
import org.openmole.misc.workspace.IWorkspace;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class Activator implements BundleActivator {

    static Activator instance = new Activator();
    BundleContext context;

    @Override
    public void start(BundleContext context) throws Exception {
        instance.context = context;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        instance.context = null;
    }

    public static IExecutorService executorService() {
        return instance.getExecutorService();
    }

    public static IWorkspace workspace() {
        return instance.getWorkspace();
    }

    @SoftCachable
    private IExecutorService getExecutorService() {
        ServiceReference ref = context.getServiceReference(IExecutorService.class.getName());
        return (IExecutorService) context.getService(ref);
    }

    @SoftCachable
    private IWorkspace getWorkspace() {
        ServiceReference ref = context.getServiceReference(IWorkspace.class.getName());
        return (IWorkspace) context.getService(ref);
    }

 
}
