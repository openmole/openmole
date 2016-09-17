/*
 * Copyright (C) 2012 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.domain

import java.io.File

import org.openmole.core.workflow.data._
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.tools._
import org.openmole.core.workflow.dsl._

package object file {

  implicit def domainFileDecorator(f: File) = new {
    def files: ListFilesDomain = files()
    def files(
      directory: OptionalArgument[FromContext[String]] = OptionalArgument(),
      recursive: Boolean                               = false,
      filter:    OptionalArgument[FromContext[String]] = OptionalArgument()
    ): ListFilesDomain = ListFilesDomain(f, directory, recursive, filter)

    def paths: ListPathsDomain = paths()
    def paths(
      directory: OptionalArgument[FromContext[String]] = OptionalArgument(),
      recursive: Boolean                               = false,
      filter:    OptionalArgument[FromContext[String]] = OptionalArgument()
    ): ListPathsDomain = ListPathsDomain(f, directory, recursive, filter)

    def select(path: FromContext[String]) = SelectFileDomain(f, path)
  }

  implicit def prototypeOfFileIsFinite = new Finite[Prototype[File], File] {
    override def computeValues(prototype: Prototype[File]) = FromContext { (ctx, rng) ⇒ ctx(prototype).listFilesSafe }
  }

  implicit def fileIsFinite = new Finite[File, File] {
    override def computeValues(f: File) = FromContext.value(f.listFilesSafe)
  }

}