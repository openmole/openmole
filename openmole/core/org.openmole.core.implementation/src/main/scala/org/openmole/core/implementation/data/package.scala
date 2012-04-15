/*
 * Copyright (C) 2012 reuillon
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

package org.openmole.core.implementation

import org.openmole.core.model.data.IData
import org.openmole.core.model.data.IPrototype

package object data {
  implicit def tupleToParameter[T](t: (IPrototype[T], T)) = new Parameter(t._1, t._2)
  implicit def prototypeToData(p: IPrototype[_]) = DataSet(p)
  implicit def dataIterableDecorator(data: Traversable[IData[_]]) = new DataSet(data.toList)
  implicit def iterableOfPrototypeToIterableOfDataConverter(prototypes: Traversable[IPrototype[_]]): Traversable[IData[_]] = DataSet(prototypes)
}