package org.openmole.gui.server.core

/*
 * Copyright (C) 22/09/14 // mathieu.leclaire@openmole.org
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

import java.util.concurrent.Semaphore
import org.openmole.core.fileservice.FileService
import org.openmole.core.location._
import org.openmole.core.preference.{ Preference, PreferenceLocation }
import org.openmole.core.workspace.{ TmpDirectory, Workspace }
import org.openmole.gui.ext.server.utils
import org.openmole.gui.server.jscompile.{ JSPack, Webpack }
import org.openmole.tool.crypto.KeyStore
import org.openmole.tool.file._
import org.openmole.tool.network.Network

object GUIServer {

  def fromWebAppLocation = openMOLELocation / "webapp"

  def webpackLocation = openMOLELocation / "webpack"

  def webapp(optimizedJS: Boolean)(implicit newFile: TmpDirectory, workspace: Workspace, fileService: FileService) = {
    val from = fromWebAppLocation
    val to = newFile.newDir("webapp")

    from / "css" copy to / "css"
    from / "fonts" copy to / "fonts"
    from / "img" copy to / "img"

    val webpacked = Plugins.openmoleFile(optimizedJS)

    val jsTarget = to /> "js"
    webpacked copy (jsTarget / utils.webpakedOpenmoleFileName)

    new File(webpacked.getAbsolutePath + ".map") copy (to /> "js" / (webpacked.getName + ".map"))

    to
  }

  lazy val port = PreferenceLocation("GUIServer", "Port", Some(Network.freePort))

  lazy val plugins = PreferenceLocation[String]("GUIServer", "Plugins", None)

  def initialisePreference(preference: Preference) = {
    if (!preference.isSet(port)) preference.setPreference(port, Network.freePort)
  }

  def lockFile(implicit workspace: Workspace) = {
    val file = utils.webUIDirectory / "GUI.lock"
    file.createNewFile
    file
  }

  def urlFile(implicit workspace: Workspace) = utils.webUIDirectory / "GUI.url"

  val servletArguments = "servletArguments"

  case class ServletArguments(
    services:           GUIServerServices,
    password:           Option[String],
    applicationControl: ApplicationControl,
    webapp:             File,
    extraHeader:        String,
    subDir:             Option[String]
  )

  // TODO scala 3
  def waitingOpenMOLEContent = "reactivate after scala 3 migration"
  //      <html>
  //        <head>
  //          <script>
  //            { """setTimeout(function(){ window.location.reload(1); }, 3000);""" }
  //          </script>
  //        </head>
  //        <link href="/css/style.css" rel="stylesheet"/>
  //        <body>
  //          <div>
  //            OpenMOLE is launching...
  //            <div class="loader" style="float: right"></div><br/>
  //          </div>
  //          (for the first launch, and after an update, it may take several minutes)
  //        </body>
  //      </html>

  case class ApplicationControl(restart: () ⇒ Unit, stop: () ⇒ Unit)

  sealed trait ExitStatus
  case object Restart extends ExitStatus
  case object Ok extends ExitStatus

}

