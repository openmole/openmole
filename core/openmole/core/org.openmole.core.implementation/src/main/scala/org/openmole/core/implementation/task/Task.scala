/*
 * Copyright (C) 2010 Romain Reuillon
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

package org.openmole.core.implementation.task

import org.openmole.misc.exception._
import org.openmole.core.implementation.data._
import org.openmole.core.model.data._
import org.openmole.core.model.task._
import org.openmole.misc.pluginmanager._
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.tools.service.Random
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.misc.workspace.Workspace
import org.openmole.misc.tools.service.Random._

object Task extends Logger {
  val OpenMOLEVariablePrefix = new ConfigurationLocation("Task", "OpenMOLEVariablePrefix")
  val ErrorArraySnipSize = new ConfigurationLocation("Task", "ErrorArraySnipSize")

  Workspace += (OpenMOLEVariablePrefix, "oM")
  Workspace += (ErrorArraySnipSize, "10")

  val openMOLESeed = Prototype[Long](Workspace.preference(OpenMOLEVariablePrefix) + "Seed")

  def buildRNG(context: Context) = newRNG(context(Task.openMOLESeed))
}

trait Task extends ITask {

  protected def verifyInput(context: Context) = {
    for (d ← inputs) {
      if (!(d.mode is Optional)) {
        val p = d.prototype
        context.variable(p.name) match {
          case None ⇒ throw new UserBadDataError(s"Input data named ${p.name} of type ${p.`type`} required by the task $name has not been found")
          case Some(v) ⇒ if (!p.isAssignableFrom(v.prototype)) throw new UserBadDataError(s"Input data named ${p.name} required by the task $name is of type ${v.prototype.`type`} which is incompatible with the required type ${p.`type`}")
        }
      }
    }
    context
  }

  protected def filterOutput(context: Context): Context =
    outputs.flatMap {
      d ⇒
        val p = d.prototype
        context.variable(p) match {
          case None ⇒
            if (!(d.mode is Optional)) throw new UserBadDataError(s"Variable ${p.name} of type ${p.`type`} is not optional and has not found in output of task $name")
            else Option.empty[Variable[_]]
          case Some(v) ⇒
            if (p.accepts(v.value)) Some(v)
            else throw new UserBadDataError(s"Value of variable ${p.name} (prototype: ${v.prototype.`type`}) is instance of class ${v.value.asInstanceOf[AnyRef].getClass} and doesn't match the expected class ${p.`type`} in output of task $name")
        }
    }.toContext

  private def init(context: Context): Context =
    verifyInput(
      context ++
        parameters.flatMap {
          parameter ⇒
            if (parameter.`override` || !context.contains(parameter.variable.prototype.name)) Some(parameter.variable)
            else Option.empty[Variable[_]]
        })


  private def end(context: Context) = filterOutput(context)

  protected def process(context: Context): Context

  override def perform(context: Context) =
    try end(context + process(init(context)))
    catch {
      case e: Throwable ⇒
        throw new InternalProcessingError(e, s"Error in task $name for context values ${context.prettified(Workspace.preferenceAsInt(Task.ErrorArraySnipSize))}")
    }

  override def toString: String = name

}
