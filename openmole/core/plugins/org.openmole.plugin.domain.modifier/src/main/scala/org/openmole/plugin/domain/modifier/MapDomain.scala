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

import org.openmole.core.model.data._
import org.openmole.core.model.domain._
import org.openmole.plugin.tools.groovy.ContextToGroovyCode
import org.openmole.core.implementation.tools._

object MapDomain {

  def apply[I, O](domain: Domain[I] with Discrete[I], name: String, code: String) =
    new MapDomain[I, O](domain, name, code)

}

sealed class MapDomain[-I, +O](domain: Domain[I] with Discrete[I], name: String, code: String) extends Domain[O] with Discrete[O] {

  def this(domain: Domain[I] with Discrete[I], prototype: Prototype[I], code: String) = this(domain, prototype.name, code)

  override def inputs = domain.inputs

  @transient lazy val contextToGroovyCode = new ContextToGroovyCode(code, Iterable.empty)

  override def iterator(context: Context): Iterator[O] =
    domain.iterator(context).map {
      e ⇒
        val b = context.toBinding
        b.setVariable(name, e)
        contextToGroovyCode.execute(b).asInstanceOf[O]
    }
}
