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


package org.openmole.misc.updater.internal;


import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.openmole.misc.executorservice.IExecutorService;
import org.openmole.misc.updater.IUpdater;
import org.openmole.misc.workspace.IWorkspace;

public class Activator implements BundleActivator {

	
	private static BundleContext context;

	private static IExecutorService executorService;
        private static IWorkspace workspace;
	private IUpdater updater;
	
	ServiceRegistration regUpdater;
	
	@Override
	public void start(BundleContext context) throws Exception {
		this.context = context;
		updater = new Updater();
		regUpdater = context.registerService(IUpdater.class.getName(), updater, null);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		regUpdater.unregister();
		//updater.setShutDown(true);
		updater = null;
		context = null;
	}

	public static IExecutorService getExecutorService() {
		
		if(executorService != null) return executorService;

		synchronized (Activator.class) {
			if(executorService == null) {
				ServiceReference ref = getContext().getServiceReference(IExecutorService.class.getName());
				executorService = (IExecutorService) getContext().getService(ref);
			}
		}
		return executorService;
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
	
	private static BundleContext getContext() {
		return context;
	}


}
