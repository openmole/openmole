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

package org.openmole.plugin.task.external

import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import org.openmole.tool.system.OS
//import java.io._
import org.openmole.core.context.Val
import org.openmole.core.setter.{InputOutputBuilder, Setter}

object InputFilesKeyword:
  object InputFilesSetter:
    given [T: {ExternalBuilder as eb, InputOutputBuilder}]: Setter[InputFilesSetter, T] = setter =>
      eb.inputFiles add External.InputFile(setter.p, setter.name, setter.link) andThen (inputs += setter.p)

  case class InputFilesSetter(p: Val[File], name: FromContext[String], link: Boolean)

class InputFilesKeyword:
  /**
   * Copy a file or directory from the dataflow to the task workspace
   */
  def +=(p: Val[File], name: FromContext[String], link: Boolean = false) = InputFilesKeyword.InputFilesSetter(p, name, link = link)

lazy val inputFiles = new InputFilesKeyword

object OutputFilesKeyword:
  object OutputFilesSetter:
    given [T: {ExternalBuilder as eb, InputOutputBuilder}]: Setter[OutputFilesSetter, T] = setter =>
      (eb.outputFiles add External.OutputFile(setter.name, setter.p)) andThen (outputs += setter.p)

  case class OutputFilesSetter(name: FromContext[String], p: Val[File])

class OutputFilesKeyword:
  /**
   * Get a file generate by the task and inject it in the dataflow
   *
   */
  def +=(name: FromContext[String], p: Val[File]) = OutputFilesKeyword.OutputFilesSetter(name, p)

lazy val outputFiles = new OutputFilesKeyword

object ResourcesKeyword:
  object ResourceSetter:
    given [T: ExternalBuilder as eb]: Setter[ResourceSetter, T] =
      case setter: FileResourceSetter =>
        def resource = External.FileResource(setter.file, setter.name.getOrElse(setter.file.getName), link = setter.link, os = setter.os)
        eb.resources.add(resource, head = setter.head)
      case setter: EmptyDirectoryResourceSetter =>
        def resource = External.EmptyDirectoryResource(setter.name, os = setter.os)
        eb.resources.add(resource, head = setter.head)

  trait ResourceSetter
  case class FileResourceSetter(file: File, name: OptionalArgument[FromContext[String]] = None, link: Boolean, os: OS, head: Boolean) extends ResourceSetter
  case class EmptyDirectoryResourceSetter(name: FromContext[String], os: OS, head: Boolean) extends ResourceSetter

  case class EmptyDirectory(name: FromContext[String])

class ResourcesKeyword:
  /**
   * Copy a file from your computer in the workspace of the task
   */
  def +=(file: File | ResourcesKeyword.EmptyDirectory, name: OptionalArgument[FromContext[String]] = None, link: Boolean = false, os: OS = OS(), head: Boolean = false) =
    file match
      case file: File => ResourcesKeyword.FileResourceSetter(file, name, link = link, os = os, head = head)
      case e: ResourcesKeyword.EmptyDirectory =>
        val nameValue = name.getOrElse(e.name)
        ResourcesKeyword.EmptyDirectoryResourceSetter(nameValue, os, head)


lazy val resources = new ResourcesKeyword
export ResourcesKeyword.EmptyDirectory

object EnvironmentVariable:
  implicit def fromTuple[N, V](tuple: (N, V))(implicit toFromContextN: ToFromContext[N, String], toFromContextV: ToFromContext[V, String]): EnvironmentVariable =
    EnvironmentVariable(toFromContextN.convert(tuple._1), toFromContextV.convert(tuple._2))

case class EnvironmentVariable(name: FromContext[String], value: FromContext[String])

trait EnvironmentVariables[T]:
  def environmentVariables: monocle.Lens[T, Vector[EnvironmentVariable]]


