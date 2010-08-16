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

package org.openmole.misc.hashservice.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.openmole.misc.executorservice.IExecutorService;
import org.openmole.misc.hashservice.IHashService;

/**
 *
 * @author reuillon
 */
public class Activator implements BundleActivator {

    private static BundleContext context;
    private static IExecutorService executorService;
    private ServiceRegistration regExecutor;

    @Override
    public void start(BundleContext context) throws Exception {
        this.context = context;
        regExecutor = context.registerService(IHashService.class.getName(), new SHA1Computing(), null);
    }

    @Override
    public void stop(BundleContext arg0) throws Exception {
        regExecutor.unregister();
    }

    public static BundleContext getContext() {
        return context;
    }

    public static IExecutorService getExecutorService() {
        if (executorService == null) {
            ServiceReference ref = getContext().getServiceReference(IExecutorService.class.getName());
            executorService = (IExecutorService) getContext().getService(ref);
        }
        return executorService;
    }
}
