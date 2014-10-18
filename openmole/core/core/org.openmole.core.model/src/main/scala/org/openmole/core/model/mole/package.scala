/*
 * Copyright (C) 20/02/13 Romain Reuillon
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

package org.openmole.core.model

package object mole {

  case class Hooks(map: Map[ICapsule, Traversable[IHook]])
  case class Sources(map: Map[ICapsule, Traversable[ISource]])

  implicit def hooksToMap(h: Hooks) = h.map.withDefault(_ ⇒ List.empty)
  implicit def mapToHooks(m: Map[ICapsule, Traversable[IHook]]) = new Hooks(m)

  implicit def sourcesToMap(s: Sources) = s.map.withDefault(_ ⇒ List.empty)
  implicit def mapToSources(m: Map[ICapsule, Traversable[ISource]]) = new Sources(m)

  object Hooks {
    def empty = Map.empty[ICapsule, Traversable[IHook]]
  }

  object Sources {
    def empty = Map.empty[ICapsule, Traversable[ISource]]
  }

}
