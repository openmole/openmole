/*
 *
 *  Copyright (c) 2010, Cemagref
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation; either version 3 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston,
 *  MA  02110-1301  USA
 */
package org.simexplorer.openmole.plugin.task.sensitivity.internal;

import org.simexplorer.openmole.plugin.task.sensitivity.R;
import org.openmole.ui.console.IConsole;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 *
 * @author dumoulin
 */
public class Activator implements BundleActivator {

    @Override
    public void start(BundleContext bc) throws Exception {
        ServiceReference serviceReference = bc.getServiceReference(IConsole.class.getName());
        if (serviceReference != null) {
            ((IConsole) bc.getService(serviceReference)).setVariable("R", R.getInstance());
        } else {
            System.out.println("No service found for IConsole");
        }
    }

    @Override
    public void stop(BundleContext bc) throws Exception {
    }
}
