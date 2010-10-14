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

package org.openmole.plugin.environment.jsaga;


import java.util.Map;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.core.implementation.execution.batch.BatchEnvironment;
import org.openmole.misc.workspace.ConfigurationLocation;
import org.openmole.plugin.environment.jsaga.internal.Activator;
import static org.openmole.plugin.environment.jsaga.JSAGAAttributes.*;


public abstract class JSAGAEnvironment extends BatchEnvironment<JSAGAJobService> {
    
    final static ConfigurationLocation DefaultRequieredMemory  = new ConfigurationLocation(JSAGAEnvironment.class.getSimpleName(), "RequieredMemory");

    static  {
        Activator.getWorkspace().addToConfigurations(DefaultRequieredMemory, "1024");
    }

    final private Map<String, String> attributes;

    public JSAGAEnvironment(Map<String, String> attributes) throws InternalProcessingError {
        super();    
        initDefault(attributes);
        this.attributes = attributes;
    }

    public JSAGAEnvironment(int requieredMemory, Map<String, String> attributes) throws InternalProcessingError {
        super(requieredMemory);       
        initDefault(attributes);
        this.attributes = attributes;
    }
    
    private void initDefault(Map<String, String> attributes) throws InternalProcessingError {
        if(!attributes.containsKey(MEMORY)) attributes.put(MEMORY, Activator.getWorkspace().getPreference(DefaultRequieredMemory));
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }
    
    abstract public IJSAGALaunchingScript getLaunchingScript();
}
