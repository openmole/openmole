/*
 * Copyright (C) 2010 mathieu leclaire <mathieu.leclaire@openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.task.systemexec

import org.openmole.core.model.data.IPrototype
import org.openmole.misc.tools.service.ProcessUtil._
import org.openmole.misc.tools.io.StringBuilderOutputStream
import org.openmole.core.implementation.data.Prototype
import org.openmole.core.implementation.data.Variable
import org.openmole.core.model.data.IContext
import java.io.PrintStream

class SystemExecToStringTask(
  val name: String, 
  val cmd: String, 
  val returnValue: Option[Prototype[Int]], 
  val exceptionIfReturnValueNotZero: Boolean,
  val relativeDir: String,
  val outString: IPrototype[String],
  val errString: IPrototype[String]) extends AbstractSystemExecTask {
  
  
  def this(name: String, cmd: String, outString: IPrototype[String], errString: IPrototype[String]) = {
    this(name, cmd, None, true, "", outString, errString)
  }
  
  def this(name: String, cmd: String, relativeDir: String, outString: IPrototype[String], errString: IPrototype[String]) = {
    this(name, cmd, None, true, relativeDir, outString, errString)
  }
  
  def this(name: String, cmd: String, exceptionIfReturnValueNotZero: Boolean, outString: IPrototype[String], errString: IPrototype[String]) = {
    this(name, cmd, None, exceptionIfReturnValueNotZero, "", outString, errString)
  }
  
  def this(name: String, cmd: String, relativeDir: String,  exceptionIfReturnValueNotZero: Boolean, outString: IPrototype[String], errString: IPrototype[String]) = {
    this(name, cmd, None, exceptionIfReturnValueNotZero, relativeDir, outString, errString)
  }
  
  def this(name: String, cmd: String, returnValue: Prototype[Int], outString: IPrototype[String], errString: IPrototype[String]) = {
    this(name, cmd, Some(returnValue), false, "", outString, errString)
  }
  
  def this(name: String, cmd: String, relativeDir: String, returnValue: Prototype[Int], outString: IPrototype[String], errString: IPrototype[String]) = {
    this(name, cmd, Some(returnValue), false, relativeDir, outString, errString)
  }

  addOutput(outString)
  addOutput(errString)
  
  override protected def execute(process: Process, context: IContext) = {    
    val outStringBuilder = new StringBuilder
    val errStringBuilder = new StringBuilder
    
    val ret = executeProcess(process,new PrintStream(new StringBuilderOutputStream(outStringBuilder)),new PrintStream(new StringBuilderOutputStream(errStringBuilder)))
    (ret, List(new Variable(outString, outStringBuilder.toString), new Variable(errString,errStringBuilder.toString) ))
  }
}
