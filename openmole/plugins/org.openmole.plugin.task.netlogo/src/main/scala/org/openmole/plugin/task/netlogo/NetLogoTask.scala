/*
 * Copyright (C) 2011 reuillon
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
import org.openmole.core.model.data.IPrototype
import org.openmole.misc.tools.service.Logger
import org.openmole.misc.workspace.Workspace
import org.openmole.plugin.task.external.system.ExternalSystemTask
import scala.collection.mutable.ListBuffer

object NetLogoTask extends Logger

abstract class NetLogoTask(
  name: String,
  workspace: File,
  sriptName: String,
  launchingCommands: Iterable[String]) extends ExternalSystemTask(name) {

  import NetLogoTask._
  
  val inputBinding = new ListBuffer[(IPrototype[_], String)]
  val outputBinding = new ListBuffer[(String, IPrototype[_])]
  def relativeScriptPath = workspace.getName + "/" + sriptName
    
  addResource(workspace)

  override def process(context: IContext): IContext = {
        
    val tmpDir = Workspace.newDir("netLogoTask")
    prepareInputFiles(context, tmpDir)
    val script = new File(tmpDir, relativeScriptPath)
    val netLogo = netLogoFactory()
    try {
      netLogo.open(script.getAbsolutePath)

      for (inBinding <- inputBinding) {
        val v = context.value(inBinding._1).get
        netLogo.command("set " + inBinding._2 + " " + v.toString)
      }

      for (cmd <- launchingCommands) netLogo.command(VariableExpansion.expandData(context, cmd))
                
      fetchOutputFiles(context, tmpDir) ++ outputBinding.map {
        case(name, prototype) =>
          val outputValue = netLogo.report(name)
          if (!prototype.`type`.erasure.isArray) new Variable(prototype.asInstanceOf[IPrototype[Any]], outputValue)
          else {
            val netlogoCollection = outputValue.asInstanceOf[AbstractCollection[Any]]
            val array = java.lang.reflect.Array.newInstance(prototype.`type`.erasure.getComponentType, netlogoCollection.size)
            val it = netlogoCollection.iterator
            for (i <- 0 until netlogoCollection.size) java.lang.reflect.Array.set(array, i, it.next)
            new Variable(prototype.asInstanceOf[IPrototype[Any]], array)
          }
      }
    } finally netLogo.dispose

  }

  def addNetLogoInput(prototype: IPrototype[_]): this.type = addNetLogoInput(prototype, prototype.name)

  def addNetLogoInput(prototype: IPrototype[_], binding: String): this.type = {
    inputBinding += prototype -> binding
    super.addInput(prototype)
    this
  }

  def addNetLogoOutput(binding: String, prototype: IPrototype[_]): this.type = {
    outputBinding += binding -> prototype
    super.addOutput(prototype)
    this
  }

  def addNetLogoOutput(prototype: IPrototype[_]): this.type = addNetLogoOutput(prototype.name, prototype) 

  def netLogoFactory: NetLogoFactory
}
