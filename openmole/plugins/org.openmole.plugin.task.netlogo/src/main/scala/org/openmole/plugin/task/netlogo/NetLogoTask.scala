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

import java.util.AbstractCollection

import org.openmole.core.context.{ Context, Val, Variable }
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.expansion._
import org.openmole.core.tools.io.Prettifier._
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.validation._
import org.openmole.plugin.task.external._
import org.openmole.plugin.tool.netlogo.NetLogo
import org.openmole.tool.cache._
import shapeless._

object NetLogoTask {
  sealed trait Workspace

  object Workspace {
    case class Script(script: File, name: String) extends Workspace
    case class Directory(directory: File, name: String, script: String) extends Workspace
  }

  lazy val netLogoWorkspace = CacheKey[org.openmole.plugin.tool.netlogo.NetLogo]()

  val caseFile = TypeCase[Val[File]]
}

trait NetLogoTask extends Task with ValidateTask {

  def workspace: NetLogoTask.Workspace
  def launchingCommands: Seq[FromContext[String]]
  def netLogoInputs: Seq[(Val[_], String)]
  def netLogoOutputs: Iterable[(String, Val[_])]
  def netLogoArrayOutputs: Iterable[(String, Int, Val[_])]
  def netLogoFactory: NetLogoFactory
  def ignoreError: Boolean
  def seed: Option[Val[Int]]
  //def cached: Boolean
  def external: External

  override def validate = Validate { p ⇒
    import p._
    val allInputs = External.PWD :: inputs.toList
    launchingCommands.flatMap(_.validate(allInputs)) ++ External.validate(external)(allInputs).apply
  }

  def wrapError[T](msg: String)(f: ⇒ T): T =
    try f
    catch {
      case e: Throwable ⇒
        throw new UserBadDataError(s"$msg:\n" + e.stackStringWithMargin)
    }

  def deployWorkspace(directory: File) = {
    import org.openmole.tool.file._

    val resolver = External.relativeResolver(directory)(_)
    workspace match {
      case s: NetLogoTask.Workspace.Script ⇒
        s.script.realFile.copy(resolver(s.name))
        (directory, directory / s.name)
      case w: NetLogoTask.Workspace.Directory ⇒
        w.directory.realFile.copy(resolver(w.name))
        (directory / w.name, directory / w.name / w.script)
    }
  }

  def openNetlogoWorkspace(directory: File) = {
    val (workDir, script) = deployWorkspace(directory)
    val resolver = External.relativeResolver(workDir)(_)
    val netLogo = netLogoFactory()

    withThreadClassLoader(netLogo.getNetLogoClassLoader) {
      wrapError(s"Error while opening the file $script") {
        netLogo.open(script.getAbsolutePath)
      }
    }

    (netLogo, workDir)
  }

  def executeNetLogo(netLogo: NetLogo, cmd: String, ignoreError: Boolean = false) =
    withThreadClassLoader(netLogo.getNetLogoClassLoader) {
      wrapError(s"Error while executing command $cmd") {
        try netLogo.command(cmd)
        catch {
          case t: Throwable ⇒
            if (ignoreError && netLogo.isNetLogoException(t)) {} else throw t
        }
      }
    }

  def report(netLogo: NetLogo, name: String) =
    withThreadClassLoader(netLogo.getNetLogoClassLoader) { netLogo.report(name) }

  def dispose(netLogo: NetLogo) =
    withThreadClassLoader(netLogo.getNetLogoClassLoader) { netLogo.dispose() }

  override protected def process(executionContext: TaskExecutionContext) = FromContext { parameters ⇒
    External.withWorkDir(executionContext) { tmpDir ⇒
      import parameters._

      val (netLogo, workDir) = openNetlogoWorkspace(tmpDir)
      val resolver = External.relativeResolver(workDir)(_)
      val context = parameters.context + (External.PWD → workDir.getAbsolutePath)
      val preparedContext = External.deployInputFilesAndResources(external, context, resolver)

      try {
        seed.foreach { s ⇒ executeNetLogo(netLogo, s"random-seed ${context(s)}") }

        for (inBinding ← netLogoInputs) {
          val v = preparedContext(inBinding._1) match {
            case x: String ⇒ '"' + x + '"'
            case x: File   ⇒ '"' + x.getAbsolutePath + '"'
            case x         ⇒ x.toString
          }
          executeNetLogo(netLogo, "set " + inBinding._2 + " " + v)
        }

        for (cmd ← launchingCommands.map(_.from(context))) executeNetLogo(netLogo, cmd, ignoreError)

        val contextResult =
          External.fetchOutputFiles(external, outputs, preparedContext, External.relativeResolver(workDir), tmpDir) ++ netLogoOutputs.map {
            case (name, prototype) ⇒
              try {
                val outputValue = report(netLogo, name)
                if (!prototype.`type`.runtimeClass.isArray) Variable(prototype.asInstanceOf[Val[Any]], outputValue)
                else {
                  val netLogoCollection = outputValue.asInstanceOf[AbstractCollection[Any]]
                  netLogoArrayToVariable(netLogoCollection, prototype)
                }
              }
              catch {
                case e: Throwable ⇒
                  throw new UserBadDataError(
                    s"Error when fetching netlogo output $name in variable $prototype:\n" + e.stackStringWithMargin
                  )
              }
          } ++ netLogoArrayOutputs.map {
            case (name, column, prototype) ⇒
              try {
                val netLogoCollection = report(netLogo, name)
                val outputValue = netLogoCollection.asInstanceOf[AbstractCollection[Any]].toArray()(column)
                if (!prototype.`type`.runtimeClass.isArray) Variable(prototype.asInstanceOf[Val[Any]], outputValue)
                else netLogoArrayToVariable(outputValue.asInstanceOf[AbstractCollection[Any]], prototype)
              }
              catch {
                case e: Throwable ⇒ throw new UserBadDataError(e, s"Error when fetching column $column of netlogo output $name in variable $prototype")
              }
          }

        External.cleanWorkDirectory(outputs, contextResult, tmpDir)
        //else External.cleanWorkDirectory(outputs, contextResult, tmpDir, Seq(workDir))

        contextResult
      }
      finally dispose(netLogo)
    }
  }

  def netLogoArrayToVariable(netlogoCollection: AbstractCollection[Any], prototype: Val[_]) = {
    val arrayType = prototype.`type`.runtimeClass.getComponentType
    val array = java.lang.reflect.Array.newInstance(arrayType, netlogoCollection.size)
    val it = netlogoCollection.iterator
    for (i ← 0 until netlogoCollection.size) {
      val v = it.next
      try java.lang.reflect.Array.set(array, i, v)
      catch {
        case e: Throwable ⇒ throw new UserBadDataError(e, s"Error when adding a variable of type ${v.getClass} in an array of ${arrayType}")
      }
    }
    Variable(prototype.asInstanceOf[Val[Any]], array)
  }

}
