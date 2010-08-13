/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
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

package org.openmole.plugin.task.systemexec.internal;

import org.openmole.commons.aspect.caching.SoftCachable;
import org.openmole.misc.workspace.IWorkspace;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class Activator implements BundleActivator {
   
    static Activator instance = new Activator();
    
    BundleContext context = null;

    public static IWorkspace workspace() {
        return instance.getWorkspace();
    }

    @SoftCachable
    private IWorkspace getWorkspace()
    {
      ServiceReference ref = context.getServiceReference(IWorkspace.class.getName());
      return (IWorkspace) context.getService(ref);
    }

    @Override
    public void start(BundleContext bc) throws Exception {
        instance.context = bc;
    }

    @Override
    public void stop(BundleContext bc) throws Exception {
        instance.context = null;
    }

}
