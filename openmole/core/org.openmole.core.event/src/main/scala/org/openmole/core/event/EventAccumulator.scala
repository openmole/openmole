/*
 * Copyright (C) 2015 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
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
package org.openmole.core.event

import scala.concurrent.stm._

object EventAccumulator {

  def apply[T, E](t: Seq[T], max: Option[Int] = None)(f: PartialFunction[(T, Event[T]), E])(implicit eventDispatcher: EventDispatcher) = {
    val accumulator = new EventAccumulator[E](max)
    val listener = f andThen accumulator.accumulate _
    t.foreach(_ listen listener)
    accumulator
  }

}

class EventAccumulator[E](val maxEvents: Option[Int] = None) {

  def events = _events.single().reverse
  def read = atomic { implicit ctx ⇒
    val res = _events()
    _events() = Nil
    _number() = 0
    res
  }

  private lazy val _events = Ref(List[E]())
  private lazy val _number = Ref(0)

  def accumulate(e: E) = atomic { implicit ctx ⇒
    _events() = e :: _events()
    maxEvents.foreach {
      max ⇒ if (_number() > max) _events() = _events().take(max)
    }
  }

}
