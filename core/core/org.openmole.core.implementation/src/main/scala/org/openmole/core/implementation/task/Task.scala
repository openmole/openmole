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
import org.openmole.core.implementation.tools.InputOutputCheck
import util.{ Try, Success, Failure }

object Task extends Logger {
  val OpenMOLEVariablePrefix = new ConfigurationLocation("Task", "OpenMOLEVariablePrefix")

  Workspace += (OpenMOLEVariablePrefix, "oM")

  val openMOLESeed = Prototype[Long](Workspace.preference(OpenMOLEVariablePrefix) + "Seed")

  def buildRNG(context: Context) = newRNG(context(Task.openMOLESeed))
}

trait Task extends ITask with InputOutputCheck {

  protected def process(context: Context): Context

  def perform(context: Context) = perform(context, process)

  override def toString: String = name
}
