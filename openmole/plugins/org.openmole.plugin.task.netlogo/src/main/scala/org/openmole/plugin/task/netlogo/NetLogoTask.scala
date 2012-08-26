/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.plugin.task.netlogo

import java.io.File
import java.util.AbstractCollection
import org.openmole.core.implementation.data.Variable
import org.openmole.core.implementation.tools.VariableExpansion
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IDataSet
import org.openmole.core.model.data.IParameterSet
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.task.IPluginSet
import org.openmole.plugin.task.external.ExternalTask

object NetLogoTask {

  class Workspace(val location: Either[(File, String), File]) {
    def this(workspace: File, script: String) = this(Left(workspace, script))
    def this(script: File) = this(Right(script))
  }

}

class NetLogoTask(
    val name: String,
    val workspace: NetLogoTask.Workspace,
    val launchingCommands: Iterable[String],
    val netLogoInputs: Iterable[(IPrototype[_], String)],
    val netLogoOutputs: Iterable[(String, IPrototype[_])],
    val netLogoFactory: NetLogoFactory,
    val inputs: IDataSet,
    val outputs: IDataSet,
    val parameters: IParameterSet,
    val inputFiles: Iterable[(IPrototype[File], String, Boolean)],
    val outputFiles: Iterable[(String, IPrototype[File])],
    val resources: Iterable[(File, String, Boolean)])(implicit val plugins: IPluginSet) extends ExternalTask {

  val scriptPath =
    workspace.location match {
      case Left((f, s)) ⇒ f.getName + "/" + s
      case Right(s) ⇒ s.getName
    }

  override def process(context: IContext): IContext = {

    val tmpDir = org.openmole.misc.workspace.Workspace.newDir("netLogoTask")
    val links = prepareInputFiles(context, tmpDir)

    val script = new File(tmpDir, scriptPath)
    val netLogo = netLogoFactory()
    try {
      netLogo.open(script.getAbsolutePath)

      for (inBinding ← netLogoInputs) {
        val v = context.value(inBinding._1).get
        netLogo.command("set " + inBinding._2 + " " + v.toString)
      }

      for (cmd ← launchingCommands) netLogo.command(VariableExpansion(context, cmd))

      fetchOutputFiles(context, tmpDir, links) ++ netLogoOutputs.map {
        case (name, prototype) ⇒
          val outputValue = netLogo.report(name)
          if (!prototype.`type`.erasure.isArray) new Variable(prototype.asInstanceOf[IPrototype[Any]], outputValue)
          else {
            val netlogoCollection = outputValue.asInstanceOf[AbstractCollection[Any]]
            val array = java.lang.reflect.Array.newInstance(prototype.`type`.erasure.getComponentType, netlogoCollection.size)
            val it = netlogoCollection.iterator
            for (i ← 0 until netlogoCollection.size) java.lang.reflect.Array.set(array, i, it.next)
            new Variable(prototype.asInstanceOf[IPrototype[Any]], array)
          }
      }
    } finally netLogo.dispose

  }

}
