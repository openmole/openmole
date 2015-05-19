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
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.tools.io.Prettifier
import org.openmole.core.tools.service.OS
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.tools._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.tools.{ VariableExpansion, ExpandedString }
import org.openmole.plugin.task.external.ExternalTask._
import org.openmole.plugin.task.external._

import Prettifier._

object NetLogoTask {

  case class Workspace(script: String, workspace: Option[String]) {
    def this(workspace: File, script: String) = this(script, Some(workspace.getName))
    def this(script: File) = this(script.getName, None)
    val workDirectory = workspace.getOrElse("")
  }

}

trait NetLogoTask extends ExternalTask {

  def workspace: NetLogoTask.Workspace
  def launchingCommands: Seq[String]
  def netLogoInputs: Seq[(Prototype[_], String)]
  def netLogoOutputs: Iterable[(String, Prototype[_])]
  def netLogoArrayOutputs: Iterable[(String, Int, Prototype[_])]
  def netLogoFactory: NetLogoFactory

  private def wrapError[T](msg: String)(f: ⇒ T): T =
    try f
    catch {
      case e: Throwable ⇒
        throw new UserBadDataError(s"$msg:\n" + e.stackStringWithMargin)
    }

  @transient lazy val expandedCommands = launchingCommands.map(VariableExpansion(_))

  override def process(context: Context)(implicit rng: RandomProvider): Context = withWorkDir { tmpDir ⇒
    val preparedContext = prepareInputFiles(context, tmpDir, workspace.workDirectory)

    val script = tmpDir / workspace.workDirectory / workspace.script
    val netLogo = netLogoFactory()
    try {
      wrapError(s"Error while opening the file $script") { netLogo.open(script.getAbsolutePath) }

      for (inBinding ← netLogoInputs) {
        val v = preparedContext(inBinding._1) match {
          case x: String ⇒ '"' + x + '"'
          case x         ⇒ x.toString
        }
        val cmd = "set " + inBinding._2 + " " + v
        wrapError(s"Error while executing command $cmd") { netLogo.command(cmd) }
      }

      for (cmd ← expandedCommands) wrapError(s"Error while executing command $cmd") {
        netLogo.command(cmd.expand(context))
      }

      fetchOutputFiles(preparedContext, tmpDir, workspace.workDirectory) ++ netLogoOutputs.map {
        case (name, prototype) ⇒
          try {
            val outputValue = netLogo.report(name)
            if (!prototype.`type`.runtimeClass.isArray) Variable(prototype.asInstanceOf[Prototype[Any]], outputValue)
            else {
              val netLogoCollection = outputValue.asInstanceOf[AbstractCollection[Any]]
              netLogoArrayToVariable(netLogoCollection, prototype)
            }
          }
          catch {
            case e: Throwable ⇒
              throw new UserBadDataError(
                s"Error when fetching netlogo output $name in variable $prototype:\n" + e.stackStringWithMargin)
          }
      } ++ netLogoArrayOutputs.map {
        case (name, column, prototype) ⇒
          try {
            val netLogoCollection = netLogo.report(name)
            val outputValue = netLogoCollection.asInstanceOf[AbstractCollection[Any]].toArray()(column)
            if (!prototype.`type`.runtimeClass.isArray) Variable(prototype.asInstanceOf[Prototype[Any]], outputValue)
            else netLogoArrayToVariable(outputValue.asInstanceOf[AbstractCollection[Any]], prototype)
          }
          catch {
            case e: Throwable ⇒ throw new UserBadDataError(e, s"Error when fetching column $column of netlogo output $name in variable $prototype")
          }
      }
    }
    finally netLogo.dispose

  }

  def netLogoArrayToVariable(netlogoCollection: AbstractCollection[Any], prototype: Prototype[_]) = {
    val array = java.lang.reflect.Array.newInstance(prototype.`type`.runtimeClass.getComponentType, netlogoCollection.size)
    val it = netlogoCollection.iterator
    for (i ← 0 until netlogoCollection.size) java.lang.reflect.Array.set(array, i, it.next)
    Variable(prototype.asInstanceOf[Prototype[Any]], array)
  }

}
