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

import org.openmole.plugin.tools.utils.ProcessUtils._
import org.openmole.core.implementation.data.Prototype
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IPrototype
import org.openmole.core.implementation.tools.VariableExpansion._
import java.lang.Integer
import scala.collection.JavaConversions._


class SystemExecTask(name: String, 
                     cmd: String, 
                     returnValue: Option[IPrototype[Integer]], 
                     exceptionIfReturnValueNotZero: Boolean,
                     relativeDir: String) extends AbstractSystemExecTask(name,cmd,returnValue,exceptionIfReturnValueNotZero,relativeDir) {
  
  def this(name: String, cmd: String) = this(name, cmd, None, true, "")
  
  def this(name: String, cmd: String, relativeDir: String) = this(name, cmd, None, true, relativeDir)
  
  def this(name: String, cmd: String, exceptionIfReturnValueNotZero: Boolean) = this(name, cmd, None, exceptionIfReturnValueNotZero, "")
  
  def this(name: String, cmd: String, relativeDir: String,  exceptionIfReturnValueNotZero: Boolean) = this(name, cmd, None, exceptionIfReturnValueNotZero, relativeDir)
  
  def this(name: String, cmd: String, returnValue: Prototype[Integer]) = this(name, cmd, Some(returnValue), false, "")
  
  def this(name: String, cmd: String, relativeDir: String, returnValue: Prototype[Integer]) = this(name, cmd, Some(returnValue), false, relativeDir)
  
  override protected def execute(process: Process, context: IContext): Integer = executeProcess(process,System.out,System.err)
  
}
