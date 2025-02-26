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
import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import cats.implicits.*
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.argument.OptionalArgument

package object file {

  implicit class DomainFileDecorator(f: File) {
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

  implicit def prototypeOfFileIsFinite: DiscreteFromContextDomain[Val[File], File] = prototype => Domain(FromContext.prototype(prototype).map { _.listFilesSafe.iterator })
  implicit def fileIsDiscrete: DiscreteFromContextDomain[File, File] = f =>
    Domain(
      FromContext.value(f.listFilesSafe.iterator),
      validation = Validate { _ =>
        if (!f.exists()) Seq(throw UserBadDataError(s"Directory $f used in domain doesn't exist"))
        else if (!f.isDirectory) Seq(throw UserBadDataError(s"File $f used in domain, should be a directory")) else Seq()
      }
    )

}