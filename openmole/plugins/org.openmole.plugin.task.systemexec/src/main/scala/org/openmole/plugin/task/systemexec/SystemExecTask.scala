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
 *  GNU Affero General Public License for more details.
 * 
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.task.systemexec

import org.openmole.core.model.data._
import org.openmole.core.model.task.IPluginSet
import org.openmole.misc.tools.service.ProcessUtil._
import org.openmole.core.implementation.data._
import org.openmole.core.implementation.tools.VariableExpansion._
import org.openmole.plugin.task.external.ExternalTaskBuilder
import scala.collection.JavaConversions._

object SystemExecTask {
  
  def apply(
    name: String, 
    cmd: String, 
    dir: String = "",
    exceptionIfReturnValueNotZero: Boolean = true,
    returnValue: Option[IPrototype[Int]] = None
  )(implicit plugins: IPluginSet) = new ExternalTaskBuilder { builder =>
    def toTask = new SystemExecTask(name, cmd, dir, exceptionIfReturnValueNotZero, returnValue) {
      val inputs = builder.inputs 
      val outputs: IDataSet = builder.outputs ++ DataSet(returnValue)
      val parameters = builder.parameters
      val provided = builder.provided
      val produced = builder.produced
    }
  }
  
}

sealed abstract class SystemExecTask(
  val name: String, 
  val cmd: String, 
  val dir: String,
  val exceptionIfReturnValueNotZero: Boolean,
  val returnValue: Option[IPrototype[Int]]
)(implicit val plugins: IPluginSet) extends AbstractSystemExecTask {
  override protected def execute(process: Process, context: IContext) = executeProcess(process,System.out,System.err) -> List.empty[IVariable[_]]
}
