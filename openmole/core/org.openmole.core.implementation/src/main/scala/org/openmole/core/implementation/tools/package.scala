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

import scala.ref.WeakReference

import scala.concurrent.stm._

package object tools {

  implicit def objectToSomeObjectConverter[T](v: T) = Some(v)
  implicit def objectToWeakReferenceConverter[T <: AnyRef](v: T) = new WeakReference[T](v)

  implicit def refDecorator[T](r: Ref[T]) = new {
    def getUpdate(t: T ⇒ T): T = atomic { implicit txn ⇒ val v = r(); r() = t(v); v }
  }

  implicit def longRefDecorator(r: Ref[Long]) = new {
    def next: Long = r.getUpdate(_ + 1)
  }

}