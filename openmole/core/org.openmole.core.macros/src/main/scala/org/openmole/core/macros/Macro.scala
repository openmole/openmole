/*
 * Copyright (C) 2013 Mark Hammons, Romain Reuillon
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

package org.openmole.core.macros

import reflect.macros.blackbox.Context
import scala.language.experimental.macros

object ExtractValName {

  def getValName(c: Context): c.Expr[String] = {
    def enclosingTrees(c: Context): Seq[c.Tree] =
      c.asInstanceOf[reflect.macros.runtime.Context].callsiteTyper.context.enclosingContextChain.map(_.tree.asInstanceOf[c.Tree])
    def invalidEnclosingTree(s: String): String = ???

    import c.universe.{ Apply ⇒ ApplyTree, _ }

    val methodName = c.macroApplication.symbol.name

    def processName(n: Name) = n.decodedName.toString.trim // trim is not strictly correct, but macros don't expose the API necessary

    def enclosingVal(trees: List[c.Tree]): String = {
      trees match {
        case vd @ ValDef(_, name, _, _) :: ts ⇒ processName(name)
        case dd @ DefDef(_, name, _, _, _, _) :: ts ⇒ processName(name)
        case (_: ApplyTree | _: Select | _: TypeApply) :: xs ⇒ enclosingVal(xs)
        // lazy val x: X = <methodName> has this form for some reason (only when the explicit type is present, though)
        case Block(_, _) :: DefDef(mods, name, _, _, _, _) :: xs if mods.hasFlag(Flag.LAZY) ⇒ processName(name)
        case _ ⇒
          c.error(c.enclosingPosition, invalidEnclosingTree(methodName.decodedName.toString))
          "<error>"
      }
    }
    c.Expr[String](Literal(Constant(enclosingVal(enclosingTrees(c).toList))))
  }

}
