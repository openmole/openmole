/*
 * Copyright (C) 2013 <mathieu.Mathieu Leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.ide.misc.tools.util

object Converters {
  implicit def tupleToIndexedTuple[S, T](tu: List[Tuple2[S, T]]) = {
    val tt = tu.zipWithIndex.map {
      case (t, i) ⇒ (t._1, t._2, i)
    }
    tt
  }

  implicit def flattenTuple2Options[T, R](l: List[Tuple2[Option[T], Option[R]]]): List[Tuple2[T, R]] =
    l.map {
      _ match {
        case (None, _)          ⇒ None
        case (_, None)          ⇒ None
        case (Some(x), Some(y)) ⇒ Some(x, y)
      }
    }.flatten

  implicit def flattenTupleOptionAny[T, R](l: List[Tuple2[Option[T], R]]): List[Tuple2[T, R]] = {
    l.map {
      _ match {
        case (None, _)    ⇒ None
        case (Some(x), y) ⇒ Some(x, y)
      }
    }.flatten
  }

  implicit def flattenTupleAnyOption[T, R](l: List[Tuple2[T, Option[R]]]): List[Tuple2[T, R]] = {
    l.map {
      _ match {
        case (_, None)    ⇒ None
        case (x, Some(y)) ⇒ Some(x, y)
      }
    }.flatten
  }

}

