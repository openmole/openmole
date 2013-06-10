/*
 * Copyright (C) 10/06/13 Romain Reuillon
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

package org.openmole.plugin.environment.glite

import org.openmole.misc.workspace.Workspace
import org.openmole.core.batch.environment.BatchEnvironment
import fr.iscpif.gridscale.authentication.GlobusAuthentication
import fr.iscpif.gridscale.information.BDII

trait BDIISRMServers extends BatchEnvironment {
  type SS = GliteStorageService

  def bdiiServer: BDII
  def voName: String
  def proxyCreator: GlobusAuthentication.ProxyCreator

  @transient lazy val threadsBySE = Workspace.preferenceAsInt(GliteEnvironment.LocalThreadsBySE)

  override def allStorages = {
    val stors = bdiiServer.querySRM(voName, Workspace.preferenceAsDuration(GliteEnvironment.FetchResourcesTimeOut).toSeconds.toInt)
    stors.map {
      s ⇒ GliteStorageService(s, this, proxyCreator, threadsBySE, GliteAuthentication.CACertificatesDir)
    }
  }
}
