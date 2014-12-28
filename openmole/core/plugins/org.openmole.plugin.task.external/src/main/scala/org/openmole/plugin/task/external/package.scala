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
import org.openmole.core.model.data.Prototype
import org.openmole.core.implementation.builder
import org.openmole.misc.tools.service.OS

package external {
  trait ExternalPackage {
    type InputsDecorator = external.InputsDecorator
    type OutputsDecorator = external.OutputsDecorator
    lazy val resources = external.resources
  }
}

package object external {

  implicit class InputsDecorator(i: builder.inputs.type) {
    def +=(p: Prototype[File], name: String, link: Boolean = false): builder.Op[ExternalTaskBuilder] =
      (_: ExternalTaskBuilder).addInput(p, name, link)
  }

  implicit class OutputsDecorator(i: builder.outputs.type) {
    def +=(name: String, p: Prototype[File]): builder.Op[ExternalTaskBuilder] =
      (_: ExternalTaskBuilder).addOutput(name, p)
  }

  lazy val resources = new {
    def +=(file: File, name: Option[String] = None, link: Boolean = false, os: OS = OS()): builder.Op[ExternalTaskBuilder] =
      (_: ExternalTaskBuilder).addResource(file, name, link, os)
  }

}