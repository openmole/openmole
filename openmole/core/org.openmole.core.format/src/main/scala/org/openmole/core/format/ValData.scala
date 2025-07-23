package org.openmole.core.format

/*
 * Copyright (C) 2022 Romain Reuillon
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

import org.openmole.core.context.*
import org.openmole.tool.types.TypeTool
import io.circe.*
import org.openmole.core.pluginmanager.PluginManager

object ValData:
  def apply[T](v: Val[T]) = 
    new ValData(v.name, ValType.toTypeString(v.`type`, rootPrefix = false, replaceObject$ = false))

  def toVal(data: ValData) =
    val (ns, n) = Val.parseName(data.name)
    new Val(n, ValType(using TypeTool.toManifest(data.`type`, PluginManager.globalClassLoader(ValData.getClass))), ns)

case class ValData(name: String, `type`: String) derives derivation.ConfiguredCodec


