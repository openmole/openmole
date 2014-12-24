/*
 * Copyright (C) 2014 Romain Reuillon
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

package org.openmole.core.model

import scala.language.experimental.macros
import org.openmole.misc.macros.ExtractValName._
import reflect.macros.blackbox.{ Context ⇒ MContext }

package object data {

  def Val[T: Manifest](name: String) = Prototype(name)

  def Val[T]: Prototype[T] = macro valImpl[T]

  def valImpl[T: c.WeakTypeTag](c: MContext): c.Expr[Prototype[T]] = {
    import c.universe._
    val n = getValName(c)
    val wt = weakTypeTag[T].tpe
    c.Expr[Prototype[T]](q"Prototype[$wt](${n})")
  }

}
