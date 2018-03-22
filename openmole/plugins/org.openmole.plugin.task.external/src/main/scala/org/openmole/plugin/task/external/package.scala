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

import org.openmole.core.dsl._
import org.openmole.core.tools.service.OS

package external {

  import java.io._

  import org.openmole.core.context.Val
  import org.openmole.core.expansion.{ FromContext, ToFromContext }
  import org.openmole.core.workflow.builder.InputOutputBuilder

  trait ExternalPackage {

    implicit class InputFileAsDecorator(v: Val[File]) {
      def as(name: FromContext[String], link: Boolean = false) = External.InputFile(v, name, link)
    }

    implicit class InputFileArrayAsDecorator(v: Val[Array[File]]) {
      def as(prefix: FromContext[String], suffix: FromContext[String] = "", link: Boolean = false) = External.InputFileArray(v, prefix, suffix, link)
    }

    implicit class ResourceDecorator[T](file: File) {
      def as(name: OptionalArgument[FromContext[String]] = None, link: Boolean = false, os: OS = OS()) =
        External.Resource(file, name.getOrElse(file.getName), link = link, os = os)
    }

    implicit class OutputFileDecorator[T](t: T)(implicit toFromContext: ToFromContext[T, String]) {
      def as(p: Val[File]) = External.OutputFile(toFromContext(t), p)
    }

    lazy val inputFiles = new {
      /**
       * Copy a file or directory from the dataflow to the task workspace
       */
      def +=[T: ExternalBuilder: InputOutputBuilder](inputFile: External.InputFile): T ⇒ T =
        implicitly[ExternalBuilder[T]].inputFiles add inputFile andThen (inputs += inputFile.prototype)

      @deprecated
      def +=[T: ExternalBuilder: InputOutputBuilder](p: Val[File], name: FromContext[String], link: Boolean = false): T ⇒ T = this += (p as (name, link))
    }

    lazy val inputFileArrays = new {
      /**
       * Copy an array of files or directory from the dataflow to the task workspace. The files
       * in the array are named prefix$nSuffix where $n i the index of the file in the array.
       */
      def +=[T: ExternalBuilder: InputOutputBuilder](inputFileArray: External.InputFileArray): T ⇒ T =
        (implicitly[ExternalBuilder[T]].inputFileArrays add inputFileArray) andThen (inputs += inputFileArray.prototype)

      @deprecated
      def +=[T: ExternalBuilder: InputOutputBuilder](p: Val[Array[File]], prefix: FromContext[String], suffix: FromContext[String] = "", link: Boolean = false): T ⇒ T =
        this += (p as (prefix = prefix, suffix = suffix, link = link))
    }

    lazy val outputFiles = new {
      /**
       * Get a file generate by the task and inject it in the dataflow
       *
       */
      def +=[T: ExternalBuilder: InputOutputBuilder](outputFile: External.OutputFile): T ⇒ T =
        (implicitly[ExternalBuilder[T]].outputFiles add outputFile) andThen (outputs += outputFile.prototype)

      @deprecated
      def +=[T: ExternalBuilder: InputOutputBuilder](name: FromContext[String], p: Val[File]): T ⇒ T =
        this += (name as p)
    }

    lazy val resources =
      new {
        /**
         * Copy a file from your computer in the workspace of the task
         */
        def +=[T: ExternalBuilder](resource: External.Resource*): T ⇒ T =
          resource.map(implicitly[ExternalBuilder[T]].resources add _)

        @deprecated
        def +=[T: ExternalBuilder](file: File, name: OptionalArgument[FromContext[String]] = None, link: Boolean = false, os: OS = OS()): T ⇒ T =
          implicitly[ExternalBuilder[T]].resources add (file as (name, link, os))

      }
  }
}

package object external extends ExternalPackage {
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