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

package org.openmole.plugin.environment.jsaga;


import org.joda.time.format.ISOPeriodFormat;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.core.implementation.execution.batch.BatchEnvironment;
import org.openmole.core.model.execution.batch.IBatchEnvironmentDescription;
import org.openmole.misc.workspace.ConfigurationLocation;
import org.openmole.plugin.environment.jsaga.internal.Activator;


public abstract class JSAGAEnvironment extends BatchEnvironment<JSAGAJobService> {

    final static ConfigurationLocation CPUTime  = new ConfigurationLocation(JSAGAEnvironment.class.getSimpleName(), "CPUTime");
    final static ConfigurationLocation Memory  = new ConfigurationLocation(JSAGAEnvironment.class.getSimpleName(), "Memory");

    static  {
        Activator.getWorkspace().addToConfigurations(CPUTime, "PT12H");
        Activator.getWorkspace().addToConfigurations(Memory, "800");
    }

    int memoryVal;
    String CPUTimeVal;

    public JSAGAEnvironment(IBatchEnvironmentDescription description) throws InternalProcessingError {
        super(description);
        this.memoryVal = Activator.getWorkspace().getPreferenceAsInt(Memory);
        this.CPUTimeVal = Activator.getWorkspace().getPreference(CPUTime);
    }

    public int getCPUTime() {
        return ISOPeriodFormat.standard().parsePeriod(CPUTimeVal).toStandardSeconds().getSeconds();
    }

    public int getMemory() {
        return memoryVal;
    }

    abstract public IJSAGALaunchingScript getLaunchingScript();
}
