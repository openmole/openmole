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

import org.openmole.core.tools.service.OS

package external {

  import java.io._
  import org.openmole.core.workflow.data._
  import org.openmole.core.workflow.tools._

  trait ExternalPackage {

    lazy val inputFiles = new {
      def +=(p: Prototype[File], name: String, link: Boolean = false, toWorkDirectory: Boolean = true) = (_: ExternalTaskBuilder).addInputFile(p, name, link, toWorkDirectory)
    }

    lazy val inputFileArrays = new {
      def +=(p: Prototype[Array[File]], prefix: String, suffix: String = "", link: Boolean = false, toWorkDirectory: Boolean = true) =
        (_: ExternalTaskBuilder).addInputFileArray(p, prefix, suffix, link, toWorkDirectory)
    }

    lazy val outputFiles = new {
      def +=(name: String, p: Prototype[File], fromWorkDirectory: Boolean = true) = (_: ExternalTaskBuilder).addOutputFile(name, p, fromWorkDirectory)
    }

    lazy val resources =
      new {
        def +=(file: File, name: Option[ExpandedString] = None, link: Boolean = false, fromWorkDirectory: Boolean = false, os: OS = OS()) =
          (_: ExternalTaskBuilder).addResource(file, name, link, fromWorkDirectory, os)
      }
  }
}

package object external extends ExternalPackage