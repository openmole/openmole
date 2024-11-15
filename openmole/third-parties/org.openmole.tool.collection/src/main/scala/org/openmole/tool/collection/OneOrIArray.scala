package org.openmole.tool.collection

import scala.reflect.ClassTag

/*
 * Copyright (C) 2024 Romain Reuillon
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


object OneOrIArray:
  given [T]: Conversion[T | IArray[T], OneOrIArray[T]] = identity
  extension [T](a: OneOrIArray[T])
    def toIArray(using ClassTag[T], ClassTag[IArray[T]]): IArray[T] =
      a match
        case a: T => IArray(a)
        case a: IArray[T] => a

    def value: T | IArray[T] = a

opaque type OneOrIArray[T] = T | IArray[T]
