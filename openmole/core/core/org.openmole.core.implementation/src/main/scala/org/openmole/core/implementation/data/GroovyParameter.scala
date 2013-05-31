/*
 * Copyright (C) 30/05/13 Romain Reuillon
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.implementation.data

import org.openmole.core.model.data._
import org.openmole.core.implementation.tools._
import org.openmole.misc.tools.script.GroovyProxyPool

object GroovyParameter {

  def apply[T](prototype: Prototype[T], value: String, `override`: Boolean = false) = {
    val (p, v, o) = (prototype, value, `override`)
    new Parameter[T] {
      @transient lazy val groovyProxy = GroovyProxyPool(v)
      def prototype = p
      def value(ctx: Context) = groovyProxy(ctx).asInstanceOf[T]
      def `override` = o
    }
  }

}
