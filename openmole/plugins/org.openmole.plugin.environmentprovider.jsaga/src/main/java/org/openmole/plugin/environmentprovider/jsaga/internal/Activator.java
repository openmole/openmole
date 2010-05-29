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


package org.openmole.plugin.environmentprovider.jsaga.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import org.openmole.misc.executorservice.IExecutorService;
import org.openmole.core.jsagasession.IJSagaSessionService;
import org.openmole.core.workflow.model.execution.IEnvironmentExecutionStatistics;
import org.openmole.core.execution.replicacatalog.IReplicaCatalog;
import org.openmole.core.execution.runtimemessageserializer.IEnvironmentDescriptionSerializer;
import org.openmole.core.execution.runtimemessageserializer.IRuntimeMessageSerializer;
import org.openmole.misc.updater.IUpdater;
import org.openmole.misc.workspace.IWorkspace;

public class Activator implements BundleActivator {

	private static BundleContext context;
	private static IUpdater updater;
	private static IRuntimeMessageSerializer messageSerializer;
	private static IReplicaCatalog replicaCatalog;
	private static IWorkspace workspace;
	private static IJSagaSessionService jSagaSessionService;
        private static IExecutorService executorService;
	
	@Override
	public void start(BundleContext context) throws Exception {
		this.context = context;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		this.context = null;
	}
	
	
	
	public synchronized static IWorkspace getWorkspace() {
		if(workspace  == null) {
			ServiceReference ref = getContext().getServiceReference(IWorkspace.class.getName());
			workspace = (IWorkspace) getContext().getService(ref);
		}
		return workspace;
	}
/*	public static IExecutionJobRegistry getExecutionJobRegistry() {
		if(executionJobRegistry == null) {
			ServiceReference ref = getContext().getServiceReference(IExecutionJobRegistry.class.getName());
			executionJobRegistry = (IExecutionJobRegistry) getContext().getService(ref);
		}
		return executionJobRegistry;
	}
*/
	
	/*public static IExecutionStat getExecutionStatService() {
		if(statService == null) {
			ServiceReference ref = getContext().getServiceReference(IExecutionStat.class.getName());
			statService = (IExecutionStat) getContext().getService(ref);
		}
		return statService;
	}*/
	
	public synchronized static IUpdater getUpdater() {
		if(updater == null) {
			ServiceReference ref = getContext().getServiceReference(IUpdater.class.getName());
			updater = (IUpdater) getContext().getService(ref);
		}
		return updater;
	}
	
	public synchronized static IRuntimeMessageSerializer getMessageSerialiser() {
		if(messageSerializer == null) {
			ServiceReference ref = getContext().getServiceReference(IRuntimeMessageSerializer.class.getName());
			messageSerializer = (IRuntimeMessageSerializer) getContext().getService(ref);
		}
		return messageSerializer;
	}
	
	
	public synchronized static IReplicaCatalog getReplicaCatalog() {
		if(replicaCatalog  == null) {
			ServiceReference ref = getContext().getServiceReference(IReplicaCatalog.class.getName());
			replicaCatalog = (IReplicaCatalog) getContext().getService(ref);
		}
		return replicaCatalog;
	}

	private static BundleContext getContext() {
		return context;
	}

	public static IJSagaSessionService getJSagaSessionService() {
		if(jSagaSessionService != null) return jSagaSessionService;
		
		synchronized (Activator.class) {
			if(jSagaSessionService  == null) {
				ServiceReference ref = getContext().getServiceReference(IJSagaSessionService.class.getName());
				jSagaSessionService = (IJSagaSessionService) getContext().getService(ref);
			}
			return jSagaSessionService;
		}
	}

        public synchronized static IExecutorService getExecutorService() {
		if(executorService == null) {
			ServiceReference ref = getContext().getServiceReference(IExecutorService.class.getName());
			executorService = (IExecutorService) getContext().getService(ref);
		}
		return executorService;
	}
	
}
