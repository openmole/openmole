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

package org.openmole.core.context

import scala.collection.immutable.TreeMap

object PrototypeSet {
  implicit def traversableToProtoypeSet(ps: Traversable[Val[_]]) = PrototypeSet(ps.toSeq)
  val empty = PrototypeSet(Seq.empty)
}

case class PrototypeSet(prototypes: Seq[Val[_]], explore: Set[String] = Set.empty) extends Iterable[Val[_]] { self ⇒

  @transient lazy val prototypeMap: Map[String, Val[_]] =
    TreeMap.empty[String, Val[_]] ++ prototypes.map { d ⇒ (d.name, d) }

  /**
   * Get the @link{Data} by its name as an Option.
   *
   * @param name the name of the @link{Data}
   * @return Some(data) if it is present in the data set None otherwise
   */
  def apply(name: String): Option[Val[_]] = prototypeMap.get(name)

  /**
   * Test if a variable with a given name is present in the data set.
   *
   * @param name the name of the @link{Data}
   * @return true if the variable with a matching name is present in the data
   * set false otherwise
   */
  def contains(name: String): Boolean = prototypeMap.contains(name)

  def explored: Seq[Val[_]] = prototypes.filter(explored)

  def explored(p: Val[_]): Boolean = p.`type`.isArray && explore.contains(p.name)

  override def iterator: Iterator[Val[_]] = prototypes.iterator

  def explore(d: String*) = copy(explore = explore ++ d)

  def ++(d: Traversable[Val[_]]) = copy(prototypes = d.toList ::: prototypes.toList)

  def +(set: PrototypeSet): PrototypeSet = copy(prototypes = set.prototypes.toList ::: prototypes.toList)

  def +(d: Val[_]) = copy(prototypes = d :: prototypes.toList)

  def -(d: Val[_]) = copy(prototypes = prototypes.filter(_.name != d.name).toList)

  def --(d: Traversable[Val[_]]) = {
    val dset = d.map(_.name).toSet
    copy(prototypes = prototypes.filter(p ⇒ !dset.contains(p.name)).toList)
  }

  def contains(data: Val[_]) = prototypeMap.contains(data.name)

  def toMap = prototypeMap

}
