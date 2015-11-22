/*
 * Copyright (C) 2011 romain
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

package org.openmole.plugin.domain.modifier

import org.openmole.core.tools.obj.ClassUtils
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.tools.FromContext
import collection.JavaConversions._
import ClassUtils._

import scala.util.Random

object GroupDomain {

  implicit def isDiscrete[T: Manifest, D] = new Discrete[Array[T], GroupDomain[T, D]] {
    override def iterator(domain: GroupDomain[T, D]) = domain.iterator()
    override def inputs(domain: GroupDomain[T, D]) = domain.inputs
  }

  def apply[T: Manifest, D](d: D, size: FromContext[Int])(implicit discrete: Discrete[T, D]) =
    new GroupDomain(d, size)

}

sealed class GroupDomain[T: Manifest, D](val d: D, val size: FromContext[Int])(implicit val discrete: Discrete[T, D]) {

  def inputs = discrete.inputs(d)

  def iterator() =
    for {
      it ← discrete.iterator(d)
      s ← size
    } yield it.grouped(s) map (_.toArray)

}
