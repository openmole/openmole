/*
 * Copyright (C) 2010 reuillon
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

package org.openmole.misc.workspace

import java.io.File

object IWorkspace {
  val ConfigurationFile = ".preferences"
  val GlobalGroup = "Global"
  val DefaultObjectRepoLocaltion = ".objectRepository.bin" 
  val DefaultTmpLocation = ".tmp"
	
  val UniqueID = new ConfigurationLocation(GlobalGroup, "UniqueID")
  val ObjectRepoLocation = new ConfigurationLocation(GlobalGroup, "ObjectRepoLocation")
  val TmpLocation = new ConfigurationLocation(GlobalGroup, "TmpLocation")

}

trait IWorkspace {

	def location_=(location: File)
        def location: File

	def newDir(prefix: String): File
        def newDir: File
	def newFile(prefix: String, suffix: String): File
        def newFile: File

        def file(name: String): File

        def preference(location: ConfigurationLocation): String 

        def preferenceAsInt(location: ConfigurationLocation): Int

        def preferenceAsLong(location: ConfigurationLocation): Long

        def preferenceAsDouble(location: ConfigurationLocation): Double

        def preferenceAsDurationInMs(location: ConfigurationLocation): Long

        def preferenceAsDurationInS(location: ConfigurationLocation): Int

	def setPreference(configurationLocation: ConfigurationLocation, value: String)

        def removePreference(configurationElement: ConfigurationLocation)

        def password_=(password: String)

        def reset

        def isPreferenceSet(location: ConfigurationLocation): Boolean
        
	def +=(location: ConfigurationLocation, defaultValue : () => String)
	def +=(location: ConfigurationLocation, defaultValue : String)
        def defaultValue(location: ConfigurationLocation): String
}
