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
import org.openmole.tool.file._

package object file {

  implicit def domainFileDecorator(f: File) = new {
    def files: ListFilesDomain = files()
    def files(
      directory: Option[ExpandedString] = None,
      recursive: Boolean                = false,
      filter:    Option[ExpandedString] = None
    ): ListFilesDomain = ListFilesDomain(f, directory, recursive, filter)
    def select(path: ExpandedString) = SelectFileDomain(f, path)
  }

  implicit def prototypeOfFileIsFinite = new Finite[Prototype[File], File] {
    override def computeValues(prototype: Prototype[File]) = FromContext { (ctx, rng) ⇒ ctx(prototype).listFilesSafe }
  }

  implicit def fileIsFinite = new Finite[File, File] {
    override def computeValues(f: File) = FromContext.value(f.listFilesSafe)
  }

  implicit def filePrototypeDecorator(f: Prototype[File]) = new {
    def /(path: ExpandedString) =
      FromContext { (ctx, rng) ⇒ (ctx(f) / path.from(ctx)(rng)).listFilesSafe }
  }

}