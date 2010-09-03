/*
 *  Copyright (C) 2010 reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the Affero GNU General Public License as published by
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

package org.openmole.misc.executorservice.internal;

import java.util.concurrent.ThreadFactory;
import org.openmole.commons.exception.UserBadDataError;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.misc.executorservice.IExecutorService;
import org.openmole.misc.workspace.IWorkspace;

/**
 *
 * @author reuillon
 */
public class Activator implements BundleActivator {
    private static IWorkspace workspace;
    private static BundleContext context;

    private ServiceRegistration regExecutor;
    private static IExecutorService executorService;

    @Override
    public void start(BundleContext context) throws Exception {
        this.context = context;
        executorService = executorService = new ExecutorService();
        regExecutor = context.registerService(IExecutorService.class.getName(), executorService, null);
    }

    @Override
    public void stop(BundleContext arg0) throws Exception {
        regExecutor.unregister();
    }

    public static BundleContext getContext() {
		return context;
	}


    public static IWorkspace getWorkspace() {
		if(workspace != null) return workspace;

		synchronized (Activator.class) {
			if(workspace  == null) {
				ServiceReference ref = getContext().getServiceReference(IWorkspace.class.getName());
				workspace = (IWorkspace) getContext().getService(ref);
			}
		}
		return workspace;
	}
}
