/*
 * Copyright (C) 2015 Jonathan Passerat-Palmbach
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

package toolxit.bibtex.macros

import scala.reflect.macros.Context
import scala.language.experimental.macros

// from http://stackoverflow.com/a/16591277/470341
object OctalLiterals {
  implicit class OctallerContext(sc: StringContext) {
    def o(): Int = macro oImpl
  }

  def oImpl(c: Context)(): c.Expr[Int] = {
    import c.universe._

    c.literal(c.prefix.tree match {
      case Apply(_, Apply(_, Literal(Constant(oct: String)) :: Nil) :: Nil) ⇒
        Integer.decode("0" + oct)
      case _ ⇒ c.abort(c.enclosingPosition, "Invalid octal literal.")
    })
  }
}

