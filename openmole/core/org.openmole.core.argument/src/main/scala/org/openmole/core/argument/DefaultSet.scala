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

package org.openmole.core.argument

import monocle.Lens
import org.openmole.core.context.*
import org.openmole.core.argument.Default
import org.openmole.tool.random.RandomProvider
import org.openmole.core.fileservice.FileService
import org.openmole.core.workspace.TmpDirectory
import scala.collection.immutable.TreeMap

object DefaultSet {
  implicit def seqToDefaultSet(s: Seq[Default[?]]): DefaultSet = DefaultSet(s *)
  implicit def defaultSetToSeq(d: DefaultSet): Seq[Default[?]] = d.defaultMap.values.toSeq

  lazy val empty = DefaultSet(Iterable.empty)

  type DefaultAssignment = DefaultSet ⇒ DefaultSet
  def fromAssignments(assignments: Seq[DefaultAssignment]): DefaultSet = assignments.foldLeft(empty)((ds, a) ⇒ a(ds))

  def apply(p: Default[?]*): DefaultSet = DefaultSet(p)

  /**
   * Extend a context with default values (taken into account if overriding is activated or variable is missing in previous context)
   *
   * @param defaults default value
   * @param context  context to be extended
   * @return the new context
   */
  def completeContext(defaults: DefaultSet, context: Context)(implicit randomProvider: RandomProvider, newFile: TmpDirectory, fileService: FileService): Context =
    context ++
      defaults.flatMap {
        parameter ⇒
          if (parameter.`override` || !context.contains(parameter.prototype.name)) Some(parameter.toVariable.from(context))
          else Option.empty[Variable[?]]
      }

  def vals(defaults: DefaultSet): Seq[Val[?]] = defaults.defaults.map(_.prototype).toSeq

  def defaultVals(inputs: PrototypeSet, defaults: DefaultSet): Seq[Val[?]] =
    defaults.flatMap {
      parameter ⇒
        if (parameter.`override` || !inputs.contains(parameter.prototype.name)) Some(parameter.prototype)
        else None
    }

}

/**
 * A set of default values for prototypes
 * @param defaults
 */
case class DefaultSet(defaults: Iterable[Default[?]]) {

  @transient lazy val defaultMap =
    TreeMap.empty[String, Default[?]] ++ defaults.map { p ⇒ (p.prototype.name, p) }

  /**
   * add a Default to the DefaultSet
   * @param p
   * @return
   */
  def +(p: Default[?]) = DefaultSet(p :: defaults.toList.filter(_.prototype != p.prototype))

  /**
   * Remove a default
   * @param p
   * @return
   */
  def -(p: Default[?]) = DefaultSet((defaultMap - p.prototype.name).values.toList)
  def contains(p: Default[?]) = defaultMap.contains(p.prototype.name)
  def get(name: String): Option[Default[?]] = defaultMap.get(name)
  def get(v: Val[?]): Option[Default[?]] = get(v.name)
}
