package org.openmole.core.setter

/*
 * Copyright (C) 2025 Romain Reuillon
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

import scala.quoted.*

object SourcePosition:

  def findOwner(using Quotes)(owner: quotes.reflect.Symbol, skipIf: quotes.reflect.Symbol => Boolean): quotes.reflect.Symbol =
    var owner0 = owner
    while (skipIf(owner0)) owner0 = owner0.owner
    owner0

  def nonMacroOwner(using Quotes)(owner: quotes.reflect.Symbol): quotes.reflect.Symbol =
    def getName(using Quotes)(s: quotes.reflect.Symbol) = s.name.trim.stripSuffix("$")
    findOwner(
      owner,
      owner0 => owner0.flags.is(quotes.reflect.Flags.Macro) && getName(owner0) == "macro"
    )

  def lineImpl(using ctx: Quotes): Expr[SourcePosition] =
    import quotes.reflect._
    val owner = nonMacroOwner(Symbol.spliceOwner)

    val line =
      owner.pos match
        case Some(pos) => Expr(Some(pos.startLine + 1))
        case None => Expr(None)
    val macroLine =  sourcecode.Macros.lineImpl
    val file = sourcecode.Macros.fileImpl
    '{SourcePosition(${file}.value, $line, ${macroLine}.value)}

  inline given SourcePosition = ${lineImpl}

final case class SourcePosition(file: String, expLine: Option[Int], macroLine: Int):
  def line = expLine.getOrElse(macroLine)
  override def toString = s"$file:$line"

