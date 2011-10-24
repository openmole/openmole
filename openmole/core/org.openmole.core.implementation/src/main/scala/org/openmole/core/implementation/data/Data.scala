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

package org.openmole.core.implementation.data

import org.openmole.core.model.data.{IData,IPrototype,DataModeMask, IDataMode}

object Data {
  import org.openmole.core.implementation.data.Prototype
  def toArray[T](data: IData[T]): IData[Array[T]] = new Data[Array[T]](Prototype.toArray(data.prototype), data.mode)  

  implicit lazy val dataOrderingOnName = new Ordering[IData[_]] {
    override def compare(left: IData[_], right: IData[_]) = 
      Prototype.prototypeOrderingOnName.compare(left.prototype, right.prototype)
  }
}

class Data[T](val prototype: IPrototype[T], val mode: IDataMode) extends IData[T] {

  def this(prototype: IPrototype[T]) = this(prototype, DataMode.NONE)

  def this(prototype: IPrototype[T], masks: Array[DataModeMask]) = this(prototype, DataMode(masks: _*))

  def this(prototype: IPrototype[T], masks: DataModeMask*) = this(prototype, DataMode(masks: _*))
    
  def this(name: String, `type`: Class[T]) = this(new Prototype[T](name, `type`))

  def this(name: String, `type`: Class[T], masks: Array[DataModeMask]) = this(new Prototype[T](name, `type`), masks)

}
