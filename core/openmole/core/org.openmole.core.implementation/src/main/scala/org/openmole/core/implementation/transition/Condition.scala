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

package org.openmole.core.implementation.transition

import org.openmole.core.model.data._
import org.openmole.core.model.transition._
import org.openmole.misc.tools.script._
import org.openmole.core.implementation.tools._

object Condition {

  def apply(_code: String) = new Condition {
    val code = _code
  }

}

trait Condition extends ICondition {

  def code: String

  @transient lazy val groovyProxy = new GroovyProxy(code, Iterable.empty) with GroovyContextAdapter

  override def evaluate(context: Context) = groovyProxy.execute(context).asInstanceOf[Boolean]

  def unary_! = Condition(s"!($code)")

}
