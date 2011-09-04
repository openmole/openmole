/*
 * Copyright (C) 2010 reuillon
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

package org.openmole.core.implementation.tools

import org.openmole.core.implementation.data.Context
import org.openmole.core.implementation.data.Variable
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.data.IVariable
import org.openmole.core.model.tools.IVariablesBuffer
import org.openmole.core.implementation.data.Prototype._
import scala.collection.immutable.TreeMap
import scala.collection.mutable.ListBuffer

object VariablesBuffer {
  
  def apply(context: Iterable[IVariable[_]]): VariablesBuffer = {
    val ret = new VariablesBuffer
    ret ++= context
  }

}


class VariablesBuffer extends IVariablesBuffer {
  val variableBuffers = new ListBuffer[IVariable[_]]
  
  override def toIterable = variableBuffers.toIterable
  
  override def += (v: IVariable[_]): this.type = {
    variableBuffers += v
    this
  }
  
  override def ++= (context: Iterable[IVariable[_]]): this.type = {
    for(v <- context) this += v
    this
  }
}
