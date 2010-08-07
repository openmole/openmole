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
import org.openmole.misc.workspace.ConfigurationLocation;
import org.openmole.plugin.environment.jsaga.internal.Activator;


public abstract class JSAGAEnvironment extends BatchEnvironment<JSAGAJobService> {

    final static ConfigurationLocation DefaultRequieredCPUTime  = new ConfigurationLocation(JSAGAEnvironment.class.getSimpleName(), "RequieredCPUTime");
    final static ConfigurationLocation DefaultRequieredMemory  = new ConfigurationLocation(JSAGAEnvironment.class.getSimpleName(), "RequieredMemory");

    static  {
        Activator.getWorkspace().addToConfigurations(DefaultRequieredCPUTime, "PT12H");
        Activator.getWorkspace().addToConfigurations(DefaultRequieredMemory, "1024");
    }

    final private int requieredMemory;
    final private String requieredCPUTime;

    public JSAGAEnvironment(int requieredMemory, String requieredCPUTime) throws InternalProcessingError {
        super();
        this.requieredMemory = requieredMemory;
        this.requieredCPUTime = requieredCPUTime;
    }

    public JSAGAEnvironment(int requieredMemory) throws InternalProcessingError {
        this(requieredMemory, Activator.getWorkspace().getPreference(DefaultRequieredCPUTime));
    }

    public JSAGAEnvironment(String requieredCPUTime) throws InternalProcessingError {
        this(Activator.getWorkspace().getPreferenceAsInt(DefaultRequieredMemory), requieredCPUTime);
    }

    public JSAGAEnvironment() throws InternalProcessingError {
        this(Activator.getWorkspace().getPreferenceAsInt(DefaultRequieredMemory), Activator.getWorkspace().getPreference(DefaultRequieredCPUTime));
    }

    public JSAGAEnvironment(int requieredMemory, int memoryForRuntime, String requieredCPUTime) throws InternalProcessingError {
        super(memoryForRuntime);
        this.requieredMemory = requieredMemory;
        this.requieredCPUTime = requieredCPUTime;
    }

    public JSAGAEnvironment(int requieredMemory, int memoryForRuntime) throws InternalProcessingError {
        this(requieredMemory, memoryForRuntime, Activator.getWorkspace().getPreference(DefaultRequieredCPUTime));
    }

    public int getRequieredCPUTime() {
        return ISOPeriodFormat.standard().parsePeriod(requieredCPUTime).toStandardSeconds().getSeconds();
    }

    public int getRequieredMemory() {
        return requieredMemory;
    }

    abstract public IJSAGALaunchingScript getLaunchingScript();
}
