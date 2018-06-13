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

import java.io.File

import monocle.macros.Lenses
import org.openmole.core.context.Val
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.dsl._
import org.openmole.plugin.task.external._
import org.openmole.plugin.task.netlogo.NetLogoTask.Workspace
import org.openmole.plugin.task.netlogo._
import org.openmole.plugin.tool.netlogo5._
import org.openmole.core.expansion._

object NetLogo5Task {

  def factory = new NetLogoFactory {
    def apply = new NetLogo5
  }

  implicit def isTask: InputOutputBuilder[NetLogo5Task] = InputOutputBuilder(NetLogo5Task.config)
  implicit def isExternal: ExternalBuilder[NetLogo5Task] = ExternalBuilder(NetLogo5Task.external)
  implicit def isInfo = InfoBuilder(info)

  implicit def isBuilder = new NetLogoTaskBuilder[NetLogo5Task] {
    override def netLogoInputs = NetLogo5Task.netLogoInputs
    override def netLogoArrayInputs = NetLogo5Task.netLogoArrayInputs
    override def netLogoArrayOutputs = NetLogo5Task.netLogoArrayOutputs
    override def netLogoOutputs = NetLogo5Task.netLogoOutputs
  }

  def workspace(
    workspace:         File,
    script:            String,
    launchingCommands: Seq[FromContext[String]],
    seed:              OptionalArgument[Val[Int]],
    ignoreError:       Boolean,
    reuseWorkspace:    Boolean
  )(implicit name: sourcecode.Name, definitionScope: DefinitionScope): NetLogo5Task =
    withDefaultArgs(
      workspace = Workspace.Directory(directory = workspace, script = script, name = workspace.getName),
      launchingCommands = launchingCommands,
      seed = seed,
      ignoreError = ignoreError,
      reuseWorkspace = reuseWorkspace
    ) set (
        inputs += (seed.option.toSeq: _*)
      )

  def file(
    script:            File,
    launchingCommands: Seq[FromContext[String]],
    seed:              OptionalArgument[Val[Int]],
    ignoreError:       Boolean,
    reuseWorkspace:    Boolean
  )(implicit name: sourcecode.Name, definitionScope: DefinitionScope): NetLogo5Task =
    withDefaultArgs(
      workspace = Workspace.Script(script = script, name = script.getName),
      launchingCommands = launchingCommands,
      seed = seed,
      ignoreError = ignoreError,
      reuseWorkspace = reuseWorkspace
    ) set (
        inputs += (seed.option.toSeq: _*)
      )

  def apply(
    script:            File,
    launchingCommands: Seq[FromContext[String]],
    embedWorkspace:    Boolean                    = false,
    seed:              OptionalArgument[Val[Int]] = None,
    ignoreError:       Boolean                    = false,
    reuseWorkspace:    Boolean                    = false
  )(implicit name: sourcecode.Name, definitionScope: DefinitionScope): NetLogo5Task =
    if (embedWorkspace) workspace(script.getCanonicalFile.getParentFile, script.getName, launchingCommands, seed = seed, ignoreError = ignoreError, reuseWorkspace = reuseWorkspace)
    else file(script, launchingCommands, seed = seed, ignoreError = ignoreError, reuseWorkspace = reuseWorkspace)

  private def withDefaultArgs(
    workspace:         NetLogoTask.Workspace,
    launchingCommands: Seq[FromContext[String]],
    seed:              Option[Val[Int]],
    ignoreError:       Boolean,
    reuseWorkspace:    Boolean
  )(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    NetLogo5Task(
      config = InputOutputConfig(),
      external = External(),
      info = InfoConfig(),
      netLogoInputs = Vector.empty,
      netLogoArrayInputs = Vector.empty,
      netLogoOutputs = Vector.empty,
      netLogoArrayOutputs = Vector.empty,
      workspace = workspace,
      launchingCommands = launchingCommands,
      seed = seed,
      ignoreError = ignoreError,
      reuseWorkspace = reuseWorkspace
    )

}

@Lenses case class NetLogo5Task(
  config:              InputOutputConfig,
  external:            External,
  info:                InfoConfig,
  netLogoInputs:       Vector[(Val[_], String)],
  netLogoArrayInputs: Vector[(Val[_], String)],
  netLogoOutputs:      Vector[(String, Val[_])],
  netLogoArrayOutputs: Vector[(String, Int, Val[_])],
  workspace:           NetLogoTask.Workspace,
  launchingCommands:   Seq[FromContext[String]],
  seed:                Option[Val[Int]],
  ignoreError:         Boolean,
  reuseWorkspace:      Boolean
) extends NetLogoTask {
  override def netLogoFactory: NetLogoFactory = NetLogo5Task.factory
}

