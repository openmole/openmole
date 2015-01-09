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
import org.openmole.plugin.tool.groovy.ContextToGroovyCode
import org.openmole.core.workflow.tools._

import scala.util.Random

object MapDomain {

  def apply[I, O](domain: Domain[I] with Discrete[I], name: String, source: String) =
    new MapDomain[I, O](domain, name, source)

}

sealed class MapDomain[-I, +O](domain: Domain[I] with Discrete[I], name: String, val source: String) extends Domain[O] with Discrete[O] with ContextToGroovyCode { d ⇒

  override def inputs = domain.inputs
  def libraries = Seq.empty

  override def iterator(context: Context)(implicit rng: Random): Iterator[O] =
    domain.iterator(context).map {
      e ⇒
        val b = context.toBinding
        b.setVariable(name, e)
        execute(b).asInstanceOf[O]
    }
}
