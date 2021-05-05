/*
 * Copyright (C) 2012 reuillon
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

package org.openmole.plugin.domain.file

import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import cats.implicits._
import org.openmole.core.workflow.tools.FileList

object SelectFileDomain {

  implicit def isDiscrete: DiscreteFromContextDomain[SelectFileDomain, File] = domain ⇒ domain.iterator
  implicit def inputs: RequiredInput[SelectFileDomain] = domain ⇒ domain.provider.inputs
  implicit def validate: ExpectedValidation[SelectFileDomain] = domain ⇒ domain.provider.validate

  def apply(base: File, path: FromContext[String]) = new SelectFileDomain(FileList(base, path))
}

class SelectFileDomain(val provider: FromContext[File]) {
  def iterator = provider.map(p ⇒ Iterator(p))
}
