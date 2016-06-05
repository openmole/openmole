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
import org.openmole.plugin.task.netlogo._
import org.openmole.plugin.task.external._
import NetLogoTask.Workspace
import org.openmole.core.workflow.task._
import java.io.File

import org.openmole.core.workflow.dsl._
import monocle.macros.Lenses
import org.openmole.core.workflow.builder._

import collection.JavaConversions._
import org.openmole.plugin.tool.netlogo5.NetLogo5

object NetLogo5Task {

  def factory = new NetLogoFactory {
    def apply = new NetLogo5
  }

  implicit def isTask: InputOutputBuilder[NetLogo5Task] = InputOutputBuilder(NetLogo5Task.config)
  implicit def isExternal: ExternalBuilder[NetLogo5Task] = ExternalBuilder(NetLogo5Task.external)

  implicit def isBuilder = new NetLogoTaskBuilder[NetLogo5Task] {
    override def netLogoInputs = NetLogo5Task.netLogoInputs
    override def netLogoArrayOutputs = NetLogo5Task.netLogoArrayOutputs
    override def netLogoOutputs = NetLogo5Task.netLogoOutputs
  }

  def workspace(
    workspace:         File,
    script:            String,
    launchingCommands: Seq[String],
    seed:              OptionalArgument[Prototype[Int]] = None
  ): NetLogo5Task =
    withDefaultArgs(
      workspace = Workspace(script = script, workspace = workspace.getName),
      launchingCommands = launchingCommands,
      seed = seed
    ) set (
        inputs += (seed.option.toSeq: _*),
        resources += workspace
      )

  def file(
    script:            File,
    launchingCommands: Seq[String],
    seed:              OptionalArgument[Prototype[Int]] = None
  ): NetLogo5Task =
    withDefaultArgs(
      workspace = Workspace(script = script.getName),
      launchingCommands = launchingCommands,
      seed = seed
    ) set (
        inputs += (seed.option.toSeq: _*),
        resources += script
      )

  def apply(
    script:            File,
    launchingCommands: Seq[String],
    embedWorkspace:    Boolean                          = false,
    seed:              OptionalArgument[Prototype[Int]] = None
  ): NetLogo5Task =
    if (embedWorkspace) workspace(script.getCanonicalFile.getParentFile, script.getName, launchingCommands, seed)
    else file(script, launchingCommands, seed)

  private def withDefaultArgs(
    workspace:         NetLogoTask.Workspace,
    launchingCommands: Seq[String],
    seed:              Option[Prototype[Int]]
  ) =
    NetLogo5Task(
      config = InputOutputConfig(),
      external = External(),
      netLogoInputs = Vector.empty,
      netLogoOutputs = Vector.empty,
      netLogoArrayOutputs = Vector.empty,
      workspace = workspace,
      launchingCommands = launchingCommands,
      seed = seed
    )

}

@Lenses case class NetLogo5Task(
    config:              InputOutputConfig,
    external:            External,
    netLogoInputs:       Vector[(Prototype[_], String)],
    netLogoOutputs:      Vector[(String, Prototype[_])],
    netLogoArrayOutputs: Vector[(String, Int, Prototype[_])],
    workspace:           NetLogoTask.Workspace,
    launchingCommands:   Seq[String],
    seed:                Option[Prototype[Int]]
) extends NetLogoTask {
  override def netLogoFactory: NetLogoFactory = NetLogo5Task.factory
}

