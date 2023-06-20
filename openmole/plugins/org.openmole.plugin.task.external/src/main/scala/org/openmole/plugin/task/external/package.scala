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

package org.openmole.plugin.task

import org.openmole.core.dsl.*
import org.openmole.core.expansion.*

import org.openmole.core.tools.service.OS
//import java.io._
import org.openmole.core.context.Val
import org.openmole.core.workflow.builder.{InputOutputBuilder, Setter}

package external {



  trait ExternalPackage {

    object InputFilesKeyword:
      object InputFilesSetter:
        given [T: ExternalBuilder: InputOutputBuilder]: Setter[InputFilesSetter, T] = setter =>
          implicitly[ExternalBuilder[T]].inputFiles add External.InputFile(setter.p, setter.name, setter.link) andThen (inputs += setter.p)

      case class InputFilesSetter(p: Val[File], name: FromContext[String], link: Boolean)

    class InputFilesKeyword:
      /**
       * Copy a file or directory from the dataflow to the task workspace
       */
      def +=(p: Val[File], name: FromContext[String], link: Boolean = false) = InputFilesKeyword.InputFilesSetter(p, name, link = link)

    lazy val inputFiles = new InputFilesKeyword

    class InputFileArraysKeyword:
      /**
       * Copy an array of files or directory from the dataflow to the task workspace. The files
       * in the array are named prefix$nSuffix where $n i the index of the file in the array.
       */
      def +=[T: ExternalBuilder: InputOutputBuilder](p: Val[Array[File]], prefix: FromContext[String], suffix: FromContext[String] = "", link: Boolean = false): T ⇒ T =
        (implicitly[ExternalBuilder[T]].inputFileArrays add External.InputFileArray(p, prefix, suffix, link)) andThen (inputs += p)

    lazy val inputFileArrays = new InputFileArraysKeyword


    object OutputFilesKeyword:
      object OutputFilesSetter:
        given [T: ExternalBuilder: InputOutputBuilder]: Setter[OutputFilesSetter, T] = setter =>
          (implicitly[ExternalBuilder[T]].outputFiles add External.OutputFile(setter.name, setter.p)) andThen (outputs += setter.p)

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
        given [T: ExternalBuilder]: Setter[ResourceSetter, T] = setter =>
          def resource = External.Resource(setter.file, setter.name.getOrElse(setter.file.getName), link = setter.link, os = setter.os)
          if setter.head
          then implicitly[ExternalBuilder[T]].resources add (resource, head = true)
          else implicitly[ExternalBuilder[T]].resources add resource

      case class ResourceSetter(file: File, name: OptionalArgument[FromContext[String]] = None, link: Boolean, os: OS, head: Boolean)


    class ResourcesKeyword:
      /**
       * Copy a file from your computer in the workspace of the task
       */
      def +=(file: File, name: OptionalArgument[FromContext[String]] = None, link: Boolean = false, os: OS = OS(), head: Boolean = false) = ResourcesKeyword.ResourceSetter(file, name, link = link, os = os, head = head)

    lazy val resources = new ResourcesKeyword
  }
}

package object external extends ExternalPackage {

  object EnvironmentVariable {
    implicit def fromTuple[N, V](tuple: (N, V))(implicit toFromContextN: ToFromContext[N, String], toFromContextV: ToFromContext[V, String]): EnvironmentVariable =
      EnvironmentVariable(toFromContextN.convert(tuple._1), toFromContextV.convert(tuple._2))

  }

  case class EnvironmentVariable(name: FromContext[String], value: FromContext[String])

  trait EnvironmentVariables[T] {
    def environmentVariables: monocle.Lens[T, Vector[EnvironmentVariable]]
  }

  import org.openmole.tool.file._

  def directoryContentInformation(directory: File, margin: String = "  ") = {
    def fileInformation(file: File) = {
      def permissions = {
        val w = if (file.canWrite) "w" else ""
        val r = if (file.canRead) "r" else ""
        val x = if (file.canExecute) "x" else ""
        s"$r$w$x"
      }

      def fileType =
        if (file.isDirectory) "directory"
        else if (file.isSymbolicLink) "link"
        else if (file.isFile) "file"
        else "unknown"

      s"""${directory.toPath.relativize(file.toPath)} (type=$fileType, permissions=$permissions)"""
    }

    directory.listRecursive(_ ⇒ true).filter(_ != directory).map(fileInformation).map(i ⇒ s"$margin$i").mkString("\n")
  }

}