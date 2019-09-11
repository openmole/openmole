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
import org.openmole.core.expansion._
import org.openmole.core.tools.io.Prettifier._
import org.openmole.core.workflow.builder.MappedInputOutputConfig
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.validation._
import org.openmole.core.workspace.NewFile
import org.openmole.plugin.task.external._
import org.openmole.plugin.tool.netlogo.NetLogo
import org.openmole.tool.cache._

import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer

object NetLogoTask {
  sealed trait Workspace

  /**
   * Workspace is either a script, or a full directory
   */
  object Workspace {
    case class Script(script: File, name: String) extends Workspace
    case class Directory(directory: File, name: String, script: String) extends Workspace
  }

  /**
   * Errors as [[UserBadDataError]]
   * @param msg
   * @param f
   * @tparam T
   * @return
   */
  def wrapError[T](msg: String)(f: ⇒ T): T =
    try f
    catch {
      case e: InternalProcessingError ⇒ throw e
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

  def dispose(netLogo: NetLogo, ignoreErrorOnDispose: Boolean) =
    withThreadClassLoader(netLogo.getNetLogoClassLoader) {
      try netLogo.dispose()
      catch {
        //FIXME it hapen with the nw extension, this error actually leaks memory. Bug report: https://github.com/NetLogo/NetLogo/issues/1766
        case t: NoClassDefFoundError ⇒ if (!ignoreErrorOnDispose) throw t
      }
    }

  def createPool(netLogoFactory: NetLogoFactory, workspace: NetLogoTask.Workspace, cached: Boolean, ignoreErrorOnDispose: Boolean, switch3d: Boolean)(implicit newFile: NewFile) = {
    def createInstance = {
      val workspaceDirectory = newFile.newDir("netlogoworkpsace")
      NetLogoTask.openNetLogoWorkspace(netLogoFactory, workspace, workspaceDirectory, switch3d)
    }

    def destroyInstance(instance: NetLogoTask.NetoLogoInstance) = {
      instance.directory.recursiveDelete
      dispose(instance.netLogo, ignoreErrorOnDispose)
    }

    WithInstance[NetLogoTask.NetoLogoInstance](
      () ⇒ createInstance,
      close = destroyInstance,
      pooled = cached
    )
  }

  /**
   * Ensure a variable type is compatible with NetLogo. Goes recursively inside arrays at any level.
   * @param x
   * @return
   */
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
        case x: AnyRef ⇒ x // Double, String and Boolean are unchanged
      }
      v.asInstanceOf[AnyRef]
    }

    x match {
      case x: Array[_] ⇒ convertArray(x)
      case x           ⇒ safeType(x)
    }
  }

  /**
   * Convert a netlogo collection to a Variable for which the prototype is expected to have the corresponding depth.
   *
   * @param netlogoCollection
   * @param prototype
   * @return
   */
  def netLogoArrayToVariable(netlogoCollection: AbstractCollection[Any], prototype: Val[_]) = {
    // get arrayType and depth of multiple array prototype
    val (multiArrayType, depth): (ValType[_], Int) = ValType.unArrayify(prototype.`type`)

    // recurse to get sizes, Nested LogoLists assumed rectangular : size of first element is taken for each dimension
    // will fail if the depth of the prototype is not the depth of the LogoList
    @tailrec def getdims(collection: AbstractCollection[Any], dims: Seq[Int], maxdepth: Int): Seq[Int] = {
      maxdepth match {
        case d if d == 1 ⇒ (dims ++ Seq(collection.size()))
        case _           ⇒ getdims(collection.iterator().next().asInstanceOf[AbstractCollection[Any]], dims ++ Seq(collection.size()), maxdepth - 1)
      }
    }

    var dims: Seq[Int] = Seq.empty
    try {
      dims = getdims(netlogoCollection, Seq.empty, depth)
    }
    catch {
      case e: Throwable ⇒ throw new UserBadDataError(e, s"Error when mapping a prototype array of depth ${depth} and type ${multiArrayType} with nested LogoLists")
    }

    // create multi array
    try {
      val array = java.lang.reflect.Array.newInstance(multiArrayType.runtimeClass.asInstanceOf[Class[_]], dims: _*)

      //
      // Manually do conversions to java native types ; necessary to add an output cast feature,
      // e.g. NetLogo numeric ~ java.lang.Double -> Int or String and not necessarily Double,
      // the target type being the one of the prototype
      def safeOutput(value: AnyRef, arrayType: Class[_]) = {
        try {
          arrayType match {
            // all netlogo numeric are java.lang.Double
            case c if c == classOf[Double]  ⇒ value.asInstanceOf[java.lang.Double].doubleValue()
            case c if c == classOf[Float]   ⇒ value.asInstanceOf[java.lang.Double].floatValue()
            case c if c == classOf[Int]     ⇒ value.asInstanceOf[java.lang.Double].intValue()
            case c if c == classOf[Long]    ⇒ value.asInstanceOf[java.lang.Double].longValue()
            // target boolean
            case c if c == classOf[Boolean] ⇒ value.asInstanceOf[java.lang.Boolean].booleanValue()
            // target string assume the origin type has a toString
            case c if c == classOf[String]  ⇒ value.toString
            // try casting anyway - NOTE : untested
            case c                          ⇒ c.cast(value)
          }
        }
        catch {
          case e: Throwable ⇒ throw new UserBadDataError(e, s"Error when casting a variable of type ${value.getClass} to target type ${arrayType}")
        }
      }

      // recurse in the multi array
      def setMultiArray(collection: AbstractCollection[Any], currentArray: AnyRef, arrayType: Class[_], maxdepth: Int): Unit = {
        val it = collection.iterator()
        for (i ← 0 until collection.size) {
          val v = it.next
          maxdepth match {
            case d if d == 1 ⇒ {
              try {
                java.lang.reflect.Array.set(currentArray, i, safeOutput(v.asInstanceOf[AnyRef], arrayType))
              }
              catch {
                case e: Throwable ⇒ throw new UserBadDataError(e, s"Error when adding a variable of type ${v.getClass} in an array of type ${multiArrayType}")
              }
            }
            case _ ⇒
              try {
                setMultiArray(v.asInstanceOf[AbstractCollection[Any]], java.lang.reflect.Array.get(currentArray, i), arrayType, maxdepth - 1)
              }
              catch {
                case e: Throwable ⇒ throw new UserBadDataError(e, s"Error when recursing at depth ${maxdepth} in a multi array of type ${multiArrayType}")
              }
          }
        }
      }

      setMultiArray(netlogoCollection, array, multiArrayType.runtimeClass.asInstanceOf[Class[_]], depth)

      // return Variable
      Variable(prototype.asInstanceOf[Val[Any]], array)
    }
    catch {
      case e: Throwable ⇒ throw new UserBadDataError(e, s"Error constructing array with dims ${dims.toString()} and type ${multiArrayType.runtimeClass}")
    }
  }

  /**
   * Check if provided inputs are compatible with NetLogo
   *
   * @param inputs
   * @return
   */
  def validateNetLogoInputTypes(inputs: Seq[Val[_]]) = {
    def acceptedType(c: Class[_]): Boolean =
      if (c.isArray()) acceptedType(c.getComponentType)
      else Seq(classOf[String], classOf[Int], classOf[Double], classOf[Long], classOf[Float], classOf[File], classOf[Boolean]).contains(c)

    inputs.flatMap {
      case v ⇒
        v.`type`.runtimeClass.asInstanceOf[Class[_]] match {
          case c if acceptedType(c) ⇒ None
          case _                    ⇒ Some(new UserBadDataError(s"""Error for netLogoInput ${v.name} : type ${v.`type`.runtimeClass.toString()} is not managed by NetLogo."""))
        }
    }
  }
}

/**
 * Generic NetLogoTask
 */
trait NetLogoTask extends Task with ValidateTask {

  lazy val netLogoInstanceKey = CacheKey[WithInstance[NetLogoTask.NetoLogoInstance]]()

  /**
   * Workspace (either file or directory)
   * @return
   */
  def workspace: NetLogoTask.Workspace

  /**
   * Commands to run
   * @return
   */
  def launchingCommands: Seq[FromContext[String]]

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

  override def validate = Validate { p ⇒
    import p._
    val allInputs = External.PWD :: inputs.toList
    launchingCommands.flatMap(_.validate(allInputs)) ++
      External.validate(external)(allInputs).apply ++
      NetLogoTask.validateNetLogoInputTypes(mapped.inputs.map(_.v))
  }

  override protected def process(executionContext: TaskExecutionContext) = FromContext { parameters ⇒
    import parameters._

    val pool = executionContext.cache.getOrElseUpdate(netLogoInstanceKey, NetLogoTask.createPool(netLogoFactory, workspace, reuseWorkspace, ignoreErrorOnDispose = ignoreErrorOnDispose, switch3d = switch3d))

    pool { instance ⇒

      val resolver = External.relativeResolver(instance.workspaceDirectory)(_)
      val context = parameters.context + (External.PWD → instance.workspaceDirectory.getAbsolutePath)
      val preparedContext = External.deployInputFilesAndResources(external, context, resolver)

      NetLogoTask.executeNetLogo(instance.netLogo, "clear-all")

      seed.foreach { s ⇒ NetLogoTask.executeNetLogo(instance.netLogo, s"random-seed ${context(s)}") }

      for (inBinding ← mapped.inputs) {
        val v = NetLogoTask.netLogoCompatibleType(preparedContext(inBinding.v))
        NetLogoTask.setGlobal(instance.netLogo, inBinding.name, v)
      }

      for (cmd ← launchingCommands.map(_.from(context))) NetLogoTask.executeNetLogo(instance.netLogo, cmd, ignoreError)

      val contextResult =
        External.fetchOutputFiles(external, outputs, preparedContext, resolver, Seq(instance.workspaceDirectory)) ++ mapped.outputs.map {
          case mapped ⇒
            try {
              val outputValue = NetLogoTask.report(instance.netLogo, mapped.name)
              if (outputValue == null) throw new InternalProcessingError(s"Value of netlogo output ${mapped.name} has been reported as null by netlogo")
              if (!mapped.v.`type`.runtimeClass.isArray) Variable.unsecure(mapped.v, outputValue)
              else {
                val netLogoCollection: util.AbstractCollection[Any] =
                  if (classOf[AbstractCollection[Any]].isAssignableFrom(outputValue.getClass)) outputValue.asInstanceOf[AbstractCollection[Any]]
                  else {
                    val newArray = new util.LinkedList[Any]()
                    newArray.add(outputValue)
                    newArray
                  }
                NetLogoTask.netLogoArrayToVariable(netLogoCollection, mapped.v)
              }
            }
            catch {
              case e: Throwable ⇒
                throw new UserBadDataError(
                  s"Error when fetching netlogo output ${mapped.name} in variable ${mapped.v}:\n" + e.stackStringWithMargin
                )
            }
        }

      contextResult
    }
  }

}
