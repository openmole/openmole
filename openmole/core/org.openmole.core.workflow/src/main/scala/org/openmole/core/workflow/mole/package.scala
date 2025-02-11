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

package org.openmole.core.workflow.mole

import org.openmole.core.workflow.hook.Hook

import scala.language.implicitConversions

trait MolePackage:
  type FromContextSource = org.openmole.core.workflow.mole.FromContextSource

type SubMoleExecution = Long
type Grouping = Int

def Source = FromContextSource

case class Hooks(map: Map[MoleCapsule, Iterable[Hook]])
case class Sources(map: Map[MoleCapsule, Iterable[Source]])

implicit def hooksToMap(h: Hooks): Map[MoleCapsule, Iterable[Hook]] = h.map.withDefault(_ => List.empty)
implicit def sourcesToMap(s: Sources): Map[MoleCapsule, Iterable[Source]] = s.map.withDefault(_ => List.empty)

object Hooks:
  def empty = Map.empty[MoleCapsule, Iterable[Hook]]

  implicit def iterableTupleToHooks(h: Iterable[(MoleCapsule, Hook)]): Hooks = new Hooks(h.groupBy(_._1).map { case (k, v) => k -> v.map(_._2) })
  implicit def mapToHooks(m: Map[MoleCapsule, Iterable[Hook]]): Hooks = new Hooks(m)

object Sources:
  def empty = Map.empty[MoleCapsule, Iterable[Source]]

  implicit def mapToSources(m: Map[MoleCapsule, Iterable[Source]]): Sources = new Sources(m)
  implicit def iterableTupleToSources(s: Iterable[(MoleCapsule, Source)]): Sources = new Sources(s.groupBy(_._1).map { case (k, v) => k -> v.map(_._2) })


