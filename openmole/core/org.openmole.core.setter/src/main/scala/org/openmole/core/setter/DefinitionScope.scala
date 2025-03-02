package org.openmole.core.setter

/*
 * Copyright (C) 2023 Romain Reuillon
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


import sourcecode.Line

object DefinitionScope:
  case class InternalScope(name: String) extends DefinitionScope
  case class UserScope(line: Option[Int]) extends DefinitionScope

  given Conversion[String, InternalScope] = scope => InternalScope(scope)

  object user:
    inline implicit def default(using line: SourcePosition, relativizeLine: DefinitionLine): DefinitionScope =
      if DefinitionLine.isNoLine(relativizeLine)
      then UserScope(None)
      else UserScope(Some(line.line - relativizeLine.offset))

  object DefinitionLine:
    def isNoLine(relativizeLine: DefinitionLine) = relativizeLine == noLine
    given noLine: DefinitionLine = DefinitionLine(-1)
    def imported = noLine

  case class DefinitionLine(offset: Int)

  def isUser(scope: DefinitionScope) =
    scope match
      case _: UserScope => true
      case _ => false


sealed trait DefinitionScope
