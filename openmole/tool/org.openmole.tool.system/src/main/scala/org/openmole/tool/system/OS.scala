/*
 * Copyright (C) 28/03/13 Romain Reuillon
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

package org.openmole.tool.system

object OS {
  val windows = "Windows"
  val linux = "Linux"
  val mac = "Mac"

  val arch32 = "x86"
  val arch64 = "amd64"

  lazy val actualOS = OS(System.getProperty("os.name"), System.getProperty("os.arch"))
}

import org.openmole.tool.system.OS.*

case class OS(name: String = "", arch: String = "") {
  def compatibleWith(testOS: String, testArch: String) =
    testOS.toLowerCase.contains(name.toLowerCase) &&
      testArch.toLowerCase.contains(arch.toLowerCase)

  def osName = System.getProperty("os.name")
  def osArch = System.getProperty("os.arch")

  def isWindows = osName.toLowerCase.contains(windows.toLowerCase)

  def is64Bit = osArch.toLowerCase.contains(arch64)

  def compatible = compatibleWith(osName, osArch)
}
