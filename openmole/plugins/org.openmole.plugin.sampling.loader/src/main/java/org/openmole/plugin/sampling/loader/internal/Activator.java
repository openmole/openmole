/*
 * Copyright (C) 2010 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.sampling.loader.internal;

import org.openmole.core.serializer.ISerializer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 *
 * @author reuillon
 */
public class Activator implements BundleActivator {
    
    private static BundleContext context;
    private static ISerializer messageSerializer;
    
    @Override
    public void start(BundleContext bc) throws Exception {
        context = bc;
    }

    @Override
    public void stop(BundleContext bc) throws Exception {
        context = null;
    }
    
    public synchronized static ISerializer getSerializer() {
        if (messageSerializer == null) {
            ServiceReference ref = getContext().getServiceReference(ISerializer.class.getName());
            messageSerializer = (ISerializer) getContext().getService(ref);
        }
        return messageSerializer;
    }

    public static BundleContext getContext() {
        return context;
    }
   
}
