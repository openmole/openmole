/*
 * Copyright (C) 2010 reuillon
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
import org.openmole.core.model.data.DataModeMask._
import org.openmole.core.model.data._
import org.openmole.core.model.task._
import org.openmole.core.implementation.data.Context._
import org.openmole.misc.pluginmanager._
import org.openmole.misc.tools.service.Random
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.misc.workspace.Workspace
import org.openmole.misc.tools.service.Random._

object Task {
  val OpenMOLEVariablePrefix = new ConfigurationLocation("Task", "OpenMOLEVariablePrefix")
  Workspace += (OpenMOLEVariablePrefix, "oM")

  val openMOLESeed = new Prototype[Long](Workspace.preference(OpenMOLEVariablePrefix) + "Seed")

  def buildRNG(context: IContext) = newRNG(context.valueOrException(Task.openMOLESeed))
}

trait Task extends ITask {

  def inputs: IDataSet
  def outputs: IDataSet
  def parameters: IParameterSet
  def plugins: IPluginSet

  protected def verifyInput(context: IContext) = {
    for (d ← inputs) {
      if (!(d.mode is optional)) {
        val p = d.prototype
        context.variable(p.name) match {
          case None ⇒ throw new UserBadDataError("Input data named \"" + p.name + "\" of type \"" + p.`type`.toString + "\" required by the task \"" + name + "\" has not been found");
          case Some(v) ⇒ if (!p.isAssignableFrom(v.prototype)) throw new UserBadDataError("Input data named \"" + p.name + "\" required by the task \"" + name + "\" has the wrong type: \"" + v.prototype.`type`.toString + "\" instead of \"" + p.`type`.toString + "\" required")
        }
      }
    }
    context
  }

  protected def filterOutput(context: IContext): IContext =
    outputs.flatMap {
      d ⇒
        val p = d.prototype
        context.variable(p) match {
          case None ⇒
            if (!(d.mode is optional)) throw new UserBadDataError("Variable " + p.name + " of type " + p.`type`.toString + " in not optional and has not found in output of task " + name + ".")
            else Option.empty[IVariable[_]]
          case Some(v) ⇒
            if (p.accepts(v.value)) Some(v)
            else throw new UserBadDataError("Output value of variable " + p.name + " (prototype: " + v.prototype.`type`.toString + ") is instance of class '" + v.value.asInstanceOf[AnyRef].getClass + "' and doesn't match the expected class '" + p.`type`.toString + "' in task" + name + ".")
        }
    }.toContext

  private def init(context: IContext): IContext = {
    if (PluginManagerInfo.enabled) PluginManager.loadIfNotAlreadyLoaded(plugins.toIterable)
    else if (!plugins.isEmpty) throw new InternalProcessingError("Plugins can't be loadded cause the application isn't run in an osgi environment.")

    verifyInput(
      context ++
        parameters.flatMap {
          parameter ⇒
            if (parameter.`override` || !context.contains(parameter.variable.prototype.name)) Some(parameter.variable)
            else Option.empty[IVariable[_]]
        })
  }

  private def end(context: IContext) = filterOutput(context)

  /**
   * The main operation of the processor.
   * @param context
   * @param progress
   */
  @throws(classOf[Throwable])
  protected def process(context: IContext): IContext

  /* (non-Javadoc)
   * @see org.openmole.core.processors.ITask#run(org.openmole.core.processors.ApplicativeContext)
   */
  override def perform(context: IContext) = {
    try end(context + process(init(context)))
    catch {
      case e ⇒ throw new InternalProcessingError(e, "Error in task " + name + " for context values " + context)
    }
  }

  override def toString: String = name

}
