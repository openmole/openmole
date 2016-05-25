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
import org.openmole.plugin.task.external.ExternalTask.{ InputFile, InputFileArray, OutputFile, Resource }
import org.openmole.plugin.task.netlogo._
import org.openmole.plugin.task.external._
import NetLogoTask.Workspace
import org.openmole.core.workflow.task._
import java.io.File
import org.openmole.core.workflow.dsl
import org.openmole.core.workflow.dsl._
import org.openmole.plugin.task.external
import monocle.macros.Lenses
import org.openmole.core.workflow.builder
import org.openmole.plugin.task.external

import collection.JavaConversions._
import org.openmole.plugin.tool.netlogo5.NetLogo5

object NetLogo5Task {

  def factory = new NetLogoFactory {
    def apply = new NetLogo5
  }

  implicit def isBuilder = new NetLogoTaskBuilder[NetLogo5Task] {
    override def netLogoInputs = NetLogo5Task.netLogoInputs
    override def netLogoArrayOutputs = NetLogo5Task.netLogoArrayOutputs
    override def netLogoOutputs = NetLogo5Task.netLogoOutputs
    override def resources = NetLogo5Task.resources
    override def outputFiles = NetLogo5Task.outputFiles
    override def inputFileArrays = NetLogo5Task.inputFileArrays
    override def inputFiles = NetLogo5Task.inputFiles
    override def defaults = NetLogo5Task.defaults
    override def outputs = NetLogo5Task.outputs
    override def name = NetLogo5Task.name
    override def inputs = NetLogo5Task.inputs
  }

  def workspace(
    workspace:         File,
    script:            String,
    launchingCommands: Seq[String],
    seed:              Option[Prototype[Int]]
  ): NetLogo5Task =
    withDefaultArgs(
      workspace = Workspace(script = script, workspace = Some(workspace.getName)),
      launchingCommands = launchingCommands,
      seed = seed
    ) set (
        dsl.inputs += (seed.toSeq: _*),
        external.resources += workspace
      )

  def file(
    script:            File,
    launchingCommands: Seq[String],
    seed:              Option[Prototype[Int]] = None
  ): NetLogo5Task =
    withDefaultArgs(
      workspace = Workspace(script = script.getName),
      launchingCommands = launchingCommands,
      seed = seed
    ) set (
        dsl.inputs += (seed.toSeq: _*),
        external.resources += script
      )

  def apply(
    script:            File,
    launchingCommands: Seq[String],
    embedWorkspace:    Boolean                = false,
    seed:              Option[Prototype[Int]] = None
  ): NetLogo5Task =
    if (embedWorkspace) workspace(script.getCanonicalFile.getParentFile, script.getName, launchingCommands, seed)
    else file(script, launchingCommands, seed)

  private def withDefaultArgs(
    workspace:         NetLogoTask.Workspace,
    launchingCommands: Seq[String],
    seed:              Option[Prototype[Int]]
  ) =
    NetLogo5Task(
      inputs = PrototypeSet.empty,
      outputs = PrototypeSet.empty,
      defaults = DefaultSet.empty,
      name = None,
      netLogoInputs = Vector.empty,
      netLogoOutputs = Vector.empty,
      netLogoArrayOutputs = Vector.empty,
      workspace = workspace,
      launchingCommands = launchingCommands,
      seed = seed,
      inputFiles = Vector.empty,
      inputFileArrays = Vector.empty,
      outputFiles = Vector.empty,
      resources = Vector.empty
    )

}

@Lenses case class NetLogo5Task(
    inputs:              PrototypeSet,
    outputs:             PrototypeSet,
    defaults:            DefaultSet,
    name:                Option[String],
    netLogoInputs:       Vector[(Prototype[_], String)],
    netLogoOutputs:      Vector[(String, Prototype[_])],
    netLogoArrayOutputs: Vector[(String, Int, Prototype[_])],
    workspace:           NetLogoTask.Workspace,
    launchingCommands:   Seq[String],
    seed:                Option[Prototype[Int]],
    inputFiles:          Vector[InputFile],
    inputFileArrays:     Vector[InputFileArray],
    outputFiles:         Vector[OutputFile],
    resources:           Vector[Resource]
) extends NetLogoTask {
  override def netLogoFactory: NetLogoFactory = NetLogo5Task.factory
}

