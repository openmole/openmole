package org.openmole.core.authenticationregistry.ssh.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.openmole.misc.executorservice.IExecutorService;
import org.openmole.core.jsagasession.IJSagaSessionService;
import org.openmole.misc.updater.IUpdater;
import org.openmole.misc.workspace.IWorkspace;


public class Activator implements BundleActivator{

	static BundleContext context;
	private static IUpdater updater;
	private static IJSagaSessionService jSagaSessionService;
	private static IWorkspace workspace;
	private static IExecutorService executorService;
	
	@Override
	public void start(BundleContext context) throws Exception {
		this.context = context;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		context = null;
	}

	
/*	public static IExecutionJobRegistry getExecutionJobRegistry() {
		if(executionJobRegistry == null) {
			ServiceReference ref = getContext().getServiceReference(IExecutionJobRegistry.class.getName());
			executionJobRegistry = (IExecutionJobRegistry) getContext().getService(ref);
		}
		return executionJobRegistry;
	}*/

	private static BundleContext getContext() {
		return context;
	}
	
	
	public synchronized static IUpdater getUpdater() {
		if(updater == null) {
			ServiceReference ref = getContext().getServiceReference(IUpdater.class.getName());
			updater = (IUpdater) getContext().getService(ref);
		}
		return updater;
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


}
