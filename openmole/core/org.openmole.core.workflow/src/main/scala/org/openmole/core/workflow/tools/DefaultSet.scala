/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.core.workflow.tools

import monocle.Lens

import scala.collection.immutable.TreeMap
import org.openmole.core.context._
import org.openmole.core.workflow.builder.{ DefaultBuilder, InputBuilder }

object DefaultSet {
  implicit def seqToDefaultSet(s: Seq[Default[_]]) = DefaultSet(s: _*)
  implicit def defaultSetToSeq(d: DefaultSet): Seq[Default[_]] = d.defaultMap.values.toSeq

  implicit def defaultBuilder = new DefaultBuilder[DefaultSet] with InputBuilder[DefaultSet] {
    override def defaults: Lens[DefaultSet, DefaultSet] = Lens.id
    override def inputs: Lens[DefaultSet, PrototypeSet] = Lens { _: DefaultSet ⇒ PrototypeSet.empty } { p ⇒ d ⇒ d }
  }

  lazy val empty = DefaultSet(Iterable.empty)

  type DefaultAssignment = DefaultSet ⇒ DefaultSet
  def fromAssignments(assignments: Seq[DefaultAssignment]): DefaultSet = assignments.foldLeft(empty)((ds, a) ⇒ a(ds))

  def apply(p: Default[_]*): DefaultSet = DefaultSet(p)

}

/**
  * A set of default values for prototypes
  * @param defaults
  */
case class DefaultSet(defaults: Iterable[Default[_]]) {

  @transient lazy val defaultMap =
    TreeMap.empty[String, Default[_]] ++ defaults.map { p ⇒ (p.prototype.name, p) }

  def +(p: Default[_]) = DefaultSet(p :: defaults.toList.filter(_.prototype != p.prototype))
  def -(p: Default[_]) = DefaultSet((defaultMap - p.prototype.name).values.toList)
  def contains(p: Default[_]) = defaultMap.contains(p.prototype.name)
  def get(name: String): Option[Default[_]] = defaultMap.get(name)
  def get(v: Val[_]): Option[Default[_]] = get(v.name)
}
