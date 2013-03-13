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

import org.openmole.core.model.task._
import org.openmole.core.model.data._
import org.openmole.plugin.task.netlogo._
import org.openmole.plugin.task.external._
import NetLogoTask.Workspace
import org.openmole.core.implementation.task._
import java.io.File
import collection.JavaConversions._

object NetLogo4Task {

  def factory = new NetLogoFactory {
    def apply = new NetLogo4
  }

  def apply(
    name: String,
    workspace: File,
    script: String,
    launchingCommands: Iterable[String])(implicit plugins: PluginSet): NetLogoTaskBuilder = {
    val _launchingCommands = launchingCommands
    val (_workspace, _script) = (workspace, script)

    new NetLogoTaskBuilder { builder ⇒

      addResource(workspace)

      def toTask = new NetLogo4Task(
        name,
        workspace = new Workspace(_workspace, _script),
        launchingCommands = _launchingCommands,
        inputs = builder.inputs,
        outputs = builder.outputs,
        parameters = builder.parameters,
        inputFiles = builder.inputFiles,
        outputFiles = builder.outputFiles,
        resources = builder.resources,
        netLogoInputs = builder.netLogoInputs,
        netLogoOutputs = builder.netLogoOutputs,
        netLogoArrayOutputs = builder.netLogoArrayOutputs,
        netLogoFactory = factory)
    }
  }

  def apply(
    name: String,
    script: File,
    launchingCommands: Iterable[String])(implicit plugins: PluginSet): NetLogoTaskBuilder = {
    val _launchingCommands = launchingCommands
    new NetLogoTaskBuilder { builder ⇒

      addResource(script)

      def toTask = new NetLogo4Task(
        name,
        launchingCommands = _launchingCommands,
        workspace = new Workspace(script),
        inputs = builder.inputs,
        outputs = builder.outputs,
        parameters = builder.parameters,
        inputFiles = builder.inputFiles,
        outputFiles = builder.outputFiles,
        resources = builder.resources,
        netLogoInputs = builder.netLogoInputs,
        netLogoOutputs = builder.netLogoOutputs,
        netLogoArrayOutputs = builder.netLogoArrayOutputs,
        netLogoFactory = factory)
    }
  }

  def apply(
    name: String,
    script: File,
    launchingCommands: Iterable[String],
    embedWorkspace: Boolean)(implicit plugins: PluginSet): NetLogoTaskBuilder =
    if (embedWorkspace) apply(name, script.getParentFile, script.getName, launchingCommands)
    else apply(name, script, launchingCommands)

}

sealed class NetLogo4Task(
  name: String,
  workspace: NetLogoTask.Workspace,
  launchingCommands: Iterable[String],
  netLogoInputs: Iterable[(Prototype[_], String)],
  netLogoOutputs: Iterable[(String, Prototype[_])],
  netLogoArrayOutputs: Iterable[(String, Int, Prototype[_])],
  netLogoFactory: NetLogoFactory,
  inputs: DataSet,
  outputs: DataSet,
  parameters: ParameterSet,
  inputFiles: Iterable[(Prototype[File], String, Boolean)],
  outputFiles: Iterable[(String, Prototype[File])],
  resources: Iterable[(File, String, Boolean, OS)])(implicit plugins: PluginSet) extends NetLogoTask(
  name,
  workspace,
  launchingCommands,
  netLogoInputs,
  netLogoOutputs,
  netLogoArrayOutputs,
  netLogoFactory,
  inputs,
  outputs,
  parameters,
  inputFiles,
  outputFiles,
  resources)

