/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
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

package org.openmole.plugin.task.systemexec

import org.openmole.core.model.execution.IProgress
import org.openmole.core.model.job.IContext
import org.openmole.core.model.task.annotations.Resource
import org.openmole.plugin.resource.virtual.VirtualMachineResource
import org.openmole.plugin.task.external.ExternalVirtualTask
import org.openmole.plugin.task.systemexec.internal.Activator._

import scala.collection.JavaConversions._

class VirtualSystemExecTask(name: String, virtualMachineResourceArg: VirtualMachineResource, val cmd: String, relativeDir: String) extends ExternalVirtualTask(name, relativeDir) {

  def this(name: String, virtualMachineResourceArg: VirtualMachineResource, cmd: String) {
    this(name, virtualMachineResourceArg, cmd, "")
  }
  
  @Resource
  val virtualMachineResource: VirtualMachineResource = virtualMachineResourceArg

  override protected def process(global: IContext, context: IContext, progress: IProgress) {
    val pool = virtualMachineResource.getVirtualMachineShared
    val virtualMachine = pool.borrowAVirtualMachine
   
    try {
       execute(global, context, progress, cmd, virtualMachine, virtualMachineResource.user, virtualMachineResource.password)
    } finally {
       pool.returnVirtualMachine(virtualMachine)
    }
  }
}
