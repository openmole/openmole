/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
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

package org.openmole.plugin.resource.virtual;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.misc.workspace.ConfigurationLocation;
import static org.openmole.plugin.resource.virtual.internal.Activator.*;


/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public abstract class AbstractVirtualMachinePool implements IVirtualMachinePool {

  final static String Group = IVirtualMachinePool.class.getSimpleName();
  final static ConfigurationLocation unusedVMKeepOn = new ConfigurationLocation(Group, "UnusedVMKeepOn");

  static {
    workspace().addToConfigurations(unusedVMKeepOn, "PT2M");
  }

  long delay() throws InternalProcessingError {
      return workspace().getPreferenceAsDurationInMs(unusedVMKeepOn);
  }

  private final VirtualMachineResource resource;

    public AbstractVirtualMachinePool(VirtualMachineResource resource) {
        this.resource = resource;
    }

    public VirtualMachineResource resource() {
        return resource;
    }


}
