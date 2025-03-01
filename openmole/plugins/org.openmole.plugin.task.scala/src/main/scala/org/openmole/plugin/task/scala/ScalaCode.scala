package org.openmole.plugin.task.scala

/*
 * Copyright (C) 2024 Romain Reuillon
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

import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import monocle.*

object ScalaCode:
  given DefaultBuilder[ScalaCode] with
    def defaults = Focus[ScalaCode](_.defaults)

  given [T: Manifest]: DiscreteFromContextDomain[ScalaCode | String, T] =
    code ⇒ implicitly[DiscreteFromContextDomain[FromContext[Iterable[T]], T]].apply(ScalaCode.fromContext[Iterable[T]](code))

  def fromContext[T: Manifest](code: ScalaCode | String) =
    code match
      case code: ScalaCode => FromContext.codeToFromContext[T](code.source) copy (defaults = code.defaults)
      case code: String => FromContext.codeToFromContext[T](code)

case class ScalaCode(source: String, defaults: DefaultSet = DefaultSet.empty)

export org.openmole.plugin.task.scala.{ScalaCode => Evaluation}