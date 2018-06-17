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
import java.lang.Class

import scala.reflect.ClassTag

import org.openmole.core.context.{ Context, Val, Variable }
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.expansion._
import org.openmole.core.tools.io.Prettifier._
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.validation._
import org.openmole.core.workspace.NewFile
import org.openmole.plugin.task.external._
import org.openmole.plugin.tool.netlogo.NetLogo
import org.openmole.tool.cache._

object NetLogoTask {
  sealed trait Workspace

  object Workspace {
    case class Script(script: File, name: String) extends Workspace
    case class Directory(directory: File, name: String, script: String) extends Workspace
  }

  def wrapError[T](msg: String)(f: ⇒ T): T =
    try f
    catch {
      case e: Throwable ⇒
        throw new UserBadDataError(s"$msg:\n" + e.stackStringWithMargin)
    }

  def deployWorkspace(workspace: Workspace, directory: File) = {
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

  case class NetoLogoInstance(directory: File, workspaceDirectory: File, netLogo: NetLogo)

  def openNetLogoWorkspace(netLogoFactory: NetLogoFactory, workspace: Workspace, directory: File) = {
    val (workDir, script) = deployWorkspace(workspace, directory)
    val resolver = External.relativeResolver(workDir)(_)
    val netLogo = netLogoFactory()

    withThreadClassLoader(netLogo.getNetLogoClassLoader) {
      wrapError(s"Error while opening the file $script") {
        netLogo.open(script.getAbsolutePath)
      }
    }

    NetoLogoInstance(directory, workDir, netLogo)
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

  def setGlobal(netLogo: NetLogo, variable: String, value: AnyRef, ignoreError: Boolean = false) =
    withThreadClassLoader(netLogo.getNetLogoClassLoader) {
      wrapError(s"Error while setting $variable") {
        try netLogo.setGlobal(variable, value)
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

  def createPool(netLogoFactory: NetLogoFactory, workspace: NetLogoTask.Workspace, cached: Boolean)(implicit newFile: NewFile) = {
    def createInstance = {
      val workspaceDirectory = newFile.newDir("netlogoworkpsace")
      NetLogoTask.openNetLogoWorkspace(netLogoFactory, workspace, workspaceDirectory)
    }

    def destroyInstance(instance: NetLogoTask.NetoLogoInstance) = {
      instance.directory.recursiveDelete
      instance.netLogo.dispose()
    }

    WithInstance[NetLogoTask.NetoLogoInstance](
      () ⇒ createInstance,
      close = destroyInstance,
      pooled = cached
    )
  }

  def netLogoCompatibleType(x: Any) = {
    def convertArray(x: Any): AnyRef = x match {
      case a: Array[_] ⇒ a.asInstanceOf[Array[_]].map { x ⇒ convertArray(x.asInstanceOf[AnyRef]) }
      case x           ⇒ safeType(x)
    }

    def safeType(x: Any): AnyRef = {
      val v = x match {
        case i: Int    ⇒ i.toDouble
        case l: Long   ⇒ l.toDouble
        case fl: Float ⇒ fl.toDouble
        case f: File   ⇒ f.getAbsolutePath
        case x: AnyRef ⇒ x // Double and String are unchanged
      }
      v.asInstanceOf[AnyRef]
    }

    x match {
      case x: Array[_] ⇒ convertArray(x)
      case x           ⇒ safeType(x)
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

  def validateNetLogoInputTypes(inputs: Seq[Val[_]]) = {
    def acceptedType(c: Class[_]): Boolean =
      if (c.isArray()) acceptedType(c.getComponentType)
      else Seq(classOf[String], classOf[Int], classOf[Double], classOf[Long], classOf[Float], classOf[File]).contains(c)

    inputs.flatMap {
      case v ⇒
        v.`type`.runtimeClass.asInstanceOf[Class[_]] match {
          case c if acceptedType(c) ⇒ None
          case _                    ⇒ Some(new UserBadDataError(s"""Error for netLogoInput "${v.name} : type "${v.`type`.runtimeClass.toString()} is not managed by NetLogo."""))
        }
    }
  }
}

trait NetLogoTask extends Task with ValidateTask {

  lazy val netLogoInstanceKey = CacheKey[WithInstance[NetLogoTask.NetoLogoInstance]]()

  def workspace: NetLogoTask.Workspace
  def launchingCommands: Seq[FromContext[String]]
  def netLogoInputs: Seq[(Val[_], String)]
  def netLogoOutputs: Iterable[(String, Val[_])]
  def netLogoArrayOutputs: Iterable[(String, Int, Val[_])]
  def netLogoFactory: NetLogoFactory
  def ignoreError: Boolean
  def seed: Option[Val[Int]]
  def external: External
  def reuseWorkspace: Boolean

  override def validate = Validate { p ⇒
    import p._
    val allInputs = External.PWD :: inputs.toList
    launchingCommands.flatMap(_.validate(allInputs)) ++
      External.validate(external)(allInputs).apply ++
      NetLogoTask.validateNetLogoInputTypes(netLogoInputs.map(_._1))
  }

  override protected def process(executionContext: TaskExecutionContext) = FromContext { parameters ⇒
    import parameters._

    val pool = executionContext.cache.getOrElseUpdate(netLogoInstanceKey, NetLogoTask.createPool(netLogoFactory, workspace, reuseWorkspace))

    pool { instance ⇒

      val resolver = External.relativeResolver(instance.workspaceDirectory)(_)
      val context = parameters.context + (External.PWD → instance.workspaceDirectory.getAbsolutePath)
      val preparedContext = External.deployInputFilesAndResources(external, context, resolver)

      seed.foreach { s ⇒ NetLogoTask.executeNetLogo(instance.netLogo, s"random-seed ${context(s)}") }

      for (inBinding ← netLogoInputs) {
        val v = NetLogoTask.netLogoCompatibleType(preparedContext(inBinding._1))
        NetLogoTask.setGlobal(instance.netLogo, inBinding._2, v)
      }

      for (cmd ← launchingCommands.map(_.from(context))) NetLogoTask.executeNetLogo(instance.netLogo, cmd, ignoreError)

      val contextResult =
        External.fetchOutputFiles(external, outputs, preparedContext, resolver, instance.workspaceDirectory) ++ netLogoOutputs.map {
          case (name, prototype) ⇒
            try {
              val outputValue = NetLogoTask.report(instance.netLogo, name)
              if (!prototype.`type`.runtimeClass.isArray) Variable(prototype.asInstanceOf[Val[Any]], outputValue)
              else {
                val netLogoCollection = outputValue.asInstanceOf[AbstractCollection[Any]]
                NetLogoTask.netLogoArrayToVariable(netLogoCollection, prototype)
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
              val netLogoCollection = NetLogoTask.report(instance.netLogo, name)
              val outputValue = netLogoCollection.asInstanceOf[AbstractCollection[Any]].toArray()(column)
              if (!prototype.`type`.runtimeClass.isArray) Variable(prototype.asInstanceOf[Val[Any]], outputValue)
              else NetLogoTask.netLogoArrayToVariable(outputValue.asInstanceOf[AbstractCollection[Any]], prototype)
            }
            catch {
              case e: Throwable ⇒ throw new UserBadDataError(e, s"Error when fetching column $column of netlogo output $name in variable $prototype")
            }
        }

      contextResult
    }
  }

}
