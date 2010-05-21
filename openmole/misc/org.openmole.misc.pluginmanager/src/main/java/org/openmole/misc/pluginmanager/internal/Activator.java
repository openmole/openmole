/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openmole.misc.pluginmanager.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.packageadmin.PackageAdmin;
import org.openmole.misc.pluginmanager.IPluginManager;

/**
 *
 * @author reuillon
 */
public class Activator implements BundleActivator {
    private static BundleContext context;
    private static PackageAdmin packageAdmin;

    private ServiceRegistration regExecutor;
    private static IPluginManager pluginManager;

    @Override
    public void start(BundleContext context) throws Exception {
        this.context = context;
        pluginManager = new PluginManager();
        regExecutor = context.registerService(IPluginManager.class.getName(), pluginManager, null);
    }

    @Override
    public void stop(BundleContext arg0) throws Exception {
        regExecutor.unregister();
    }

    public static BundleContext getContext() {
	return context;
    }

    public static PackageAdmin getPackageAdmin() {
		if(packageAdmin != null) return packageAdmin;

		synchronized (Activator.class) {
			if(packageAdmin == null) {
				ServiceReference ref = getContext().getServiceReference(PackageAdmin.class.getName());
				packageAdmin = (PackageAdmin) getContext().getService(ref);
			}
			return packageAdmin;
		}
	}

   
}
