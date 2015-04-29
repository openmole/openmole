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

package org.openmole.core.workflow.data

/**
 * It is a set of @link{Data}. It allows manipulating data by set instead of
 * individualy.
 */
import scala.collection.{ TraversableLike, SetLike }
import scala.collection.immutable.TreeMap

object PrototypeSet {

  implicit def traversableOfPrototypesToPrototoypeSet(ps: Traversable[Prototype[_]]) = apply(ps)

  val empty = PrototypeSet(List.empty)

  def apply(ps: Traversable[Prototype[_]]): PrototypeSet =
    new PrototypeSet {
      val prototypes = ps.toIterable
    }

  def apply(d: Prototype[_]*): PrototypeSet = PrototypeSet(d)
}

trait PrototypeSet extends Set[Prototype[_]] with SetLike[Prototype[_], PrototypeSet] with TraversableLike[Prototype[_], PrototypeSet] { self ⇒

  def prototypes: Iterable[Prototype[_]]

  @transient lazy val prototypeMap: Map[String, Prototype[_]] =
    TreeMap.empty[String, Prototype[_]] ++ prototypes.map { d ⇒ (d.name, d) }

  /**
   * Get the @link{Data} by its name as an Option.
   *
   * @param name the name of the @link{Data}
   * @return Some(data) if it is present in the data set None otherwise
   */
  def apply(name: String): Option[Prototype[_]] = prototypeMap.get(name)

  /**
   * Test if a variable with a given name is present in the data set.
   *
   * @param name the name of the @link{Data}
   * @return true if the variable with a matching name is present in the data
   * set false otherwise
   */
  def contains(name: String): Boolean = prototypeMap.contains(name)

  override def empty = PrototypeSet.empty

  override def iterator: Iterator[Prototype[_]] = prototypeMap.values.iterator

  def ++(d: Traversable[Prototype[_]]) = PrototypeSet(d.toList ::: prototypes.toList)

  def +(set: PrototypeSet): PrototypeSet = PrototypeSet(set.prototypes.toList ::: prototypes.toList)

  def +(d: Prototype[_]) = PrototypeSet(d :: prototypes.toList)

  def -(d: Prototype[_]) = PrototypeSet((prototypeMap - d.name).map { _._2 }.toList)

  override def contains(data: Prototype[_]) = prototypeMap.contains(data.name)

  def toMap = map(d ⇒ d.name -> d).toMap[String, Prototype[_]]

}
