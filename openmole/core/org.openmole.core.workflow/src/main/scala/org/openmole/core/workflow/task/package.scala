/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.core.workflow.task

import org.openmole.core.workflow.mole._
import monocle.Focus

import org.openmole.core.context._

trait TaskPackage:

  def newRNG(context: Context) = Task.buildRNG(context)

  object implicits:
    def +=(p: Val[_]) = Focus[MoleTask](_.implicits).modify(_ ++ Seq(p.name))



import scala.quoted._
inline def inspect[T](x: T): T = ${ inspectCode('x) }
def inspectCode[T](x: Expr[T])(using Quotes): Expr[T] =
  println(x)
  x