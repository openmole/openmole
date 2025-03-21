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
import java.util

import scala.reflect.ClassTag
import org.openmole.core.context.{ Context, Val, ValType, Variable }
import org.openmole.core.exception.{ InternalProcessingError, UserBadDataError }
import org.openmole.core.argument._
import org.openmole.core.setter.MappedInputOutputConfig
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.validation._
import org.openmole.core.workspace.TmpDirectory
import org.openmole.plugin.task.external._
import org.openmole.plugin.tool.netlogo.NetLogo
import org.openmole.tool.cache._
import org.openmole.core.dsl.extension.*
import org.openmole.tool.logger.Prettifier

import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer

object AbstractNetLogoTask {
  sealed trait Workspace

  /**
   * Workspace is either a script, or a full directory
   */
  object Workspace:
    case class Script(script: File, name: String) extends Workspace
    case class Directory(directory: File, name: String, script: String) extends Workspace

  /**
   * Errors as [[UserBadDataError]]
   * @param msg
   * @param f
   * @tparam T
   * @return
   */
  def wrapError[T](msg: String)(f: => T): T =
    try f
    catch
      case e: InternalProcessingError => throw e
      case e: Throwable =>
        throw new UserBadDataError(s"$msg:\n" + Prettifier.stackStringWithMargin(e))

  def deployWorkspace(workspace: Workspace, directory: File) = {
    import org.openmole.tool.file._

    val resolver = External.relativeResolver(directory)(_)
    workspace match {
      case s: AbstractNetLogoTask.Workspace.Script =>
        s.script.realFile.copy(resolver(s.name))
        (directory, directory / s.name)
      case w: AbstractNetLogoTask.Workspace.Directory =>
        w.directory.realFile.copy(resolver(w.name))
        (directory / w.name, directory / w.name / w.script)
    }
  }

  case class NetoLogoInstance(directory: File, workspaceDirectory: File, netLogo: NetLogo)

  def openNetLogoWorkspace(netLogoFactory: NetLogoFactory, workspace: Workspace, directory: File, switch3d: Boolean) = {
    val (workDir, script) = deployWorkspace(workspace, directory)
    val resolver = External.relativeResolver(workDir)(_)
    val netLogo = netLogoFactory()

    withThreadClassLoader(netLogo.getNetLogoClassLoader) {
      wrapError(s"Error while opening the file $script") {
        netLogo.open(script.getAbsolutePath, switch3d)
      }
    }

    NetoLogoInstance(directory, workDir, netLogo)
  }

  def executeNetLogo(netLogo: NetLogo, cmd: String, ignoreError: Boolean = false) =
    withThreadClassLoader(netLogo.getNetLogoClassLoader) {
      wrapError(s"Error while executing command $cmd") {
        try netLogo.command(cmd)
        catch {
          case t: Throwable =>
            if (ignoreError && netLogo.isNetLogoException(t)) {} else throw t
        }
      }
    }

  def setGlobal(netLogo: NetLogo, variable: String, value: AnyRef, ignoreError: Boolean = false) =
    withThreadClassLoader(netLogo.getNetLogoClassLoader) {
      wrapError(s"Error while setting $variable") {
        try netLogo.setGlobal(variable, value)
        catch {
          case t: Throwable =>
            if (ignoreError && netLogo.isNetLogoException(t)) {} else throw t
        }
      }
    }

  def report(netLogo: NetLogo, name: String) =
    withThreadClassLoader(netLogo.getNetLogoClassLoader) { netLogo.report(name) }

  def dispose(netLogo: NetLogo, ignoreErrorOnDispose: Boolean) =
    withThreadClassLoader(netLogo.getNetLogoClassLoader) {
      try netLogo.dispose()
      catch {
        //FIXME it hapen with the nw extension, this error actually leaks memory. Bug report: https://github.com/NetLogo/NetLogo/issues/1766
        case t: NoClassDefFoundError => if (!ignoreErrorOnDispose) throw t
      }
    }

  def createPool(netLogoFactory: NetLogoFactory, workspace: AbstractNetLogoTask.Workspace, cached: Boolean, ignoreErrorOnDispose: Boolean, switch3d: Boolean)(implicit newFile: TmpDirectory) = {
    def createInstance = {
      val workspaceDirectory = newFile.newDirectory("netlogoworkpsace")
      AbstractNetLogoTask.openNetLogoWorkspace(netLogoFactory, workspace, workspaceDirectory, switch3d)
    }

    def destroyInstance(instance: AbstractNetLogoTask.NetoLogoInstance) = {
      instance.directory.recursiveDelete
      dispose(instance.netLogo, ignoreErrorOnDispose)
    }

    WithInstance[AbstractNetLogoTask.NetoLogoInstance](close = destroyInstance, pooled = cached) { () => createInstance }
  }

  /**
   * Ensure a variable type is compatible with NetLogo. Goes recursively inside arrays at any level.
   * @param x
   * @return
   */
  def netLogoCompatibleType(x: Any) = {
    def convertArray(x: Any): AnyRef = x match {
      case a: Array[?] => a.asInstanceOf[Array[?]].map { x => convertArray(x.asInstanceOf[AnyRef]) }
      case x           => safeType(x)
    }

    def safeType(x: Any): AnyRef = {
      val v = x match {
        case i: Int    => i.toDouble
        case l: Long   => l.toDouble
        case fl: Float => fl.toDouble
        case f: File   => f.getAbsolutePath
        case x: AnyRef => x // Double, String and Boolean are unchanged
      }
      v.asInstanceOf[AnyRef]
    }

    x match {
      case x: Array[?] => convertArray(x)
      case x           => safeType(x)
    }
  }

  // Manually do conversions to java native types ; necessary to add an output cast feature,
  // e.g. NetLogo numeric ~ java.lang.Double -> Int or String and not necessarily Double,
  // the target type being the one of the prototype
  def cast(value: Any, clazz: Class[?]) =
    try {
      clazz match {
        // all netlogo numeric are java.lang.Double
        case c if c == classOf[Double]  => value.asInstanceOf[java.lang.Double].doubleValue()
        case c if c == classOf[Float]   => value.asInstanceOf[java.lang.Double].floatValue()
        case c if c == classOf[Int]     => value.asInstanceOf[java.lang.Double].intValue()
        case c if c == classOf[Long]    => value.asInstanceOf[java.lang.Double].longValue()
        // target boolean
        case c if c == classOf[Boolean] => value.asInstanceOf[java.lang.Boolean].booleanValue()
        // target string assume the origin type has a toString
        case c if c == classOf[String]  => value.toString
        // try casting anyway - NOTE : untested
        case c                          => c.cast(value)
      }
    }
    catch {
      case e: Throwable => throw new UserBadDataError(e, s"Error when casting a variable of type ${value.getClass} to target type ${clazz}")
    }

  /**
   * Convert a netlogo collection to a Variable for which the prototype is expected to have the corresponding depth.
   *
   * @param netlogoCollection
   * @param prototype
   * @return
   */
  def netLogoArrayToVariable(netlogoCollection: AbstractCollection[Any], prototype: Val[?]) =
    Variable.constructArray[java.util.AbstractCollection[Any]](prototype, netlogoCollection, cast(_, _))

  /**
   * Check if provided inputs are compatible with NetLogo
   *
   * @param inputs
   * @return
   */
  def validateNetLogoInputTypes = Validate { p =>
    import p._
    def acceptedType(c: Class[?]): Boolean =
      if (c.isArray()) acceptedType(c.getComponentType)
      else Seq(classOf[String], classOf[Int], classOf[Double], classOf[Long], classOf[Float], classOf[File], classOf[Boolean]).contains(c)

    inputs.flatMap {
      case v =>
        v.`type`.runtimeClass.asInstanceOf[Class[?]] match {
          case c if acceptedType(c) => None
          case _                    => Some(new UserBadDataError(s"""Error for netLogoInput ${v.name} : type ${v.`type`.runtimeClass.toString()} is not managed by NetLogo."""))
        }
    }
  }

  def netLogoValueToVal(outputValue: => AnyRef, mapped:  Mapped[?]) =
    try {
      if (outputValue == null) throw new InternalProcessingError(s"Value of netlogo output ${mapped.name} has been reported as null by netlogo")
      val runtimeClass = mapped.v.`type`.runtimeClass
      if (!runtimeClass.isArray) Variable.unsecureUntyped(mapped.v, AbstractNetLogoTask.cast(outputValue, runtimeClass))
      else {
        val netLogoCollection: util.AbstractCollection[Any] =
          if (classOf[AbstractCollection[Any]].isAssignableFrom(outputValue.getClass)) outputValue.asInstanceOf[AbstractCollection[Any]]
          else {
            val newArray = new util.LinkedList[Any]()
            newArray.add(outputValue)
            newArray
          }
        AbstractNetLogoTask.netLogoArrayToVariable(netLogoCollection, mapped.v)
      }
    }
    catch
      case e: Throwable =>
        throw new UserBadDataError(
          s"Error when fetching netlogo output ${mapped.name} in variable ${mapped.v}:\n" + Prettifier.stackStringWithMargin(e)
        )
}

/**
 * Generic NetLogoTask
 */
trait AbstractNetLogoTask extends Task with ValidateTask:
  netlogoTask =>

  lazy val netLogoInstanceKey = CacheKey[WithInstance[AbstractNetLogoTask.NetoLogoInstance]]()

  /**
   * Workspace (either file or directory)
   * @return
   */
  def workspace: AbstractNetLogoTask.Workspace

  /**
   * Commands to run
   * @return
   */
  def go: Seq[FromContext[String]]
  def setup: Seq[FromContext[String]]

  /**
   * Mapping of prototypes
   * @return
   */
  def mapped: MappedInputOutputConfig

  def netLogoFactory: NetLogoFactory
  def ignoreError: Boolean
  def seed: Option[Val[Int]]
  def external: External
  def reuseWorkspace: Boolean
  def ignoreErrorOnDispose: Boolean

  def switch3d: Boolean

  override def validate = Validate: p =>
    import p.*
    val allInputs = External.PWD :: p.inputs.toList
    go.flatMap(_.validate(allInputs)) ++
      External.validate(external)(allInputs) ++
      AbstractNetLogoTask.validateNetLogoInputTypes(mapped.inputs.map(_.v))

  override def apply(taskExecutionBuildContext: TaskExecutionBuildContext) = TaskExecution: parameters =>
    import parameters._

    val pool = executionContext.cache.getOrElseUpdate(netLogoInstanceKey):
      AbstractNetLogoTask.createPool(netLogoFactory, workspace, reuseWorkspace, ignoreErrorOnDispose = ignoreErrorOnDispose, switch3d = switch3d)

    pool: instance =>

      val resolver = External.relativeResolver(instance.workspaceDirectory)(_)
      val context = parameters.context + (External.PWD → instance.workspaceDirectory.getAbsolutePath)
      val preparedContext = External.deployInputFilesAndResources(external, context, resolver)

      AbstractNetLogoTask.executeNetLogo(instance.netLogo, "clear-all")
      seed.foreach { s => AbstractNetLogoTask.executeNetLogo(instance.netLogo, s"random-seed ${context(s)}") }

      for (cmd ← setup.map(_.from(context))) AbstractNetLogoTask.executeNetLogo(instance.netLogo, cmd, ignoreError)

      for (inBinding <- mapped.inputs)
      do
        val v = AbstractNetLogoTask.netLogoCompatibleType(preparedContext(inBinding.v))
        AbstractNetLogoTask.setGlobal(instance.netLogo, inBinding.name, v)

      for (cmd ← go.map(_.from(context))) AbstractNetLogoTask.executeNetLogo(instance.netLogo, cmd, ignoreError)


      val contextResult =
        External.fetchOutputFiles(external, netlogoTask.outputs, preparedContext, resolver, Seq(instance.workspaceDirectory)) ++ mapped.outputs.map { mapped =>
          def outputValue = AbstractNetLogoTask.report(instance.netLogo, mapped.name)
          AbstractNetLogoTask.netLogoValueToVal(outputValue, mapped)
        }

      contextResult

