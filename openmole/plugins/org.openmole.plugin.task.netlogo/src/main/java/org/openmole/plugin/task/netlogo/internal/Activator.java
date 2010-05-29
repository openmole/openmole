/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openmole.plugin.task.netlogo.internal;

import org.openmole.misc.workspace.IWorkspace;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@iscpif.fr>
 */
public class Activator implements BundleActivator {

    private static BundleContext context;
    private static IWorkspace workspace;

    @Override
    public void start(BundleContext context) throws Exception {
        this.context = context;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        this.context = null;
    }

    public synchronized static IWorkspace getWorkspace() {
        if (workspace == null) {
            ServiceReference ref = getContext().getServiceReference(IWorkspace.class.getName());
            workspace = (IWorkspace) getContext().getService(ref);
        }
        return workspace;
    }

    private static BundleContext getContext() {
        return context;
    }
}
