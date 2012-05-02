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
import org.openmole.core.model.task.IPluginSet
import org.openmole.misc.tools.io.StringBuilderOutputStream
import org.openmole.core.implementation.data.DataSet
import org.openmole.core.implementation.data.Prototype
import org.openmole.core.implementation.data.Variable
import org.openmole.core.model.data.IContext
import java.io.PrintStream
import org.openmole.plugin.task.external.ExternalTaskBuilder

object SystemExecToStringTask {

  def apply(
    name: String,
    cmd: String,
    out: IPrototype[String],
    err: IPrototype[String],
    dir: String = "",
    exceptionIfReturnValueNotZero: Boolean = true,
    returnValue: Option[IPrototype[Int]] = None)(implicit plugins: IPluginSet) = new ExternalTaskBuilder { builder ⇒
    def toTask = new SystemExecToStringTask(name, cmd, out, err, dir, exceptionIfReturnValueNotZero, returnValue) {
      val inputs = builder.inputs
      val outputs = builder.outputs + DataSet(returnValue) + out + err
      val parameters = builder.parameters
      val provided = builder.provided
      val produced = builder.produced
    }
  }

}

sealed abstract class SystemExecToStringTask(
    val name: String,
    val cmd: String,
    out: IPrototype[String],
    err: IPrototype[String],
    val dir: String,
    val exceptionIfReturnValueNotZero: Boolean,
    val returnValue: Option[IPrototype[Int]])(implicit val plugins: IPluginSet) extends AbstractSystemExecTask {

  override protected def execute(process: Process, context: IContext) = {
    val outStringBuilder = new StringBuilder
    val errStringBuilder = new StringBuilder

    val ret = executeProcess(process, new PrintStream(new StringBuilderOutputStream(outStringBuilder)), new PrintStream(new StringBuilderOutputStream(errStringBuilder)))
    (ret, List(new Variable(out, outStringBuilder.toString), new Variable(err, errStringBuilder.toString)))
  }
}
