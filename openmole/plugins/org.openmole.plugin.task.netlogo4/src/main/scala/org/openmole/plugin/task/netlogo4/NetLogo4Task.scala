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

package org.openmole.plugin.task.netlogo4

import org.openmole.core.tools.service.OS
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.data._
import org.openmole.plugin.task.external._
import org.openmole.plugin.task.netlogo._
import NetLogoTask.Workspace
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.dsl
import org.openmole.core.workflow.dsl._
import org.openmole.plugin.task.external
import java.io.File

import monocle.Lens
import monocle.macros.Lenses
import org.openmole.core.workflow.builder._

import collection.JavaConversions._
import org.openmole.plugin.tool.netlogo4.NetLogo4

object NetLogo4Task {

  def factory = new NetLogoFactory {
    def apply = new NetLogo4
  }

  implicit def isTask: InputOutputBuilder[NetLogo4Task] = InputOutputBuilder(NetLogo4Task.config)
  implicit def isExternal: ExternalBuilder[NetLogo4Task] = ExternalBuilder(NetLogo4Task.external)

  implicit def isNetLogo: NetLogoTaskBuilder[NetLogo4Task] = new NetLogoTaskBuilder[NetLogo4Task] {
    override def netLogoInputs = NetLogo4Task.netLogoInputs
    override def netLogoArrayOutputs = NetLogo4Task.netLogoArrayOutputs
    override def netLogoOutputs = NetLogo4Task.netLogoOutputs
  }

  def workspace(
    workspace:         File,
    script:            String,
    launchingCommands: Seq[String],
    seed:              Option[Prototype[Int]]
  ): NetLogo4Task =
    withDefaultArgs(
      workspace = Workspace(script = script, workspace = Some(workspace.getName)),
      launchingCommands = launchingCommands,
      seed = seed
    ) set (
        inputs += (seed.toSeq: _*),
        resources += workspace
      )

  def file(
    script:            File,
    launchingCommands: Seq[String],
    seed:              Option[Prototype[Int]] = None
  ): NetLogo4Task =
    withDefaultArgs(
      workspace = Workspace(script = script.getName),
      launchingCommands = launchingCommands,
      seed = seed
    ) set (
        inputs += (seed.toSeq: _*),
        resources += script
      )

  def apply(
    script:            File,
    launchingCommands: Seq[String],
    embedWorkspace:    Boolean                = false,
    seed:              Option[Prototype[Int]] = None
  ): NetLogo4Task =
    if (embedWorkspace) workspace(script.getCanonicalFile.getParentFile, script.getName, launchingCommands, seed)
    else file(script, launchingCommands, seed)

  private def withDefaultArgs(
    workspace:         NetLogoTask.Workspace,
    launchingCommands: Seq[String],
    seed:              Option[Prototype[Int]]
  ) =
    NetLogo4Task(
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

@Lenses case class NetLogo4Task(
    config:              InputOutputConfig,
    external:            External,
    netLogoInputs:       Vector[(Prototype[_], String)],
    netLogoOutputs:      Vector[(String, Prototype[_])],
    netLogoArrayOutputs: Vector[(String, Int, Prototype[_])],
    workspace:           NetLogoTask.Workspace,
    launchingCommands:   Seq[String],
    seed:                Option[Prototype[Int]]
) extends NetLogoTask {
  override def netLogoFactory: NetLogoFactory = NetLogo4Task.factory
}

