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

import java.io.File
import org.openmole.core.workflow.data.Prototype
import org.openmole.core.workflow.builder
import org.openmole.misc.tools.service.OS
import org.openmole.misc.macros.Keyword._

package external {

  import org.openmole.core.workflow.tools.ExpandedString

  trait ExternalPackage {
    implicit def inputsFileDecorator(i: org.openmole.core.workflow.builder.inputs.type) = {
      def +=[T <: ExternalTaskBuilder](p: Prototype[File], name: ExpandedString, link: Boolean = false) =
        (_: T).addInput(p, name, link)
    }

    implicit def outputsFileDecorator(i: org.openmole.core.workflow.builder.outputs.type) =
      add[{ def addOutput(n: ExpandedString, p: Prototype[File]) }]

    lazy val resources =
      new {
        def +=[T <: ExternalTaskBuilder](file: File, name: Option[ExpandedString] = None, link: Boolean = false, os: OS = OS()) =
          (_: T).addResource(file, name, link, os)
      }
  }
}

package object external extends ExternalPackage