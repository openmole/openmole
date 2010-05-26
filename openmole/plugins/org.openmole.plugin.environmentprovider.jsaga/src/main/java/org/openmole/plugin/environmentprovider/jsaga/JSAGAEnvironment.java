/*
 *  Copyright (C) 2010 reuillon
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

package org.openmole.plugin.environmentprovider.jsaga;


import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.core.workflow.implementation.execution.batch.BatchEnvironment;
import org.openmole.core.workflow.model.execution.batch.IBatchEnvironmentDescription;
import org.openmole.misc.workspace.ConfigurationLocation;
import org.openmole.plugin.environmentprovider.jsaga.internal.Activator;
import org.openmole.plugin.environmentprovider.jsaga.model.IJSAGAEnvironment;
import org.openmole.plugin.environmentprovider.jsaga.model.IJSAGAJobService;

public abstract class JSAGAEnvironment<DESC extends IBatchEnvironmentDescription>  extends BatchEnvironment<IJSAGAJobService, DESC> implements IJSAGAEnvironment<DESC> {

    final public static ConfigurationLocation CPUTime  = new ConfigurationLocation(JSAGAEnvironment.class.getSimpleName(), "CPUTime");

    static  {
        Activator.getWorkspace().addToConfigurations(CPUTime, Long.toString(12 * 60 * 60));
    }
 

    public JSAGAEnvironment(DESC description) throws InternalProcessingError {
        super(description);
    }
}
