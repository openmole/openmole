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

package org.openmole.core.dsl

import org.openmole.core.pluginmanager.*
import org.openmole.core.serializer.*

trait Commands:
  def bundles = PluginManager.bundleFiles
  def dependencies(file: File) = PluginManager.dependencies(file)

  object omr:
    def toCSV(file: File, destination: File)(using SerializerService) = org.openmole.core.format.OMRFormat.writeCSV(file, destination)
    def toJSON(file: File, destination: File)(using SerializerService) = org.openmole.core.format.OMRFormat.writeJSON(file, destination)
    def copyFiles(file: File, destination: File) = org.openmole.core.format.OMRFormat.resultFileDirectory(file).foreach(_.copy(destination))
    def variables(file: File)(using SerializerService) = org.openmole.core.format.OMRFormat.variables(file)
