package org.openmole.gui.ext.plugin.server

import org.openmole.core.workspace.{ ConfigurationLocation, Workspace }
import org.openmole.gui.ext.data._

/*
 * Copyright (C) 01/06/16 // mathieu.leclaire@openmole.org
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

object Configurations {

  private val configs: Map[ConfigData, ConfigurationLocation[String]] = Map(
    VOTest → ConfigurationLocation("AuthenicationPanel", "voTest", None)
  )

  def apply(configData: ConfigData) =
    configs.get(configData).flatMap { p ⇒ Workspace.preferenceOption(p) }

  def set(configData: ConfigData, value: String) =
    configs.get(configData).map { cl ⇒ Workspace setPreference (cl, value) }

}
