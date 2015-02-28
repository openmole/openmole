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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.site

//TODO automatically generate this object as a managed source using sbt
object Resource {

  def FileResource(name: String) = RenameFileResource(name, name)

  def css = FileResource("openMOLEStyles.css")
  def ants = FileResource("ants.png")
  def antNumbers = FileResource("antnumbers.png")
  def antsNLogo = FileResource("ants.nlogo")
  def bootstrapCss = FileResource("bootstrap-3.3.2.css")
  def fireScreen = FileResource("firescreen.png")
  def fireGlobals = FileResource("fireGlobals.png")
  def fireNewGlobals = FileResource("fireNewGlobals.png")
  def fireMyDensity = FileResource("fireMyDensity.png")
  def fireNewFunction = FileResource("fireNewFunction.png")
  def fireOldSetup = FileResource("fireOldSetup.png")
  def fireRemoveClearAll = FileResource("fireRemoveClearAll.png")
  def logo = FileResource("openmole.png")
  def openmole = RenameFileResource("openmole.tar.gz", s"openmole-${Objects.version}.tar.gz")
  def openmoleDaemon = RenameFileResource("openmole-daemon.tar.gz", s"openmole-daemon-${Objects.version}.tar.gz")
  def versionLogo = FileResource("versionLogo.png")
  def api = ArchiveResource("openmole-api.tar.gz", "api")

  def all = Seq[Resource](
    css,
    bootstrapCss,
    logo,
    openmole,
    openmoleDaemon,
    api,
    ants,
    antNumbers,
    antsNLogo,
    fireScreen,
    fireGlobals,
    fireNewGlobals,
    fireNewFunction,
    fireOldSetup,
    fireRemoveClearAll,
    versionLogo)
}

sealed trait Resource
case class RenameFileResource(source: String, file: String) extends Resource
case class ArchiveResource(source: String, file: String) extends Resource

