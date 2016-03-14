/*
 * Copyright (C) 2012 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.task.netlogo5

import org.openmole.core.tools.service.OS
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.tools.ExpandedString
import org.openmole.plugin.task.external.ExternalTask.{ Resource, OutputFile, InputFile }
import org.openmole.plugin.task.netlogo._
import org.openmole.plugin.task.external._
import NetLogoTask.Workspace
import org.openmole.core.workflow.task._
import java.io.File
import collection.JavaConversions._
import org.openmole.plugin.tool.netlogo5.NetLogo5

object NetLogo5Task {

  def factory = new NetLogoFactory {
    def apply = new NetLogo5
  }

  def workspace(
    workspace:         File,
    script:            String,
    launchingCommands: Seq[String],
    seed:              Option[Prototype[Int]]
  ): NetLogoTaskBuilder = {

    new NetLogoTaskBuilder(new Workspace(workspace, script), launchingCommands, seed, factory) { builder ⇒
      addResource(workspace)
      def toTask = new NetLogo5Task with Built
    }
  }

  def file(
    script:            File,
    launchingCommands: Seq[String],
    seed:              Option[Prototype[Int]] = None
  ): NetLogoTaskBuilder = {
    new NetLogoTaskBuilder(new Workspace(script), launchingCommands, seed, factory) {
      builder ⇒

      addResource(script)

      def toTask = new NetLogo5Task with Built
    }
  }

  def apply(
    script:            File,
    launchingCommands: Seq[String],
    embedWorkspace:    Boolean                = false,
    seed:              Option[Prototype[Int]] = None
  ): NetLogoTaskBuilder =
    if (embedWorkspace) workspace(script.getCanonicalFile.getParentFile, script.getName, launchingCommands, seed)
    else file(script, launchingCommands, seed)

}

trait NetLogo5Task extends NetLogoTask

