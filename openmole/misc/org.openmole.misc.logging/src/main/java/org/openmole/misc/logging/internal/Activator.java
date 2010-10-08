/*
 *  Copyright (C) 2010 reuillon
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
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

package org.openmole.misc.logging.internal;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import org.openmole.misc.workspace.ConfigurationLocation;
import org.openmole.misc.workspace.IWorkspace;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 *
 * @author reuillon
 */
public class Activator implements BundleActivator {

    final static ConfigurationLocation LevelConfiguration = new ConfigurationLocation("Logging", "Level");
    
    private static IWorkspace workspace;
    private static BundleContext context;


    @Override
    public void start(BundleContext bc) throws Exception {
        context = bc;
        
        getWorkspace().addToConfigurations(LevelConfiguration, Level.INFO.toString());
        
        java.util.logging.Logger rootLogger = LogManager.getLogManager().getLogger("");  
        Handler[] handlers = rootLogger.getHandlers();  
        for (int i = 0; i < handlers.length; i++) {  
            rootLogger.removeHandler(handlers[i]);  
        }
        
        SLF4JBridgeHandler.install();
        
        String configuredLevel = getWorkspace().getPreference(LevelConfiguration);

        for (int i = 0; i < handlers.length; i++) {  
            rootLogger.setLevel(Level.parse(configuredLevel)); 
        }     
    }

    @Override
    public void stop(BundleContext bc) throws Exception {

    }

    private static BundleContext getContext() {
        return context;
    }
    
    public static IWorkspace getWorkspace() {
        if (workspace != null) {
            return workspace;
        }

        synchronized (Activator.class) {
            if (workspace == null) {
                ServiceReference ref = getContext().getServiceReference(IWorkspace.class.getName());
                workspace = (IWorkspace) getContext().getService(ref);
            }
        }
        return workspace;
    }
    
    
}
