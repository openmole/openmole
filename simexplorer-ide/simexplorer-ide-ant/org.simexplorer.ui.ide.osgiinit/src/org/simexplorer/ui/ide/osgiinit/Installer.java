/*
 *  Copyright (C) 2010 Cemagref
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

package org.simexplorer.ui.ide.osgiinit;

import java.util.logging.Logger;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.openmole.core.structuregenerator.IStructureGenerator;

/**
 *
 * @author Nicolas Dumoulin <nicolas.dumoulin@cemagref.fr>
 */
public class Installer implements BundleActivator {
    private static IStructureGenerator structureGenerator;
    private static BundleContext bc;

    @Override
    public void start(BundleContext c) throws Exception {
        Logger.getLogger(Installer.class.getName()).fine("Bundle context has been initialized");
        bc = c;
    }

    @Override
    public void stop(BundleContext c) throws Exception {
    }

    public synchronized static IStructureGenerator getStructureGenerator() {
        if (structureGenerator == null) {
            ServiceReference ref = bc.getServiceReference(IStructureGenerator.class.getName());
            structureGenerator = (IStructureGenerator) bc.getService(ref);
        }
        return structureGenerator;
    }
}
