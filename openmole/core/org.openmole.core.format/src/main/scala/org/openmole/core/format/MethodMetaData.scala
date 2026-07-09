package org.openmole.core.format

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

import io.circe.*

object MethodMetaData:
  def toKebabCase(name: String): String =
    name
      .replaceAll("([a-z])([A-Z])", "$1-$2")
      .replaceAll("([A-Z]+)([A-Z][a-z])", "$1-$2")
      .toLowerCase

  def name(o: Any) =
    val n = o.getClass.getSimpleName
    val pruned = if n.endsWith("$") then n.dropRight(1) else n
    toKebabCase(pruned)

  given MethodMetaData[None.type] = MethodMetaData(name("NoMethod"))

case class MethodMetaData[D](name: String)(using val encoder: Encoder[D])
