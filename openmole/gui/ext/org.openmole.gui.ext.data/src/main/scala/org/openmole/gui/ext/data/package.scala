package org.openmole.gui.ext.data

/*
 * Copyright (C) 10/03/17 // mathieu.leclaire@openmole.org
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
package object routes {
  val connectionRoute = "/connection"
  val shutdownRoute = "/shutdown"
  val restartRoute = "/restart"
  val appRoute = "/app"
  val downloadFileRoute = "/downloadFile"
  val downloadPluginsRoute = "/downloadPlugins"
  val uploadFilesRoute = "/uploadFiles"
  val resetPasswordRoute = "/resetPassword"

  val restRoute = "/rest"
  val restRouteExecution = "/rest/execution/*"

  def downloadFile(uri: String, hash: Boolean = false) = s"downloadFile?path=$uri&hash=$hash"
  def hashHeader = "Content-Hash"
}
