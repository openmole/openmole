/*
 * Copyright (C) 2015 Romain Reuillon
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

package org.openmole.core.macros

import reflect.macros.whitebox.Context
import scala.language.experimental.macros
import reflect.runtime.universe._

object Keyword {

  def generateKeyword[T: c.WeakTypeTag](c: Context)(op: String): c.Expr[Any] = {
    import c.universe._
    import compat._

    val tType = weakTypeOf[T]
    val funcs: List[MethodSymbol] = tType.decls.collect { case s: MethodSymbol ⇒ s }.toList

    def generate(func: MethodSymbol) = {
      val params = func.paramLists.map(_.map(ValDef(_)))
      val names = params.map(_.map(p ⇒ if (p.tpt.toString.endsWith("*")) q"${p.name}: _*" else q"${p.name}"))
      val opTerm = TermName(op)
      q"def ${opTerm}[V <: {def ${func.name}(...${params}): Any}](...${params}) = (_: V).${func.name}(...${names})"
    }

    val g = funcs.map(f ⇒ generate(f))

    val result =
      q"""new {
            ..$g
          }
         """
    c.Expr(result)
  }

  def addKeywordMacroImpl[T: c.WeakTypeTag](c: Context): c.Expr[Any] = generateKeyword[T](c)(q"+=".toString)
  def add[T] = macro addKeywordMacroImpl[T]

  def setKeywordMacroImpl[T: c.WeakTypeTag](c: Context): c.Expr[Any] = generateKeyword[T](c)(q":=".toString)
  def set[T] = macro setKeywordMacroImpl[T]

}
