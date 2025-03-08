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


import org.openmole.tool.file.*

object DefinitionScope:
  case class InternalScope(name: String) extends DefinitionScope
  case class UserScope(scope: UserDefinitionScope) extends DefinitionScope

  given Conversion[String, InternalScope] = scope => InternalScope(scope)

  object user:
    inline implicit def default(using line: SourcePosition, userScope: UserDefinitionScope): DefinitionScope =
      userScope match
        case u: UserScriptDefinitionScope => UserScope(u.copy(line.line + u.line))
        case x => UserScope(x)

  object UserDefinitionScope:
    given default: UserDefinitionScope = OutOfUserDefinitionScope

  trait UserDefinitionScope
  case class ImportedUserDefinitionScope(`import`: String, importedFrom: File) extends UserDefinitionScope
  case class UserScriptDefinitionScope(line: Int) extends UserDefinitionScope
  case object OutOfUserDefinitionScope extends UserDefinitionScope

  def isUser(scope: DefinitionScope) =
    scope match
      case _: UserScope => true
      case _ => false

  extension (scope: DefinitionScope)
    def line: Option[Int] =
      scope match
        case UserScope(u: UserScriptDefinitionScope) => Some(u.line)
        case _ => None

    def imported: Option[ImportedUserDefinitionScope] =
      scope match
        case UserScope(imp: ImportedUserDefinitionScope) => Some(imp)
        case _ => None

sealed trait DefinitionScope
