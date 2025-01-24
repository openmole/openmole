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
  implicit def traversableToProtoypeSet(ps: Iterable[Val[?]]): PrototypeSet = PrototypeSet(ps.toSeq)
  val empty = PrototypeSet(Vector.empty)

  def apply(prototypes: Seq[Val[?]], explore: Set[String] = Set.empty) = new PrototypeSet(prototypes.reverse.distinct.reverse, explore)
  def copy(prototypeSet: PrototypeSet)(prototypes: Seq[Val[?]] = prototypeSet.prototypes, explore: Set[String] = prototypeSet.explore) = apply(prototypes, explore)
}

/**
 * An ordered set of prototypes
 * @param prototypes the sequence of prototypes
 * @param explore names of prototypes which have already been explored
 */
class PrototypeSet(val prototypes: Seq[Val[?]], val explore: Set[String] = Set.empty) extends Iterable[Val[?]] { self ⇒

  @transient lazy val prototypeMap: Map[String, Val[?]] =
    TreeMap.empty[String, Val[?]] ++ prototypes.map { d ⇒ (d.name, d) }

  /**
   * Get the prototype by its name as an Option.
   *
   * @param name the name of the prototype
   * @return Some(data) if it is present in the prototype set, None otherwise
   */
  def apply(name: String): Option[Val[?]] = prototypeMap.get(name)

  /**
   * Test if a variable with a given name is present in the data set.
   *
   * @param name the name of the @link{Data}
   * @return true if the variable with a matching name is present in the data
   * set false otherwise
   */
  def contains(name: String): Boolean = prototypeMap.contains(name)

  /**
   * get the explored prototypes
   * @return
   */
  def explored: Seq[Val[?]] = prototypes.filter(explored)

  /**
   * check if a prototype has been explored
   * @param p
   * @return
   */
  def explored(p: Val[?]): Boolean = p.`type`.isArray && explore.contains(p.name)

  override def iterator: Iterator[Val[?]] = prototypes.iterator

  /**
   * Explore the given prototypes (by name)
   * @param d names of prototypes to explore
   * @return
   */
  def explore(d: String*): PrototypeSet = PrototypeSet.copy(this)(explore = explore ++ d)

  /**
   * Concatenate with a set of prototypes
   * @param d
   * @return
   */
  def ++(d: Iterable[Val[?]]) = PrototypeSet.copy(this)(prototypes = prototypes ++ d)

  /**
   * Prepend a PrototypeSet
   * @param set
   * @return
   */
  def +(set: PrototypeSet): PrototypeSet = PrototypeSet.copy(this)(prototypes = prototypes ++ set.prototypes)

  /**
   * Prepend a prototype
   * @param d
   * @return
   */
  def +(d: Val[?]) = PrototypeSet.copy(this)(prototypes = prototypes ++ Seq(d))

  /**
   * Remove a prototype
   * @param d
   * @return
   */
  def -(d: Val[?]) = PrototypeSet.copy(this)(prototypes = prototypes.filter(_.name != d.name).toList)

  /**
   * Remove a set of prototypes
   * @param d
   * @return
   */
  def --(d: Iterable[Val[?]]) =
    val dset = d.map(_.name).toSet
    PrototypeSet.copy(this)(prototypes = prototypes.filter(p ⇒ !dset.contains(p.name)).toList)

  /**
   * Check if a prototype is in the set by name
   * @param data
   * @return
   */
  def contains(data: Val[?]) = prototypeMap.contains(data.name)

  def toMap = prototypeMap

}
