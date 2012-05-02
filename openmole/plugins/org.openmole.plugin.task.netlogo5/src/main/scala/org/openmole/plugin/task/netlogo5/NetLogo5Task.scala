/*
 * Copyright (C) 2012 reuillon
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
import org.openmole.core.model.data.IDataSet
import org.openmole.core.model.data.IParameterSet
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.task.IPluginSet
import org.openmole.plugin.task.netlogo.NetLogoFactory
import org.openmole.plugin.task.netlogo.NetLogoTask
import org.openmole.plugin.task.netlogo.NetLogoTaskBuilder

object NetLogo5Task {

  def factory = new NetLogoFactory {
    def apply = new NetLogo5
  }

  def apply(
    name: String,
    workspace: File,
    script: String,
    launchingCommands: Iterable[String])(implicit plugins: IPluginSet) = {
    val _launchingCommands = launchingCommands
    val (_workspace, _script) = (workspace, script)

    new NetLogoTaskBuilder { builder ⇒

      addResource(workspace)

      def toTask = new NetLogo5Task(
        name,
        workspace = new NetLogoTask.Workspace(_workspace, _script),
        launchingCommands = _launchingCommands,
        inputs = builder.inputs,
        outputs = builder.outputs,
        parameters = builder.parameters,
        provided = builder.provided,
        produced = builder.produced,
        netLogoInputs = builder.netLogoInputs,
        netLogoOutputs = builder.netLogoOutputs,
        netLogoFactory = factory)
    }
  }

  def apply(
    name: String,
    script: File,
    launchingCommands: Iterable[String])(implicit plugins: IPluginSet) = {
    val _launchingCommands = launchingCommands
    new NetLogoTaskBuilder { builder ⇒

      addResource(script)

      def toTask = new NetLogo5Task(
        name,
        launchingCommands = _launchingCommands,
        workspace = new NetLogoTask.Workspace(script),
        inputs = builder.inputs,
        outputs = builder.outputs,
        parameters = builder.parameters,
        provided = builder.provided,
        produced = builder.produced,
        netLogoInputs = builder.netLogoInputs,
        netLogoOutputs = builder.netLogoOutputs,
        netLogoFactory = factory)
    }
  }

  def apply(
    name: String,
    script: File,
    launchingCommands: Iterable[String],
    embedWorkpsace: Boolean)(implicit plugins: IPluginSet): NetLogoTaskBuilder =
    if (embedWorkpsace) apply(name, script.getParentFile, script.getName, launchingCommands)
    else apply(name, script, launchingCommands)

}

sealed class NetLogo5Task(
  name: String,
  workspace: NetLogoTask.Workspace,
  launchingCommands: Iterable[String],
  netLogoInputs: Iterable[(IPrototype[_], String)],
  netLogoOutputs: Iterable[(String, IPrototype[_])],
  netLogoFactory: NetLogoFactory,
  inputs: IDataSet,
  outputs: IDataSet,
  parameters: IParameterSet,
  provided: Iterable[(Either[File, IPrototype[File]], String, Boolean)],
  produced: Iterable[(String, IPrototype[File])])(implicit plugins: IPluginSet) extends NetLogoTask(
  name,
  workspace,
  launchingCommands,
  netLogoInputs,
  netLogoOutputs,
  netLogoFactory,
  inputs,
  outputs,
  parameters,
  provided,
  produced)

