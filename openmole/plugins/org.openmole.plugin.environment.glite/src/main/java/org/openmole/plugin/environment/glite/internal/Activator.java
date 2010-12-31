package org.openmole.plugin.environment.glite.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.openmole.misc.executorservice.IExecutorService;
import org.openmole.core.jsagasession.IJSagaSessionService;
import org.openmole.misc.updater.IUpdater;
import org.openmole.misc.workspace.IWorkspace;


public class Activator implements BundleActivator{

	static BundleContext context;
	private static IUpdater _updater;
	private static IJSagaSessionService _jSagaSessionService;
	private static IWorkspace _workspace;
	private static IExecutorService _executorService;
	
	@Override
	public void start(BundleContext context) throws Exception {
		this.context = context;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		context = null;
	}


	private static BundleContext getContext() {
		return context;
	}
	
	
	public synchronized static IUpdater updater() {
		if(_updater == null) {
			ServiceReference ref = getContext().getServiceReference(IUpdater.class.getName());
			_updater = (IUpdater) getContext().getService(ref);
		}
		return _updater;
	}
	
	public static IJSagaSessionService JSagaSessionService() {
		if(_jSagaSessionService != null) return _jSagaSessionService;
		
		synchronized (Activator.class) {
			if(_jSagaSessionService  == null) {
				ServiceReference ref = getContext().getServiceReference(IJSagaSessionService.class.getName());
				_jSagaSessionService = (IJSagaSessionService) getContext().getService(ref);
			}
			return _jSagaSessionService;
		}
	}
	
	public static IWorkspace workspace() {
		if(_workspace != null) return _workspace;

		synchronized (Activator.class) {
			if(_workspace  == null) {
				ServiceReference ref = getContext().getServiceReference(IWorkspace.class.getName());
				_workspace = (IWorkspace) getContext().getService(ref);
			}
		}
		return _workspace;
	}

	public static IExecutorService executorService() {
		if(_executorService != null) return _executorService;

		synchronized (Activator.class) {
			if(_executorService == null) {
				ServiceReference ref = getContext().getServiceReference(IExecutorService.class.getName());
				_executorService = (IExecutorService) getContext().getService(ref);
			}
		}
		return _executorService;
	}


}
