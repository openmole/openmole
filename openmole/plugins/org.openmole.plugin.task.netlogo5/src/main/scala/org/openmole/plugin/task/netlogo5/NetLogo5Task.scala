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

import monocle.Focus
import org.openmole.core.context.Val
import org.openmole.core.setter._
import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import org.openmole.plugin.task.external._
import org.openmole.plugin.task.netlogo.AbstractNetLogoTask.Workspace
import org.openmole.plugin.task.netlogo._
import org.openmole.plugin.tool.netlogo5._
import org.openmole.core.argument._

object NetLogo5Task {

  def factory = new NetLogoFactory {
    def apply() = new NetLogo5
  }

  implicit def isTask: InputOutputBuilder[NetLogo5Task] = InputOutputBuilder(Focus[NetLogo5Task](_.config))
  implicit def isExternal: ExternalBuilder[NetLogo5Task] = ExternalBuilder(Focus[NetLogo5Task](_.external))
  implicit def isInfo: InfoBuilder[NetLogo5Task] = InfoBuilder(Focus[NetLogo5Task](_.info))
  implicit def isMapped: MappedInputOutputBuilder[NetLogo5Task] = MappedInputOutputBuilder(Focus[NetLogo5Task](_.mapped))

  def workspace(
    workspace:            File,
    script:               String,
    go:                   Seq[FromContext[String]],
    setup:                Seq[FromContext[String]],
    seed:                 OptionalArgument[Val[Int]],
    ignoreError:          Boolean,
    reuseWorkspace:       Boolean,
    ignoreErrorOnDispose: Boolean,
    switch3d:             Boolean
  )(implicit name: sourcecode.Name, definitionScope: DefinitionScope): NetLogo5Task =
    withDefaultArgs(
      workspace = Workspace.Directory(directory = workspace, script = script, name = workspace.getName),
      go = go,
      setup = setup,
      seed = seed,
      ignoreError = ignoreError,
      reuseWorkspace = reuseWorkspace,
      ignoreErrorOnDispose = ignoreErrorOnDispose,
      switch3d = switch3d
    ) set (
      inputs ++= seed.option.toSeq
    )

  def file(
    script:               File,
    go:                   Seq[FromContext[String]],
    setup:                Seq[FromContext[String]],
    seed:                 OptionalArgument[Val[Int]],
    ignoreError:          Boolean,
    reuseWorkspace:       Boolean,
    ignoreErrorOnDispose: Boolean,
    switch3d:             Boolean
  )(implicit name: sourcecode.Name, definitionScope: DefinitionScope): NetLogo5Task =
    withDefaultArgs(
      workspace = Workspace.Script(script = script, name = script.getName),
      go = go,
      setup = setup,
      seed = seed,
      ignoreError = ignoreError,
      reuseWorkspace = reuseWorkspace,
      ignoreErrorOnDispose = ignoreErrorOnDispose,
      switch3d = switch3d
    ) set (
      inputs ++= seed.option.toSeq
    )

  def apply(
    script:               File,
    go:                   Seq[FromContext[String]],
    setup:                Seq[FromContext[String]]   = Seq(),
    embedWorkspace:       Boolean                    = false,
    seed:                 OptionalArgument[Val[Int]] = None,
    ignoreError:          Boolean                    = false,
    reuseWorkspace:       Boolean                    = false,
    ignoreErrorOnDispose: Boolean                    = false,
    switch3d:             Boolean                    = false
  )(implicit name: sourcecode.Name, definitionScope: DefinitionScope): NetLogo5Task =
    if (embedWorkspace) workspace(script.getCanonicalFile.getParentFile, script.getName, go = go, setup = setup, seed = seed, ignoreError = ignoreError, reuseWorkspace = reuseWorkspace, ignoreErrorOnDispose = ignoreErrorOnDispose, switch3d = switch3d)
    else file(script, go = go, setup = setup, seed = seed, ignoreError = ignoreError, reuseWorkspace = reuseWorkspace, ignoreErrorOnDispose = ignoreErrorOnDispose, switch3d = switch3d)

  private def withDefaultArgs(
                               workspace:            AbstractNetLogoTask.Workspace,
                               go:                   Seq[FromContext[String]],
                               setup:                Seq[FromContext[String]],
                               seed:                 Option[Val[Int]],
                               ignoreError:          Boolean,
                               reuseWorkspace:       Boolean,
                               ignoreErrorOnDispose: Boolean,
                               switch3d:             Boolean
  )(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    NetLogo5Task(
      config = InputOutputConfig(),
      external = External(),
      info = InfoConfig(),
      mapped = MappedInputOutputConfig(),
      workspace = workspace,
      go = go,
      setup = setup,
      seed = seed,
      ignoreError = ignoreError,
      reuseWorkspace = reuseWorkspace,
      ignoreErrorOnDispose = ignoreErrorOnDispose,
      switch3d = switch3d
    )

}

case class NetLogo5Task(
                         config:               InputOutputConfig,
                         external:             External,
                         info:                 InfoConfig,
                         mapped:               MappedInputOutputConfig,
                         workspace:            AbstractNetLogoTask.Workspace,
                         go:                   Seq[FromContext[String]],
                         setup:                Seq[FromContext[String]],
                         seed:                 Option[Val[Int]],
                         ignoreError:          Boolean,
                         reuseWorkspace:       Boolean,
                         ignoreErrorOnDispose: Boolean,
                         switch3d:             Boolean
) extends AbstractNetLogoTask {
  override def netLogoFactory: NetLogoFactory = NetLogo5Task.factory
}

