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
  import org.openmole.core.expansion.FromContext
  import org.openmole.core.workflow.builder.InputOutputBuilder

  trait ExternalPackage {

    lazy val inputFiles = new {
      /**
       * Copy a file or directory from the dataflow to the task workspace
       *
       * @param p the prototype of the data containing the file to be copied
       * @param name the destination name of the file in the task workspace
       * @param link @see addResource
       *
       */
      def +=[T: ExternalBuilder: InputOutputBuilder](p: Val[File], name: FromContext[String], link: Boolean = false): T ⇒ T =
        (implicitly[ExternalBuilder[T]].inputFiles add External.InputFile(p, name, link)) andThen
          (inputs += p)
    }

    lazy val inputFileArrays = new {
      /**
       * Copy an array of files or directory from the dataflow to the task workspace. The files
       * in the array are named prefix$nSuffix where $n i the index of the file in the array.
       *
       * @param p the prototype of the data containing the array of files to be copied
       * @param prefix the prefix for naming the files
       * @param suffix the suffix for naming the files
       * @param link @see addResource
       *
       */
      def +=[T: ExternalBuilder: InputOutputBuilder](p: Val[Array[File]], prefix: FromContext[String], suffix: FromContext[String] = "", link: Boolean = false): T ⇒ T =
        (implicitly[ExternalBuilder[T]].inputFileArrays add External.InputFileArray(prototype = p, prefix = prefix, suffix = suffix, link = link)) andThen
          (inputs += p)
    }

    lazy val outputFiles = new {
      /**
       * Get a file generate by the task and inject it in the dataflow
       *
       * @param name the name of the file to be injected
       * @param p the prototype that is injected
       *
       */
      def +=[T: ExternalBuilder: InputOutputBuilder](name: FromContext[String], p: Val[File]): T ⇒ T =
        (implicitly[ExternalBuilder[T]].outputFiles add External.OutputFile(name, p)) andThen
          (outputs += p)
    }

    lazy val resources =
      new {
        /**
         * Copy a file from your computer in the workspace of the task
         *
         * @param file the file or directory to copy in the task workspace
         * @param name the destination name of the file in the task workspace, by
         * default it is the same as the original file name
         * @param link tels if the entire content of the file should be copied or
         * if a symbolic link is suitable. In the case link is set to true openmole will
         * try to use a symbolic link if available on your system.
         *
         */
        def +=[T: ExternalBuilder](file: File, name: OptionalArgument[FromContext[String]] = None, link: Boolean = false, os: OS = OS()): T ⇒ T =
          implicitly[ExternalBuilder[T]].resources add External.Resource(file, name.getOrElse(file.getName), link, os)
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

    directory.listRecursive(_ ⇒ true).map(fileInformation).map(i ⇒ s"$margin$i").mkString("\n")
  }

}