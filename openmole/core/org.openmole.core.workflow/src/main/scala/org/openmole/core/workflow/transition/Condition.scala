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

package org.openmole.core.workflow.transition

import org.openmole.core.tools.script.GroovyProxy
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.tools._
import org.openmole.core.tools.script._

object Condition {

  val True = new Condition {
    def evaluate(context: Context): Boolean = true
  }

  val False = new Condition {
    def evaluate(context: Context): Boolean = false
  }

  implicit def function2IConditionConverter(f: Context ⇒ Boolean) = new Condition {
    override def evaluate(context: Context) = f(context)
  }

  def apply(code: String) = new Condition {
    @transient lazy val groovyProxy = new GroovyProxy(code, Iterable.empty) with GroovyContextAdapter
    override def evaluate(context: Context) = groovyProxy.execute(context).asInstanceOf[Boolean]
  }

}

trait Condition { c ⇒

  /**
   *
   * Evaluate the value of this condition in a given context.
   *
   * @param context the context in which the condition is evaluated
   * @return the value of this condition
   */
  def evaluate(context: Context): Boolean

  def unary_! = new Condition {
    override def evaluate(context: Context): Boolean = !c.evaluate(context)
  }

}
