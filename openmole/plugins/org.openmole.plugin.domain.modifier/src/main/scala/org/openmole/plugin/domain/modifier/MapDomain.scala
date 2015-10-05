/*
 * Copyright (C) 2010 Romain Reuillon
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

import org.openmole.core.workflow.data._
import org.openmole.core.workflow.domain._
import org.openmole.core.workflow.tools._
import scala.reflect.runtime.universe._

import scala.util.Random

object MapDomain {

  implicit def isDiscrete[I, O, D] = new Discrete[O, MapDomain[I, O, D]] {
    override def iterator(domain: MapDomain[I, O, D], context: Context)(implicit rng: RandomProvider): Iterator[O] =
      domain.iterator(context)
    override def inputs(domain: MapDomain[I, O, D]) = domain.inputs
  }

  def apply[I: TypeTag, O: TypeTag, D](d: D, source: String)(implicit discrete: Discrete[I, D]) =
    new MapDomain[I, O, D](d, s"{$source}: (${implicitly[TypeTag[I]].tpe} => ${implicitly[TypeTag[O]].tpe})")

}

sealed class MapDomain[-I, +O, D](val domain: D, val source: String)(implicit discrete: Discrete[I, D]) { d ⇒

  def inputs = discrete.inputs(domain)
  @transient lazy val proxy = ScalaWrappedCompilation.raw(source)

  def iterator(context: Context)(implicit rng: RandomProvider): Iterator[O] =
    discrete.iterator(domain, context).map {
      e ⇒ proxy.run(context).asInstanceOf[I ⇒ O](e)
    }
}
